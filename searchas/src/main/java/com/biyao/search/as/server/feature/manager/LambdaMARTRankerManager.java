package com.biyao.search.as.server.feature.manager;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.Ranker;
import ciir.umass.edu.learning.tree.LambdaMART;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.as.server.bean.ProductInfoFromPdc;
import com.biyao.search.as.server.bean.RankItem;
import com.biyao.search.as.server.cache.ProductCache;
import com.biyao.search.as.server.cache.QueryCategoryCache;
import com.biyao.search.as.server.cache.redis.RefundRateThresholdRedisCache;
import com.biyao.search.as.server.common.util.DcLogUtil;
import com.biyao.search.as.server.common.util.FileUtil;
import com.biyao.search.as.server.experiment.ASExperimentSpace;
import com.biyao.search.as.server.feature.cache.ProductFeatureCacheV2;
import com.biyao.search.as.server.feature.cache.QueryFeatureCache;
import com.biyao.search.as.server.feature.model.*;
import com.biyao.search.as.server.feature.service.AbstractRank;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeature;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.SearchItem;
import com.google.common.base.Strings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: xiafang
 * @date: 2019/11/18
 */
@Slf4j
@Service
public class LambdaMARTRankerManager extends AbstractRank {
    /**
     * 排序特征配置文件路径
     */
    @Value("${path.feature.map}")
    private String PATH_FEATURE_MAP;
    /**
     * 排序模型配置文件路径
     */
    @Value("${path.lambdamart}")
    private String PATH_LAMBDAMART;

    @Autowired
    ProductFeatureCacheV2 productFeatureCache;
    @Autowired
    QueryFeatureCache queryFeatureCache;
    @Autowired
    FeatureExtractManager featureExtractManager;
    @Autowired
    private ProductCache productCache;
    @Autowired
    private RefundRateThresholdRedisCache refundRateThresholdRedisCache;

    @Autowired
    private QueryCategoryCache queryCategoryCache;

    @Autowired
    private ASExperimentSpace experimentSpace;

    @PostConstruct
    public void init() {
        super.refresh();
    }

    /**
     * 构造featureMap
     *
     * @return
     */
    @Override
    protected Map<String, Integer> buildFeatureMap() {
        //加载排序特征配置文件
        String featureStr = FileUtil.remoteRead(PATH_FEATURE_MAP);
        Map<String, Integer> featureMap = new HashMap<>();
        try {
            String content = "";
            BufferedReader in = new BufferedReader(new StringReader(featureStr));
            while ((content = in.readLine()) != null) {
                content = content.trim();
                if (content.length() == 0) {
                    continue;
                }
                if (content.indexOf("##") == 0) {
                    continue;
                }
                //actual feature component
                String[] featurePair = content.split(":");
                if (featurePair.length != 2) {
                    continue;
                }
                if (featurePair[1] != null && featurePair[0] != null) {
                    featureMap.put(featurePair[0].trim(), Integer.parseInt(featurePair[1].trim()));
                }
            }
            in.close();
            log.info("[操作日志]************排序特征配置文件featureMap.txt加载成功***********");
        } catch (Exception ex) {
            log.error("[严重异常][文件异常]排序特征配置文件featureMap.txt加载异常，文件url:{},异常信息：", PATH_FEATURE_MAP, ex);
        }
        return featureMap;
    }

