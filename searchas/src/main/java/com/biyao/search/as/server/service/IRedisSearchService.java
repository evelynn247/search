package com.biyao.search.as.server.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.as.server.cache.ProductCache;
import com.biyao.search.as.server.common.consts.ExperimentConsts;
import com.biyao.search.as.server.experiment.ASExperimentSpace;
import com.biyao.search.as.server.feature.manager.LambdaMARTRankerManager;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeature;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.bs.service.NewProductMatch;
import com.biyao.search.bs.service.PartialQueryFetch;
import com.biyao.search.bs.service.TagProductMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.SearchItem;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author biyao
 * @date long long ago
 */
@Service
public class IRedisSearchService {

    @Autowired
    private NewProductMatch newProductMatch;

    @Autowired
    private TagProductMatch tagProductMatch;

    @Autowired
    private PartialQueryFetch partialQueryFetch;

    @Autowired
    LambdaMARTRankerManager lambdaMARTRankerManager;
    @Autowired
    ASExperimentSpace asExperimentSpace;

    @Autowired
    ProductCache productCache;

    @Autowired
    private ASExperimentSpace experimentSpace;

    @Autowired
    private RankService rankService;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private int groupSize = 100;

    /**
     * 从redis中根据词语获取搜索结果，若无记录则调用bs搜索并排序
     *
     * @param query
     * @param expectNum
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 下午12:01:30
     */
    public List<ProductMatchResult> searchByExactQuery(String query, int expectNum, SearchRequest request,Integer aliasType) {
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setQuery(query);
        matchRequest.setExpectNum(expectNum);
        matchRequest.setCommonParam(request.getCommonParam());
        matchRequest.setCompletelyExpectNum(expectNum);
        matchRequest.setPartialExpectNum(expectNum);
        //matchRequest.setSupplierId(request.getSupplierId());
        //0 主搜，5 轮播图落地页，6 搜本店包含部分匹配召回，其他不进行部分匹配召回
        if (aliasType == null) {
            matchRequest.setIsGetPartial(request.getIsGetPartial());
        } else if (aliasType == 0 || aliasType == 5 || aliasType == 6) {
            matchRequest.setIsGetPartial(1);
            matchRequest.setAliasType(aliasType);
        } else {
            matchRequest.setIsGetPartial(0);
            matchRequest.setAliasType(aliasType);
        }
        //透传商品池
        //matchRequest.setProductPool(request.getProductPool());
        matchRequest.setHyChannelId(request.getHyChannelId());
        matchRequest.setHyActivityId(request.getHyActivityId());
        matchRequest.setHyTopicId(request.getHyTopicId());
        //鸿源V3.3-鸿源分销V1.0	场景ID透传至searchbs author：huangyq modifyDate：2021/11/22
        matchRequest.setSceneId(request.getSceneId());
        RPCResult<List<ProductMatchResult>> rpcResult = newProductMatch.match(matchRequest);
        if (!rpcResult.getStatus().equals(SearchStatus.OK)) {
            return null;
        }

        /*
         * 对返回结果进行排序
         */
        List<ProductMatchResult> productMatchResults = rpcResult.getData();

        //排序实验
        if (experimentSpace.hitExp(ExperimentConsts.FLAG_USE_NEW_RANK, ExperimentConsts.VALUE_USE_NEW_RANK, request)) {
            for (ProductMatchResult productMatchResult : productMatchResults) {
                productMatchResult.setItems(rankService.sort(productMatchResult.getItems(),query,request));
            }
        }else{
            for (ProductMatchResult productMatchResult : productMatchResults) {
                List<List<SearchItem>> groupSearchItems = grouping(productMatchResult.getItems());
                List<SearchItem> searchItems = new ArrayList<>();
                //表示待排元素是第几组数据，打印日志
                int group = 0;
                for (List<SearchItem> items : groupSearchItems) {
                    searchItems.addAll(lambdaMARTRankerManager.sort(items, query, request, group));
                    group++;
                }
                productMatchResult.setItems(searchItems);
            }
        }

        //部分匹配商品和相关召回商品合并
        if(productMatchResults.size() == 3){
            productMatchResults.get(1).getItems().addAll(productMatchResults.get(2).getItems());
            productMatchResults.remove(2);
        }
        if(productMatchResults.size() == 2 && !productMatchResults.get(0).getQuery().equals(query)){
            productMatchResults.get(0).getItems().addAll(productMatchResults.get(1).getItems());
            productMatchResults.remove(1);
        }
        return productMatchResults;
    }

