package com.biyao.search.as.server.service;

import com.biyao.search.as.server.bean.ProductInfoFromPdc;
import com.biyao.search.as.server.cache.ProductCache;
import com.biyao.search.as.server.cache.ProductScoreCache;
import com.biyao.search.as.server.cache.QueryCategoryCache;
import com.biyao.search.as.server.cache.redis.RankParamCache;
import com.biyao.search.as.server.cache.redis.RefundRateThresholdRedisCache;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.common.model.SearchItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/19 17:35
 * @description
 */
@Service
@Slf4j
public class RankService {

    @Autowired
    private RankParamCache rankParamCache;

    @Autowired
    private ProductScoreCache productScoreCache;

    @Autowired
    private QueryCategoryCache queryCategoryCache;

    @Autowired
    private ProductCache productCache;

    @Autowired
    private RefundRateThresholdRedisCache refundRateThresholdRedisCache;
    /**
     * 新排序方法
     * @param searchItemList
     * @param query
     * @param request
     * @return
     */
    public List<SearchItem> sort(List<SearchItem> searchItemList, String query, SearchRequest request) {

        if(searchItemList == null || searchItemList.size() == 0){
            return new ArrayList<>();
        }

        //根据新模型的公式计算商品rankScore
        for (SearchItem searchItem:searchItemList) {
            searchItem.setRankScore(setRankScore(searchItem.getProductId(),searchItem.getMatchScore(),query));
            
            /*鸿源V2.4-交互视觉优化  新增前台三级类目 author：huangyq，modifyDate:2021/10/11*/
            ProductInfoFromPdc pdcProductInfoFromPdc = productCache.getProductInfo(searchItem.getProductId());
            if(pdcProductInfoFromPdc != null) {
            	
            	searchItem.setFrontThirdCategoryIds(pdcProductInfoFromPdc.getFrontThirdCategoryIds());
            	searchItem.setFrontThirdCategoryNames(pdcProductInfoFromPdc.getFrontThirdCategoryNames());
            }
        }

        //根据rankScore排序
        rankBasedRankScore(searchItemList);

        //退货退款率影响排序
        rankBasedRefundRate(searchItemList);

        return searchItemList;
    }

    /**
     * 计算商品排序分
     * @return
     */
    private Double setRankScore(Integer pid,Double matchScore,String query){

        Double productScore = productScoreCache.getProductScore(pid.longValue());
        Float categoryScore = 0f;
        Map<Long,Float> categoryScoreMap = queryCategoryCache.getCategoryScoreMap(query);
        Long categoryId = productCache.getThirdCategoryId(pid.longValue());
        if (categoryId != null) {
            categoryScore = categoryScoreMap.getOrDefault(categoryId, 0f);
        }

        double param1 = rankParamCache.getParamList().get(0) * productScore;
        double param2 = rankParamCache.getParamList().get(1) * matchScore;
        double param3 = rankParamCache.getParamList().get(2) * categoryScore;

        return param1+param2+param3;
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
    private void rankBasedRefundRate(List<SearchItem> searchItemList) {
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
            }
            searchItemList.sort(Comparator.comparing(SearchItem::getRankScoreBasedRefundRate).reversed());
        } catch (Exception e) {
            log.error("[严重异常][未知异常]基于退货退款率排序异常，异常信息：{}", e);
        }
    }
}