    @Override
    public List<SearchItem> sort(List<SearchItem> searchItemList, String query, SearchRequest request, int group) {
        QueryFeature queryFeature = queryFeatureCache.getQueryFeatureByQuery(query);
        UserFeature userFeature = ThreadLocalFeature.USER_FEATURE.get();
        ContextFeature contextFeature = ThreadLocalFeature.CONTEXT_FEATURE.get();
        //记录商品特征
        Map<Integer, RankItem> rankItemMap = new HashMap<>();
            for (SearchItem item : searchItemList) {
                Integer productId = item.getProductId();
                if (ranker != null) {
	                //通过pid查询产品特征
	                ProductFeature productFeature = productFeatureCache.getProductFeatureById(productId.longValue());
	                ByBaseFeature byBaseFeature = featureExtractManager.extract(item, productFeature, queryFeature, userFeature, contextFeature);
	                //组织为RankLib包要求的数据格式
	                RankItem rankItem = new RankItem();
	                DataPoint dataPoint = super.buildDenseDataPointText(byBaseFeature, rankItem);
	                item.setRankScore(ranker.eval(dataPoint));
	                rankItem.setProductId(productId);
	                rankItem.setRankScoreBasedModel(item.getRankScore());
	                rankItem.setMatchScore(item.getMatchScore());
	                rankItemMap.put(productId, rankItem);
                }
                /*鸿源V2.4-交互视觉优化  新增前台三级类目 author：huangyq，modifyDate:2021/10/11*/
                ProductInfoFromPdc pdcProductInfoFromPdc = productCache.getProductInfo(productId);
                if(pdcProductInfoFromPdc != null) {
                	
                	item.setFrontThirdCategoryIds(pdcProductInfoFromPdc.getFrontThirdCategoryIds());
                	item.setFrontThirdCategoryNames(pdcProductInfoFromPdc.getFrontThirdCategoryNames());
                }
            }
        //LambdaMART模型排序
        rankBasedRankScore(searchItemList);
        //类目预测排序
        rankBasedCategoryScore(searchItemList, query, rankItemMap);

        //退款率排序
        rankBasedRefundRate(searchItemList, rankItemMap);
        if (ThreadLocalFeature.IS_WHITE_LIST_UUID.get()) {
            JSONObject logBody = new JSONObject();
            logBody.put("query", query);
            logBody.put("group", group);
            logBody.put("items", rankItemMap.values());
            DcLogUtil.printSearchDetailLog("searchas-rank", request.getCommonParam().getUuid(), request.getCommonParam().getSid(), JSON.toJSONString(logBody));
        }
        return searchItemList;
    }

    /**
     * 首先将综合模型排序分归到[1,100], 只有一个待排元素时，不用排序，分数不用转换
     * 然后根据公式rankScore * categoryScore计算类目提权作用下的排序分
     *
     * @param searchItemList 待排序元素
     */
    private void rankBasedCategoryScore(List<SearchItem> searchItemList, String query, Map<Integer, RankItem> rankItemMap) {
        if (searchItemList == null || searchItemList.size() <= 1) {
            return;
        }
        Double scoreFirst = searchItemList.get(0).getRankScore();
        Double scoreLast = searchItemList.get(searchItemList.size() - 1).getRankScore();
        boolean flag = scoreFirst.equals(scoreLast);
        try {
            Map<Long, Float> categoryScoreMap = queryCategoryCache.getCategoryScoreMap(query);
            searchItemList.forEach(item -> {
                Long categoryId = productCache.getThirdCategoryId(item.getProductId().longValue());
                if (categoryId != null) {
                    Float categoryScore = categoryScoreMap.getOrDefault(categoryId, 1.0f);
                    Double scoreLikeNormalization = 1.0;
                    if (!flag) {
                        scoreLikeNormalization = 99 * (item.getRankScore() - scoreLast) / (scoreFirst - scoreLast) + 1.0;
                    }
                    item.setRankScore(scoreLikeNormalization * categoryScore);
                    RankItem rankItem = rankItemMap.get(item.getProductId());
                    rankItem.setScoreLikeNormalization(scoreLikeNormalization);
                    rankItem.setCategoryScore(categoryScore);
                    rankItem.setRankBasedCategoryScore(item.getRankScore());
                }
            });
            rankBasedRankScore(searchItemList);
        } catch (Exception e) {
            log.error("[未知异常]query预测类目对排序提权", e);
        }
    }

