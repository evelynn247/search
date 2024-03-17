package com.biyao.search.bs.server.remote;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.service.DeriveProductMatch;
import com.biyao.search.bs.service.model.request.MatchDeriveRequest;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.DeriveProductMatchResult;
import com.biyao.search.common.enums.QueryAnalyzerEnum;
import com.biyao.search.common.model.DeriveSearchItem;
import com.biyao.search.common.model.RPCResult;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/1/20 15:39
 * @description
 */
@Service(value = "deriveProductMatch")
public class DeriveProductMatchImpl implements DeriveProductMatch {

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 衍生商品检索
     * @param request
     * @return
     */
    @BProfiler(key = "com.biyao.search.bs.server.remote.DeriveProductMatchImpl.match", monitorType = {MonitorType.TP, MonitorType.HEARTBEAT,
            MonitorType.FUNCTION_ERROR})
    @Override
    public RPCResult<List<DeriveProductMatchResult>> match(MatchRequest request) {

        List<DeriveProductMatchResult> result = new ArrayList<>();

        //获取完全匹配结果
        DeriveProductMatchResult completelyMatchResult = getDeriveProductMatchResult(request.getQuery(), request.getExpectNum(), request.getAliasType());
        if (completelyMatchResult.getItems() != null) {
            if (completelyMatchResult.getItems().size() > 0) {
                result.add(completelyMatchResult);
            }
        }

        //获取部分匹配结果
        //一起拼搜索不进行部分匹配
        //完全匹配结果满足ExpectNum数量时，不进行部分匹配；不满足时，用部分匹配补齐ExpectNum数量
        //部分匹配返回query设置为""
        if (request.getAliasType()==0) {
            if (completelyMatchResult.getItems().size() < request.getExpectNum()) {
                //补齐数量
                int limitNum = request.getExpectNum() - completelyMatchResult.getItems().size();

                //完全匹配商品pid集合，去重使用
                List<String> pidList = new ArrayList<>();
                completelyMatchResult.getItems().stream().forEach(item -> {pidList.add(item.getProductId());});

                DeriveProductMatchResult partMatchResult = new DeriveProductMatchResult();
                partMatchResult.setQuery("");
                List<DeriveSearchItem> items = new ArrayList<>();
                TransportClient client = ESClientConfig.getESClient();
                AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(request.getQuery())
                        .setIndex(ElasticSearchConsts.BY_MALL_DERIVE_ALIAS).setAnalyzer(QueryAnalyzerEnum.IK_SMART.getCode()).get();
                outer:
                for (AnalyzeResponse.AnalyzeToken token : analyzeResponse.getTokens()) {
                    DeriveProductMatchResult tempResult = getDeriveProductMatchResult(token.getTerm(), request.getExpectNum(), request.getAliasType());
                    if (tempResult.getItems() != null) {
                        if (tempResult.getItems().size() > 0) {
                            for (DeriveSearchItem item : tempResult.getItems()) {
                                if (items.size() < limitNum) {
                                    if(!pidList.contains(item.getProductId())){
                                        //未补齐数量时，且商品不包含在完全匹配商品中，加入部分匹配集合
                                        items.add(item);
                                    }
                                } else {
                                    //补齐数量时，直接跳出循环
                                    break outer;
                                }
                            }
                        }
                    }
                }
                partMatchResult.setItems(items);
                if(partMatchResult.getItems()!=null){
                    if(partMatchResult.getItems().size()>0){
                        result.add(partMatchResult);
                    }
                }
            }
        }


        return new RPCResult<>(result);
    }

    /**
     * 获取ES返回检索结果
     * @param query
     * @param expectNum
     * @param OnlyToggroupProduct
     * @return
     */
    private DeriveProductMatchResult getDeriveProductMatchResult(String query, int expectNum, Integer OnlyToggroupProduct) {
        DeriveProductMatchResult matchResult = new DeriveProductMatchResult();

        QueryBuilder finalQuery = buildFinalQueryBuilder(query, QueryAnalyzerEnum.IK_SMART, ElasticSearchConsts.MATCH_FIELDS_DERIVE, OnlyToggroupProduct);

        SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ElasticSearchConsts.BY_MALL_DERIVE_ALIAS)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(expectNum)
                .get();

        /*  组装召回结果 */
        SearchHits searchHits = searchResponse.getHits();
        List<DeriveSearchItem> searchResult = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            DeriveSearchItem item = new DeriveSearchItem();
            item.setProductId(hit.getSource().get("productId").toString());
            item.setMatchScore(Float.valueOf(hit.getScore()).doubleValue());
            if (searchResult.size() < expectNum) {
                searchResult.add(item);
            }
        }

        matchResult.setQuery(query);
        matchResult.setItems(searchResult);

        return matchResult;


    }

    /**
     * 构建QueryBuilder表达式
     * @param query
     * @param analyzer
     * @param matchFields
     * @param OnlyToggroupProduct
     * @return
     */
    private QueryBuilder buildFinalQueryBuilder(String query, QueryAnalyzerEnum analyzer, String[] matchFields, Integer OnlyToggroupProduct) {
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();


        // 没有替换产品词和品牌词
        TransportClient client = ESClientConfig.getESClient();
        AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(query)
                .setIndex(ElasticSearchConsts.BY_MALL_DERIVE_ALIAS).setAnalyzer(analyzer.getCode()).get();
        for (AnalyzeResponse.AnalyzeToken token : analyzeResponse.getTokens()) {
            // 分词结果多字段匹配
            boolMustQuery.must(QueryBuilders.multiMatchQuery(token.getTerm(), matchFields));
        }

        // 是否只筛选一起拼商品
        if (OnlyToggroupProduct == 1) {
            //boolMustQuery.must(QueryBuilders.termQuery("一起拼", ElasticSearchConsts.TAG_MATCH_FIELDS));
            boolMustQuery.must(QueryBuilders.termQuery("alias", "一起拼"));
        }

        // all字段短语匹配
        boolMustQuery.should(QueryBuilders.matchPhraseQuery("_all", query).boost(5.0f));

        return boolMustQuery;
    }
}
