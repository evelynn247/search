package com.biyao.search.bs.server.remote;

import com.alibaba.fastjson.JSON;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.bs.server.bean.Expression;
import com.biyao.search.bs.server.cache.memory.BrandWordCache;
import com.biyao.search.bs.server.cache.memory.SynonymsCache;
import com.biyao.search.bs.server.cache.memory.TermCombinedWithSexOrSeasonCache;
import com.biyao.search.bs.server.cache.redis.RedisUtil;
import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.common.consts.ExperimentConsts;
import com.biyao.search.bs.server.common.consts.SexLabelConsts;
import com.biyao.search.bs.server.common.util.DclogUtil;
import com.biyao.search.bs.server.experiment.BSExperimentSpace;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.server.rpc.UcRpcService;
import com.biyao.search.bs.server.service.CommonHelperService;
import com.biyao.search.bs.service.ParseService;
import com.biyao.search.bs.service.ProductMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.enums.QueryAnalyzerEnum;
import com.biyao.search.common.model.*;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.base.Strings;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 根据搜索词召回商品
 *
 * @author luozhuo
 * @date long long ago
 */
@Service("productMatch")
public class ProductMatchImpl implements ProductMatch {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    CommonHelperService helperService;

    @Autowired
    RedisUtil redisCache;

    @Autowired
    private RelativeQueryParser relativeQueryParser;

    @Autowired
    private BrandWordCache brandWordCache;

    @Autowired
    private BSExperimentSpace experimentSpace;

    @Autowired
    private ParseService parseService;

    @Autowired
    UcRpcService ucRpcService;

    @Autowired
    private TermCombinedWithSexOrSeasonCache termCombinedWithSexOrSeasonCache;

    @Autowired
    SynonymsCache synonymsCache;


    /**
     * 产品词替换日志消息
     */
    private DCLogger brandWordDclogger = DCLogger.getLogger("brand_word");


    private static final String QUERY_FACET_CACHE = "searchbs_query_facet_";
    private static final int QUERY_FACET_CACHE_TIME = 2 * 60 * 60;

    private static final String MALE = "男";
    private static final String FEMALE = "女";

    @BProfiler(key = "com.biyao.search.bs.server.remote.ProductMatchImpl.match", monitorType = { MonitorType.TP, MonitorType.HEARTBEAT,
            MonitorType.FUNCTION_ERROR })
    @Override
    public RPCResult<ProductMatchResult> match(MatchRequest request) {

        //调用query解析
        //获取性别标签、季节标签
        ParseResponse parseResult = new ParseResponse();
        int sexLabel = SexLabelConsts.NO_GENDER;
        List<String> seasonLabels = new ArrayList<>();
        try {
            parseResult =  parseService.parse(request).getData();
            sexLabel = getSexLabel(parseResult);
            seasonLabels = getSeasonLabel(parseResult);

            //满足条件时调用uc
            //query未识别出性别词且query可以和性别组合 or
            //query未识别出季节词且query可以和季节组合
            boolean unKnownSex = sexLabel == SexLabelConsts.NO_GENDER && termCombinedWithSexOrSeasonCache.canCombinedWithSex(request.getQuery());
            boolean unKnownSeason = seasonLabels.size()== 0 && termCombinedWithSexOrSeasonCache.canCombinedWithSeason(request.getQuery());
            if(unKnownSex || unKnownSeason){
                List<String> fields = new ArrayList<>();
                fields.add(UserFieldConstants.SEASON);
                fields.add(UserFieldConstants.SEX);
                User ucUser = ucRpcService.geUserData(request.getCommonParam().getUuid(), request.getCommonParam().getUid() == null ? "" : request.getCommonParam().getUid().toString(), fields, "searchbs");
                if (ucUser != null) {
                    if (StringUtils.isNotBlank(ucUser.getSeason()) && unKnownSeason) {
                        seasonLabels.add(ucUser.getSeason());
                    }
                    if (ucUser.getSex() != null && unKnownSex) {
                        sexLabel = ucUser.getSex();
                    }
                }
            }
        } catch (Exception e) {
            log.error("调用query解析失败：" + e.getMessage());
        }

        // 切分流量
        experimentSpace.divert(request);
        String[] matchFields = ElasticSearchConsts.MATCH_FIELDS;

        // 发送实验日志消息
        DclogUtil.sendBsExp(request);

        //构建Expression检索表达式
        Expression expression = buildExpression(parseResult);


        /* 分词并建立must查询条件 */
        //QueryBuilder finalQuery = buildFinalQueryBuilder(request, QueryAnalyzerEnum.IK_SMART, matchFields,sexLabel,seasonLabels);
        QueryBuilder finalQuery = buildFinalQueryBuilderByExpression(expression,request,matchFields,sexLabel,seasonLabels);

        SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ElasticSearchConsts.BY_MALL_ALIAS)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(request.getExpectNum())
                .get();