    /**
     * 基于排序分排序
     *
     * @param searchItemList 待排序元素
     */
    private void rankBasedRankScore(List<SearchItem> searchItemList) {
        //利用算法计算的排序分进行排序
        searchItemList.sort(new Comparator<SearchItem>() {
            @Override
            public int compare(SearchItem o1, SearchItem o2) {
                if (o2.getRankScore().compareTo(o1.getRankScore()) == 0) {
                    return o2.getMatchScore().compareTo(o1.getMatchScore());
                } else {
                    return o2.getRankScore().compareTo(o1.getRankScore());
                }
            }
        });
    }

    /**
     * 基于综合排名，利用退货退款率降权排序
     *
     * @param searchItemList 待排序元素
     */
    private void rankBasedRefundRate(List<SearchItem> searchItemList, Map<Integer, RankItem> rankItemMap) {
        try {
            //遍历  基于退货退款率计算排序分再排序
            double refundRateThreshold = refundRateThresholdRedisCache.getRefundRateThreshold();
            int itemNumber = searchItemList.size();
            for (int pos = 0; pos < itemNumber; pos++) {
                SearchItem item = searchItemList.get(pos);
                Integer productId = item.getProductId();
                double refundRate = productCache.getRefundRateByPid(Long.parseLong(productId.toString()));
                if (refundRate > refundRateThreshold) {
                    item.setRankScoreBasedRefundRate((double) (itemNumber - pos) / itemNumber * (1 - Math.pow(refundRate, 3)));
                } else {
                    item.setRankScoreBasedRefundRate((double) (itemNumber - pos) / itemNumber);
                }
                RankItem rankItem = rankItemMap.get(productId);
                rankItem.setRefundRate(refundRate);
                rankItem.setRankScoreBasedRefundRate(item.getRankScoreBasedRefundRate());
            }
            searchItemList.sort(Comparator.comparing(SearchItem::getRankScoreBasedRefundRate).reversed());
        } catch (Exception e) {
            log.error("[严重异常][未知异常]基于退货退款率排序异常，异常信息：{}", e);
        }
    }

    /**
     * 加载模型文件
     *
     * @return
     */
    @Override
    protected Ranker buildRanker() {
        //加载排序模型
        String modelText = FileUtil.remoteRead(PATH_LAMBDAMART);
        if (Strings.isNullOrEmpty(modelText)) {
            log.error("[严重异常][文件异常]LambdaMart模型数据获取不到，请检查模型配置文件searchas_lambdaMart.model，url:{}", PATH_LAMBDAMART);
            return null;
        }
        Ranker ranker = new LambdaMART();
        ranker.loadFromString(modelText);
        log.info("[操作日志]**********LambdaMart模型数据加载完成,模型配置文件searchas_lambdaMart.model*********");
        return ranker;
    }

    /**
     * 新增排序方法，切换到新的排序模型，下线老的排序模型
     *
     * @param products 待排商品
     * @param query
     * @return 返回排序后的商品
     */
    public List<ASProduct> sort4PC(List<ASProduct> products, String query) {
        QueryFeature queryFeature = queryFeatureCache.getQueryFeatureByQuery(query);
        UserFeature userFeature = ThreadLocalFeature.USER_FEATURE.get();
        ContextFeature contextFeature = ThreadLocalFeature.CONTEXT_FEATURE.get();
        if (ranker != null) {
            for (ASProduct product : products) {
                Long productId = Long.valueOf(product.getSuId().substring(0, 10));
                //通过pid查询产品特征
                ProductFeature productFeature = productFeatureCache.getProductFeatureById(productId);
                //ASProduct对象转换为SearchItem只是为了兼容extract方法入参
                SearchItem item = new SearchItem();
                item.setMatchScore((double) product.getScore());
                RankItem rankItem = new RankItem();
                ByBaseFeature byBaseFeature = featureExtractManager.extract(item, productFeature, queryFeature, userFeature, contextFeature);
                //组织为RankLib包要求的数据格式
                DataPoint dataPoint = super.buildDenseDataPointText(byBaseFeature, rankItem);
                product.setAlgoReScore(ranker.eval(dataPoint));
            }
        }
        products.sort(Comparator.comparing(ASProduct::getAlgoReScore).reversed());
        return products;
    }
}