    /**
     * tag搜索
     *
     * @param query
     * @param expectNum
     * @param request
     * @return
     */
    public ProductMatchResult tagSearchByExactQuery(String query, int expectNum, SearchRequest request) {
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setQuery(query);
        matchRequest.setExpectNum(expectNum);
        matchRequest.setCommonParam(request.getCommonParam());
        RPCResult<ProductMatchResult> rpcResult = tagProductMatch.match(matchRequest);
        if (!rpcResult.getStatus().equals(SearchStatus.OK)) {
            return null;
        }

        /*
         * 对返回结果进行排序
         */
        ProductMatchResult productMatchResult = rpcResult.getData();
        int group = 0;
        List<SearchItem> searchItems = lambdaMARTRankerManager.sort(productMatchResult.getItems(), query, request, group);
        productMatchResult.setItems(searchItems);
        if (ThreadLocalFeature.IS_WHITE_LIST_UUID.get()) {
            logger.error("[操作日志]uuid命中白名单，sid=" + ThreadLocalFeature.SID.get() + "\tquery=" + query + "\t排序结果=" + JSON.toJSONString(searchItems));
        }
        return productMatchResult;
    }

    private List<List<SearchItem>> grouping(List<SearchItem> searchItems) {
        List<List<SearchItem>> result = new ArrayList<>();
        try {

            //先按照matchScore排序
            searchItems.sort((o1, o2) -> o2.getMatchScore().compareTo(o1.getMatchScore()));

            while (searchItems.size() > 0) {
                List<SearchItem> productList = searchItems.stream().skip(0).limit(groupSize).collect(Collectors.toList());
                int start = searchItems.size() > groupSize ? groupSize : searchItems.size();
                if (start == groupSize && searchItems.size() != groupSize) {
                    boolean flag = true;
                    while (flag) {
                        if (start == searchItems.size()) {
                            break;
                        }
                        if (searchItems.get(start).getMatchScore().equals(productList.get(productList.size() - 1).getMatchScore())) {
                            productList.add(searchItems.get(start));
                            start++;
                        } else {
                            flag = false;
                        }
                    }
                }
                for (int i = 0; i < start; i++) {
                    if (searchItems.size() > 0) {
                        searchItems.remove(0);
                    }
                }
                result.add(productList);
            }
        } catch (Exception e) {
            logger.error("[严重异常]商品分组异常，商品信息：{}，异常信息：{}", JSONObject.toJSONString(searchItems), e.getMessage());
        }

        return result;
    }

    public boolean isTagQuery(String query, SearchRequest request) {
        try {
            RPCResult<Boolean> isTagResult = partialQueryFetch.isTagQuery(query);
            if (isTagResult.getStatus().equals(SearchStatus.OK)) {
                return isTagResult.getData();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }


    /**
     * 根据facet和实验号生成md5码用作缓存key的一部分
     *
     * @param facets
     * @param request
     * @return
     * @author: luozhuo
     * @date: 2018年7月26日 下午12:02:50
     */
    private String calcuEnvironmentHash(List<FacetItem> facets,
                                        SearchRequest request) {
        StringBuilder sb = new StringBuilder();
        for (FacetItem item : facets) {
            sb.append(item.getKey());
            sb.append(item.getValues().toString());
        }

        sb.append(request.getExpIds().toString());

        String facetMd5 = Hashing.md5().newHasher().putString(sb.toString(), Charsets.UTF_8).hash().toString();

        return facetMd5.substring(9, 25);
    }

}