        /*  组装召回结果 */
        SearchHits searchHits = searchResponse.getHits();
        List<SearchItem> searchResult = new ArrayList<SearchItem>();
        Map<String, Set<Object>> attributeMap = new HashMap<>();
        Double minPrice = searchHits.getHits().length == 0 ? 0.0
                : Double.valueOf(searchHits.getHits()[0].getSource().get(ElasticSearchConsts.PRICE_FIELD).toString());
        Double maxPrice = 0.0;
        for (SearchHit hit : searchHits) {
            SearchItem item = new SearchItem();
            item.setProductId(Integer.valueOf(hit.getSource().get("productId").toString()));
            item.setMatchScore(Float.valueOf(hit.getScore()).doubleValue());

            if (searchResult.size() < request.getExpectNum()) {
                searchResult.add(item);
            }

            // 收集并集合商品的属性信息
            Object attribute = hit.getSource().get(ElasticSearchConsts.ATTRIBUTE_FIELD);
            if (attribute != null) {
                Map<String, List<Object>> tmp = (Map<String, List<Object>>) attribute;
                for (String attriKey : tmp.keySet()) {
                    if ((tmp.get(attriKey)).size() == 0) {
                        continue;
                    }
                }
            }
            // 收集商品价格信息
            Double price = Double.valueOf(hit.getSource().get(ElasticSearchConsts.PRICE_FIELD).toString());
            if (minPrice > price) {
                // 当前最小价格比商品价格大
                minPrice = price;
            } else if (maxPrice < price) {
                // 当前最大价格比商品价格小
                maxPrice = price;
            }
        }

        ProductMatchResult result = new ProductMatchResult();
        result.setQuery(request.getQuery());
        result.setItems(searchResult);
        // 有传入facet则不再聚合facet，因为此时的结果不全   改由从缓存中取出
        if (request.getFacets() != null && request.getFacets().size() > 0) {
            List<FacetItem> facetInCache = (List<FacetItem>) redisCache.get(QUERY_FACET_CACHE + request.getQuery());
            if (facetInCache != null) {
                result.setFacets(facetInCache);
            } else {
                result.setFacets(helperService.generateFacets(minPrice, maxPrice, attributeMap));
            }
        } else {
            List<FacetItem> facet = helperService.generateFacets(minPrice, maxPrice, attributeMap);
            result.setFacets(facet);
            redisCache.set(QUERY_FACET_CACHE + request.getQuery(), facet, QUERY_FACET_CACHE_TIME);
        }

//        StringBuilder  sss = new StringBuilder();
//        result.getItems().forEach(item->{
//            if(StringUtils.isBlank(sss.toString())){
//                sss.append(item.getProductId());
//            }else{
//                sss.append(","+item.getProductId());
//            }
//
//
//        });
//        String testString = sss.toString();
        return new RPCResult<ProductMatchResult>(result);
    }

    /**
     * 构建检索表达式
     * @param parseResult
     * @return
     */
    private Expression buildExpression(ParseResponse parseResult) {
        Expression result  = new Expression();
        if(parseResult.getAllTermList() != null){
            parseResult.getAllTermList().forEach(item->{
                List<String> list = synonymsCache.getSynonymsList(item.getTerm());
                //list = new ArrayList<>();
                if(list.size() == 0){
                    list.add(item.getTerm());
                }
                result.getParseList().add(list);
            });
        }

        return result;
    }

    /**
     * 获取季节标签
     * @param parseResult
     */
    private List<String> getSeasonLabel(ParseResponse parseResult) {

        List<String> seasonLabels = new ArrayList<>();
        if(parseResult.getSeasonTerm() != null && parseResult.getSeasonTerm().size()>0){
            parseResult.getSeasonTerm().forEach(item->{
                seasonLabels.add(item.getTerm());
            });
        }
        return seasonLabels;
    }

    /**
     * 获取性别标签
     * @param parseResult
     */
    private Integer getSexLabel(ParseResponse parseResult) {

        boolean containsMale = false;
        boolean containsFamale = false;
        for (QueryTerm term : parseResult.getSexTerm()) {
            if (term.getTerm().contains(MALE)) {
                containsMale = true;
            }

            if (term.getTerm().contains(FEMALE)) {
                containsFamale = true;
            }
        }
        // 均为true时说明判断不出男女
        return  (containsMale && containsFamale) ? SexLabelConsts.NO_GENDER : (containsMale) ? SexLabelConsts.MALE : (containsFamale) ? SexLabelConsts.FEMALE : SexLabelConsts.NO_GENDER;

    }

    /**
     * 根据query构造ES QueryBuilder
     *
     * @param request
     * @param analyzer
     * @param sexLabel
     * @param seasonLabels
     * @return
     */
    private QueryBuilder buildFinalQueryBuilder(MatchRequest request, QueryAnalyzerEnum analyzer, String[] matchFields, int sexLabel, List<String> seasonLabels) {
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
        // 特殊处理 20180622 有facet时，剔除修饰词，用产品词搜索
        boolean productWordExist = false;
        boolean brandWordExist = false;
        if (request.getFacets() != null && request.getFacets().size() > 0) {
            String productWord = relativeQueryParser.getProductWord(request.getQuery());
            if (!Strings.isNullOrEmpty(productWord)) {
                productWordExist = true;
                boolMustQuery.must(QueryBuilders.multiMatchQuery(productWord, matchFields));
            }
        }
        //品牌词逻辑处理
        try {
            if (!productWordExist) {
                List<String> brandWordList = brandWordCache.getBrandWordList(request.getQuery());
                if (brandWordList != null && brandWordList.size() > 0) {
                    brandWordExist = true;
                    // 分词结果多字段匹配
                    for (String term : brandWordList) {
                        boolMustQuery.should(QueryBuilders.multiMatchQuery(term, matchFields));
                    }
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("lt", "brand_word");
                    map.put("lv", "1.0");
                    map.put("uu", request.getCommonParam().getUuid());
                    map.put("u", request.getCommonParam().getUid());
                    map.put("pf", request.getCommonParam().getPlatform());
                    map.put("query", request.getQuery());
                    map.put("rew_word", JSON.toJSONString(brandWordList));
                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    map.put("st", sdf.format(d));
                    DclogUtil.sendDclog(brandWordDclogger, map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("调用品牌词失败：{}", e);
        }

        if (!productWordExist && !brandWordExist) {
            // 没有替换产品词和品牌词
            TransportClient client = ESClientConfig.getESClient();
            AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(request.getQuery())
                    .setIndex(ElasticSearchConsts.BY_MALL_ALIAS).setAnalyzer(analyzer.getCode()).get();
            for (AnalyzeToken token : analyzeResponse.getTokens()) {
                // 分词结果多字段匹配
                boolMustQuery.must(QueryBuilders.multiMatchQuery(token.getTerm(), matchFields));
            }
        }

        // all字段短语匹配
        boolMustQuery.should(QueryBuilders.matchPhraseQuery("_all", request.getQuery()).boost(5.0f));

        // facet条件匹配
        if (request.getFacets() != null && request.getFacets().size() > 0) {
            for (FacetItem facet : request.getFacets()) {
                if (ElasticSearchConsts.PRICE_FACET_KEY.equals(facet.getKey())) {
                    // 价格特殊处理
                    BoolQueryBuilder priceRangeQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        String[] prices = value.split(" - ");
                        Double from = Double.valueOf(prices[0]);
                        Double to = Double.valueOf(prices[1]);
                        priceRangeQuery.should(QueryBuilders.rangeQuery(ElasticSearchConsts.PRICE_FIELD).gte(from).lte(to));
                    }

                    boolMustQuery.must(priceRangeQuery);
                } else if (ElasticSearchConsts.ACTIVITY_FACET_KEY.equals(facet.getKey())) {
                    // 活动筛选条件
                    BoolQueryBuilder activityQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        activityQuery.should(QueryBuilders.multiMatchQuery(value, ElasticSearchConsts.TAG_MATCH_FIELDS));
                    }

                    boolMustQuery.must(activityQuery);
                } else {
                    BoolQueryBuilder facetQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        facetQuery.should(QueryBuilders.multiMatchQuery(value, matchFields));
                    }

                    boolMustQuery.must(facetQuery);
                }
            }
        }

        //性别过滤
        //性别、季节因素加权
        if (sexLabel == SexLabelConsts.MALE){
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, FEMALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, MALE).boost(10.0f));
        }else if (sexLabel == SexLabelConsts.FEMALE){
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, MALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, FEMALE).boost(10.0f));
        }
        if (seasonLabels.size() > 0) {
            seasonLabels.forEach(item -> {
                boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEASON_LABEL_FIELD, item).boost(10.0f));
            });
        }

        return boolMustQuery;
    }

    private QueryBuilder buildFinalQueryBuilderByExpression(Expression expression,MatchRequest request, String[] matchFields, int sexLabel, List<String> seasonLabels) {

        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();

        //原产品词逻辑及品牌词逻辑（因一起拼搜索可能用到，暂时先保留）
        boolean productWordExist = false;
        boolean brandWordExist = false;
        if (request.getFacets() != null && request.getFacets().size() > 0) {
            String productWord = relativeQueryParser.getProductWord(request.getQuery());
            if (!Strings.isNullOrEmpty(productWord)) {
                productWordExist = true;
                boolMustQuery.must(QueryBuilders.multiMatchQuery(productWord, matchFields));
            }
        }
        //品牌词逻辑处理
        try {
            if (!productWordExist) {
                List<String> brandWordList = brandWordCache.getBrandWordList(request.getQuery());
                if (brandWordList != null && brandWordList.size() > 0) {
                    brandWordExist = true;
                    // 分词结果多字段匹配
                    for (String term : brandWordList) {
                        boolMustQuery.should(QueryBuilders.multiMatchQuery(term, matchFields));
                    }
                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("lt", "brand_word");
                    map.put("lv", "1.0");
                    map.put("uu", request.getCommonParam().getUuid());
                    map.put("u", request.getCommonParam().getUid());
                    map.put("pf", request.getCommonParam().getPlatform());
                    map.put("query", request.getQuery());
                    map.put("rew_word", JSON.toJSONString(brandWordList));
                    Date d = new Date();
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    map.put("st", sdf.format(d));
                    DclogUtil.sendDclog(brandWordDclogger, map);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("调用品牌词失败：{}", e);
        }


        // all字段短语匹配
        boolMustQuery.should(QueryBuilders.matchPhraseQuery("_all", request.getQuery()).boost(5.0f));

        //检索表达式匹配
        if (!productWordExist && !brandWordExist) {
            //parseList集合中为必须参数，作为must条件
            if (expression.getParseList() != null) {
                expression.getParseList().forEach(items -> {
                    BoolQueryBuilder mustQueryBuilder = QueryBuilders.boolQuery();
                    items.forEach(item -> {
                        mustQueryBuilder.should(QueryBuilders.multiMatchQuery(item, ElasticSearchConsts.MATCH_FIELDS));
                    });
                    boolMustQuery.must(mustQueryBuilder);
                });
            }

            //boostList集合中为提权参数，作为should条件
            if (expression.getBoostList() != null) {
                expression.getBoostList().forEach(items -> {
                    BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
                    items.forEach(item -> {
                        shouldQueryBuilder.should(QueryBuilders.multiMatchQuery(item, ElasticSearchConsts.MATCH_FIELDS));
                    });
                    boolMustQuery.should(shouldQueryBuilder);
                });
            }
        }

        // facet条件匹配（因一起拼搜索可能用到，暂时先保留）
        if (request.getFacets() != null && request.getFacets().size() > 0) {
            for (FacetItem facet : request.getFacets()) {
                if (ElasticSearchConsts.PRICE_FACET_KEY.equals(facet.getKey())) {
                    // 价格特殊处理
                    BoolQueryBuilder priceRangeQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        String[] prices = value.split(" - ");
                        Double from = Double.valueOf(prices[0]);
                        Double to = Double.valueOf(prices[1]);
                        priceRangeQuery.should(QueryBuilders.rangeQuery(ElasticSearchConsts.PRICE_FIELD).gte(from).lte(to));
                    }

                    boolMustQuery.must(priceRangeQuery);
                } else if (ElasticSearchConsts.ACTIVITY_FACET_KEY.equals(facet.getKey())) {
                    // 活动筛选条件
                    BoolQueryBuilder activityQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        activityQuery.should(QueryBuilders.multiMatchQuery(value, ElasticSearchConsts.TAG_MATCH_FIELDS));
                    }

                    boolMustQuery.must(activityQuery);
                } else {
                    BoolQueryBuilder facetQuery = QueryBuilders.boolQuery();
                    for (String value : facet.getValues()) {
                        facetQuery.should(QueryBuilders.multiMatchQuery(value, matchFields));
                    }

                    boolMustQuery.must(facetQuery);
                }
            }
        }
        //性别过滤
        //性别、季节因素加权
        if (sexLabel == SexLabelConsts.MALE){
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, FEMALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, MALE).boost(10.0f));
        }else if (sexLabel == SexLabelConsts.FEMALE){
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, MALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, FEMALE).boost(10.0f));
        }
        if (seasonLabels.size() > 0) {
            seasonLabels.forEach(item -> {
                boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEASON_LABEL_FIELD, item).boost(10.0f));
            });
        }
        return boolMustQuery;
    }

}
