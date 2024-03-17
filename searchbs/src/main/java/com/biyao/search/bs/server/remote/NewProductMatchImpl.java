package com.biyao.search.bs.server.remote;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.bs.server.bean.Expression;
import com.biyao.search.bs.server.cache.memory.*;
import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.common.consts.ExperimentConsts;
import com.biyao.search.bs.server.common.consts.SexLabelConsts;
import com.biyao.search.bs.server.common.enums.SexLabelEnum;
import com.biyao.search.bs.server.common.util.DclogUtil;
import com.biyao.search.bs.server.common.util.ESUtil;
import com.biyao.search.bs.server.experiment.BSExperimentSpace;
import com.biyao.search.bs.server.rpc.UcRpcService;
import com.biyao.search.bs.service.NewProductMatch;
import com.biyao.search.bs.service.ParseService;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.model.ParseResponse;
import com.biyao.search.common.model.QueryTerm;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.SearchItem;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.uc.domain.bean.User;
import com.uc.domain.constant.UserFieldConstants;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
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
 * @author zj
 * @version 1.0
 * @date 2020/2/21 16:15
 * @description
 */
@Service(value = "newProductMatch")
public class NewProductMatchImpl implements NewProductMatch {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private ParseService parseService;

    @Autowired
    UcRpcService ucRpcService;

    @Autowired
    SynonymsCache synonymsCache;

    @Autowired
    TermCombinedWithSexOrSeasonCache termCombinedWithSexOrSeasonCache;

    @Autowired
    private BSExperimentSpace experimentSpace;

    @Autowired
    private BrandWordCache brandWordCache;

    @Autowired
    private RedisCache redisCache;

    @Autowired
    private ReletedWordCache reletedWordCache;
    /**
     * 产品词替换日志消息
     */
    private DCLogger brandWordDclogger = DCLogger.getLogger("brand_word");

    @BProfiler(key = "com.biyao.search.bs.server.remote.NewProductMatchImpl.match", monitorType = {MonitorType.TP, MonitorType.HEARTBEAT,
            MonitorType.FUNCTION_ERROR})
    @Override
    public RPCResult<List<ProductMatchResult>> match(MatchRequest newMatchRequest) {
        //白名单用户打印search_detail_log
        if (redisCache.isSearchDetailLogUuid(newMatchRequest.getCommonParam().getUuid())) {
            DclogUtil.printSearchDetailLog("searchbs-request", newMatchRequest.getCommonParam().getUuid(), newMatchRequest.getCommonParam().getSid(), JSON.toJSONString(newMatchRequest));
        }
        String query = newMatchRequest.getQuery();

        List<ProductMatchResult> result = new ArrayList<>();

        //调用query解析
        //获取性别标签、季节标签
        ParseResponse parseResult = new ParseResponse();
        int sexLabel = SexLabelConsts.NO_GENDER;
        List<String> seasonLabels = new ArrayList<>();
        try {
            parseResult = parseService.parse(newMatchRequest).getData();
            sexLabel = getSexLabel(parseResult);
            seasonLabels = getTerms(parseResult.getSeasonTerm());
            //满足条件时调用uc
            //query未识别出性别词且query可以和性别组合 or
            //query未识别出季节词且query可以和季节组合
            boolean unKnownSex = sexLabel == SexLabelConsts.NO_GENDER && termCombinedWithSexOrSeasonCache.canCombinedWithSex(query);
            boolean unKnownSeason = seasonLabels.size() == 0 && termCombinedWithSexOrSeasonCache.canCombinedWithSeason(query);
            if (unKnownSex || unKnownSeason) {
                List<String> fields = new ArrayList<>();
                fields.add(UserFieldConstants.SEASON);
                fields.add(UserFieldConstants.SEX);
                User ucUser = ucRpcService.geUserData(newMatchRequest.getCommonParam().getUuid(), newMatchRequest.getCommonParam().getUid() == null ? "" : newMatchRequest.getCommonParam().getUid().toString(), fields, "searchbs");
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

        // 实验切分流量
        experimentSpace.divert(newMatchRequest);

        // 发送实验日志消息
        DclogUtil.sendBsExp(newMatchRequest);

        //构造检索表达式
        Expression expression = buildExpression(parseResult);
        //白名单用户打印search_detail_log,打印query解析结果，依赖前面的结果，不能随意调整打印日志的位置
        if (redisCache.isSearchDetailLogUuid(newMatchRequest.getCommonParam().getUuid())) {
            Set<List<String>> synonymTerms = new HashSet<>();
            synonymTerms.addAll(expression.getParseList());
            synonymTerms.add(expression.getProductWord());
            JSONObject logBody = new JSONObject();
            logBody.put("query", query);
            logBody.put("allTerms", getTerms(parseResult.getAllTermList()));
            logBody.put("productTerm", getTerms(parseResult.getProductTerm()));
            logBody.put("sexTerm", SexLabelEnum.getDescByCode(sexLabel));
            logBody.put("seasonTerms", seasonLabels);
            logBody.put("synonymTerms", synonymTerms);
            DclogUtil.printSearchDetailLog("searchbs-parse", newMatchRequest.getCommonParam().getUuid(), newMatchRequest.getCommonParam().getSid(), JSON.toJSONString(logBody));
        }
        String[] matchFields= ElasticSearchConsts.MATCH_FIELDS;
        //小程序A则支持活动标签搜索,匹配字段增加activityTag
        if ("miniappA".equals(newMatchRequest.getCommonParam().getPlatform().getName())) {
            matchFields = ElasticSearchConsts.ACTIVITY_TAG_MATCH_FIELDS;
        }
        //获取完全匹配内容
        ProductMatchResult completelyResult = getCompletelyItems(expression, newMatchRequest, matchFields, sexLabel, seasonLabels);

        //记录召回商品，用于去重
        List<Integer> matchPids = new ArrayList<>();
        completelyResult.getItems().forEach(item -> {
            matchPids.add(item.getProductId());
        });
        List<SearchItem> partialMatchList = new ArrayList<>();
        //获取部分匹配内容
        ProductMatchResult partialResult = new ProductMatchResult();
        if (newMatchRequest.getIsGetPartial() == 1) {
            for (QueryTerm queryTerm : parseResult.getAllTermList()) {
                //性别词不作为单独部分匹配条件
                if (CommonConsts.femaleList.contains(queryTerm.getTerm()) || CommonConsts.maleList.contains(queryTerm.getTerm())) {
                    continue;
                }

                ProductMatchResult tempPartialResult = getPartialItems(queryTerm.getTerm(), newMatchRequest, matchFields, sexLabel, seasonLabels, matchPids, expression.getProductWord());
                if (tempPartialResult.getItems().size() > 0) {
                    //如果term词==原始query，所有召回商品加入完全匹配模块
                    if (query.equals(tempPartialResult.getQuery())) {
                        completelyResult.getItems().addAll(tempPartialResult.getItems());
                        //如果term词！=原始query，所有召回商品加入部分匹配模块
                    } else {
                        partialResult.getItems().addAll(tempPartialResult.getItems());
                        partialResult.setQuery(tempPartialResult.getQuery());
                    }
                    partialMatchList.addAll(tempPartialResult.getItems());
                    tempPartialResult.getItems().forEach(item -> {
                        matchPids.add(item.getProductId());
                    });
                }
                if (partialMatchList.size() > newMatchRequest.getPartialExpectNum()) {
                    //超出部分匹配数量限制直接跳出，不再召回
                    break;
                }
            }
        }
        //相关词召回(实验控制)
        ProductMatchResult tempPartialResult = new ProductMatchResult();
        if (experimentSpace.hitExp(ExperimentConsts.FLAG_USE_RELATED_MATCH, ExperimentConsts.VALUE_USE_RELATED_MATCH, newMatchRequest)) {
            if(newMatchRequest.getIsGetPartial() == 1){
                if(parseResult.getProductTerm().size() > 0){
                    Set<String> relatedWordSet = new HashSet<>();
                    for (QueryTerm term : parseResult.getProductTerm()) {
                        relatedWordSet.addAll(reletedWordCache.getReletedList(term.getTerm()));
                    }
                    if(relatedWordSet.size() > 0){
                        tempPartialResult = getRelatedItems(newMatchRequest,relatedWordSet, sexLabel, seasonLabels, matchPids);
                    }
                }
            }
        }
        if (completelyResult.getItems().size() > 0) {
            result.add(completelyResult);
        }
        if (partialResult.getItems().size() > 0) {
            result.add(partialResult);
        }
        if (tempPartialResult.getItems().size() > 0) {
            result.add(tempPartialResult);
        }
        //白名单用户打印search_detail_log
        if (redisCache.isSearchDetailLogUuid(newMatchRequest.getCommonParam().getUuid())) {
            DclogUtil.printSearchDetailLog("searchbs-response", newMatchRequest.getCommonParam().getUuid(), newMatchRequest.getCommonParam().getSid(), JSON.toJSONString(result));
        }
        return new RPCResult<>(result);
    }

    /**
     * 构建检索表达式
     *
     * @param parseResult
     * @return
     */
    private Expression buildExpression(ParseResponse parseResult) {
        Expression result = new Expression();
        if (parseResult.getAllTermList() != null) {
            parseResult.getAllTermList().forEach(item -> {
                List<String> list = synonymsCache.getSynonymsList(item.getTerm());
                result.getParseList().add(list);
            });
        }

        //为检索表达式中产品词条件赋值
        if (parseResult.getProductTerm().size() > 0) {
            String productWord = parseResult.getProductTerm().get(0).getTerm();
            List<String> list = synonymsCache.getSynonymsList(productWord);
            result.setProductWord(list);
        }

        return result;
    }

    /**
     * 获取es索引完全匹配结果
     *
     * @param expression
     * @return
     */
    private ProductMatchResult getCompletelyItems(Expression expression, MatchRequest request, String[] matchFields, int sexLabel, List<String> seasonLabels) {
        ProductMatchResult result = new ProductMatchResult();
        result.setQuery(request.getQuery());
        //根据检索表达式构造QueryBuild，条件之间用and连接,如onlyTogetherGroupProduct=1，则添加活动=一起拼条件
        QueryBuilder finalQuery = buildCompletelyQueryBuilder(expression, request, matchFields, sexLabel, seasonLabels);

        //调用es，构建返回结果
        SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ElasticSearchConsts.BY_MALL_ALIAS)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(request.getCompletelyExpectNum())
                .get();

        SearchHits searchHits = searchResponse.getHits();
        List<SearchItem> searchResult = new ArrayList<SearchItem>();
        for (SearchHit hit : searchHits) {
            SearchItem item = new SearchItem();
            item.setProductId(Integer.valueOf(hit.getSource().get("productId").toString()));
            item.setMatchScore(Float.valueOf(hit.getScore()).doubleValue());
            item.setSemStr(getSemStr("allmatch",request.getExpIds()));
            searchResult.add(item);
        }
        result.setItems(searchResult);

        return result;
    }

    private QueryBuilder buildCompletelyQueryBuilder(Expression expression, MatchRequest request, String[] matchFields, int sexLabel, List<String> seasonLabels) {
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();

        //品牌词逻辑处理
        boolean brandWordExist = isbrandWord(request, boolMustQuery);

        // all字段短语匹配
        boolMustQuery.should(QueryBuilders.matchPhraseQuery("_all", request.getQuery()).boost(5.0f));

        //检索表达式匹配
        if (!brandWordExist) {
            //parseList集合中为必须参数，作为must条件
            if (expression.getParseList() != null) {
                expression.getParseList().forEach(items -> {
                    BoolQueryBuilder mustQueryBuilder = QueryBuilders.boolQuery();
                    items.forEach(item -> {
                        mustQueryBuilder.should(QueryBuilders.multiMatchQuery(item, matchFields));
                    });
                    boolMustQuery.must(mustQueryBuilder);
                });
            }

            //boostList集合中为提权参数，作为should条件
            if (expression.getBoostList() != null) {
                expression.getBoostList().forEach(items -> {
                    BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
                    items.forEach(item -> {
                        shouldQueryBuilder.should(QueryBuilders.multiMatchQuery(item, matchFields));
                    });
                    boolMustQuery.should(shouldQueryBuilder);
                });
            }
        }

        if (StringUtils.isNotEmpty(request.getQuery())) {
            switch (request.getAliasType()) {
                case 1:
                    //一起拼筛选
                    boolMustQuery.must(QueryBuilders.multiMatchQuery("一起拼", ElasticSearchConsts.TAG_MATCH_FIELDS));
                    break;
                case 4:
                    //津贴抵扣筛选
                    boolMustQuery.must(QueryBuilders.multiMatchQuery("津贴抵扣", ElasticSearchConsts.TAG_MATCH_FIELDS));
                    if(request.getIsSupportCreation() != null && request.getIsSupportCreation() == CommonConsts.IS_SUPPORT_CREATION_CON_2){
                        boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.IS_CREATOR_CON, ElasticSearchConsts.IS_CREATOR_FLAG_1));
                    }
                    break;
                case 6:
                    //店铺内搜索
                    if (request.getSupplierId() != null) {
                        boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.SUPPLIER_ID, request.getSupplierId()));
                    }
                    break;
                default:
                    break;
            }
        }
        //性别过滤
        //性别、季节因素加权
        if (sexLabel == SexLabelConsts.MALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE).boost(10.0f));
        } else if (sexLabel == SexLabelConsts.FEMALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE).boost(10.0f));
        }
        if (seasonLabels.size() > 0) {
            seasonLabels.forEach(item -> {
                boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEASON_LABEL_FIELD, item).boost(10.0f));
            });
        }


        if (expression.getProductWord().size() > 0) {
            BoolQueryBuilder shouldQueryBuilder = QueryBuilders.boolQuery();
            expression.getProductWord().forEach(item -> {
                shouldQueryBuilder.should(QueryBuilders.multiMatchQuery(item, ElasticSearchConsts.PRODUCT_WOED));
            });
            boolMustQuery.should(shouldQueryBuilder);
        }

        //分销商品池搜索传1
        if (StringUtils.isNotEmpty(request.getProductPool())) {
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.PRODUCT_POOL, request.getProductPool()));
        }

        //【商品管理V1.0-支持渠道专属商品】必要主站、分销过滤不支持当前渠道的商品 20220407
        ESUtil.buildSupportChannel(boolMustQuery,request.getProductPool());

        return boolMustQuery;
    }


    /**
     * 获取es索引部分匹配结果
     *
     * @return
     */
    private ProductMatchResult getPartialItems(String query, MatchRequest request, String[] matchFields, int sexLabel, List<String> seasonLabels, List<Integer> completelyMatchPids, List<String> productWordList) {
        ProductMatchResult result = new ProductMatchResult();
        result.setQuery(query);
        //根据检索表达式构造QueryBuild，条件之间用and连接,如onlyTogetherGroupProduct=1，则添加活动=一起拼条件
        QueryBuilder finalQuery = buildPartialQueryBuilder(query, request, matchFields, sexLabel, seasonLabels, productWordList);

        //调用es，构建返回结果
        SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ElasticSearchConsts.BY_MALL_ALIAS)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(request.getPartialExpectNum() / 2)
                .get();

        SearchHits searchHits = searchResponse.getHits();
        List<SearchItem> searchResult = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            if (!completelyMatchPids.contains(Integer.valueOf(hit.getSource().get("productId").toString()))) {
                SearchItem item = new SearchItem();
                item.setProductId(Integer.valueOf(hit.getSource().get("productId").toString()));
                item.setMatchScore(Float.valueOf(hit.getScore()).doubleValue());
                item.setSemStr(getSemStr("partmatch",request.getExpIds()));
                searchResult.add(item);
            }
        }
        result.setItems(searchResult);

        return result;
    }

    private QueryBuilder buildPartialQueryBuilder(String query, MatchRequest request, String[] matchFields, int sexLabel, List<String> seasonLabels, List<String> productWordList) {
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
        // all字段短语匹配
        boolMustQuery.should(QueryBuilders.matchPhraseQuery("_all", query).boost(5.0f));
        //使用检索表达式形式，支持同义词
        List<String> list = synonymsCache.getSynonymsList(query);
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        list.forEach(item -> {
            queryBuilder.should(QueryBuilders.multiMatchQuery(item, matchFields));
        });
        boolMustQuery.must(queryBuilder);

        if (productWordList.size() > 0) {
            BoolQueryBuilder mustQueryBuilder = QueryBuilders.boolQuery();
            productWordList.forEach(item -> {
                mustQueryBuilder.should(QueryBuilders.multiMatchQuery(item, ElasticSearchConsts.PRODUCT_MATCH_FIELDS));
            });
            boolMustQuery.must(mustQueryBuilder);
        }

        //店铺内搜索
        if (request.getAliasType() == 6 && request.getSupplierId() != null) {
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.SUPPLIER_ID, request.getSupplierId()));
        }
        //性别过滤
        //性别、季节因素加权
        if (sexLabel == SexLabelConsts.MALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE).boost(10.0f));
        } else if (sexLabel == SexLabelConsts.FEMALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE).boost(10.0f));
        }
        if (seasonLabels.size() > 0) {
            seasonLabels.forEach(item -> {
                boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEASON_LABEL_FIELD, item).boost(10.0f));
            });
        }


        //分销商品池搜索传1
        if (StringUtils.isNotEmpty(request.getProductPool())) {
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.PRODUCT_POOL, request.getProductPool()));
        }

        //【商品管理V1.0-支持渠道专属商品】必要主站、分销过滤不支持当前渠道的商品 20220407
        ESUtil.buildSupportChannel(boolMustQuery,request.getProductPool());

        return boolMustQuery;
    }

    /**
     * 是否命中品牌词改写
     *
     * @param request
     * @param boolMustQuery
     * @return
     */
    private boolean isbrandWord(MatchRequest request, BoolQueryBuilder boolMustQuery) {
        boolean brandWordExist = false;

        try {
            List<String> brandWordList = brandWordCache.getBrandWordList(request.getQuery());
            if (brandWordList != null && brandWordList.size() > 0) {
                brandWordExist = true;
                // 分词结果多字段匹配
                for (String term : brandWordList) {
                    boolMustQuery.should(QueryBuilders.multiMatchQuery(term, ElasticSearchConsts.MATCH_FIELDS));
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

                //白名单用户打印search_detail_log
                if (redisCache.isSearchDetailLogUuid(request.getCommonParam().getUuid())) {
                    JSONObject brandLogBody = new JSONObject();
                    brandLogBody.put("query", request.getQuery());
                    brandLogBody.put("brandRewriteTerms", brandWordList);
                    DclogUtil.printSearchDetailLog("searchbs-brandRewriteTerms", request.getCommonParam().getUuid(), request.getCommonParam().getSid(), JSONObject.toJSONString(brandLogBody));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            log.error("调用品牌词失败:", e);
        }
        return brandWordExist;
    }


    /**
     * 获取季节标签
     *
     * @param queryTerms
     */
    private List<String> getTerms(List<QueryTerm> queryTerms) {

        List<String> terms = new ArrayList<>();
        if (queryTerms != null && queryTerms.size() > 0) {
            queryTerms.forEach(item -> {
                terms.add(item.getTerm());
            });
        }
        return terms;
    }

    /**
     * 获取性别标签
     *
     * @param parseResult
     */
    private Integer getSexLabel(ParseResponse parseResult) {

        boolean containsMale = false;
        boolean containsFamale = false;
        for (QueryTerm term : parseResult.getAllTermList()) {
            if (CommonConsts.maleList.contains(term.getTerm())) {
                containsMale = true;
            }

            if (CommonConsts.femaleList.contains(term.getTerm())) {
                containsFamale = true;
            }
        }
        // 均为true时说明判断不出男女
        return (containsMale && containsFamale) ? SexLabelConsts.NO_GENDER : (containsMale) ? SexLabelConsts.MALE : (containsFamale) ? SexLabelConsts.FEMALE : SexLabelConsts.NO_GENDER;

    }

    /**
     * 拼接召回标识和实验号到sem参数中
     * 召回标识   allmatch代表完全匹配召回商品，partmatch代表部分匹配召回商品，relatmatch代表相关商品召回，cnxh代表猜你喜欢召回
     * 格式   allmatch:1001_2003_3001
     * @param reCallType
     * @param expIds
     * @return
     */
    private String getSemStr(String reCallType, List<Integer> expIds) {
        StringBuilder result = new StringBuilder();
        result.append(reCallType);
        if(expIds == null || expIds.size() == 0){
            return result.toString();
        }
        for (Integer id:expIds) {
            result.append(",").append(id);
        }
        return result.toString();
    }

    /**
     * 获取相关词召回结果
     * @param relatedWordSet
     * @param sexLabel
     * @param seasonLabels
     * @param matchPids
     * @return
     */
    private ProductMatchResult getRelatedItems(MatchRequest request,Set<String> relatedWordSet, int sexLabel, List<String> seasonLabels, List<Integer> matchPids) {

        ProductMatchResult result = new ProductMatchResult();
        result.setQuery("");
        //根据检索表达式构造QueryBuild，条件之间用and连接,如onlyTogetherGroupProduct=1，则添加活动=一起拼条件
        QueryBuilder finalQuery = buildRelatedQueryBuilder(request,relatedWordSet, ElasticSearchConsts.RELETED_MATCH_FIELDS, sexLabel, seasonLabels);

        //调用es，构建返回结果
        SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ElasticSearchConsts.BY_MALL_ALIAS)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(50) //相关商品召回数量暂时写死50
                .get();

        SearchHits searchHits = searchResponse.getHits();
        List<SearchItem> searchResult = new ArrayList<>();
        for (SearchHit hit : searchHits) {
            if (!matchPids.contains(Integer.valueOf(hit.getSource().get("productId").toString()))) {
                SearchItem item = new SearchItem();
                item.setProductId(Integer.valueOf(hit.getSource().get("productId").toString()));
                item.setMatchScore(Float.valueOf(hit.getScore()).doubleValue());
                item.setSemStr(getSemStr("rematch",request.getExpIds()));
                searchResult.add(item);
            }
        }
        result.setItems(searchResult);

        return result;
    }

    /**
     * 构建相关词queryBuilder
     * @param relatedWordSet
     * @param matchFields
     * @param sexLabel
     * @param seasonLabels
     * @return
     */
    private QueryBuilder buildRelatedQueryBuilder(MatchRequest request,Set<String> relatedWordSet, String[] matchFields, int sexLabel, List<String> seasonLabels) {
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();

        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        relatedWordSet.forEach(item -> {
            queryBuilder.should(QueryBuilders.multiMatchQuery(item, matchFields));
        });
        boolMustQuery.must(queryBuilder);

        //店铺内搜索
        if (request.getAliasType() == 6 && request.getSupplierId() != null) {
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.SUPPLIER_ID, request.getSupplierId()));
        }

        //性别过滤
        //性别、季节因素加权
        if (sexLabel == SexLabelConsts.MALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE).boost(10.0f));
        } else if (sexLabel == SexLabelConsts.FEMALE) {
            boolMustQuery.mustNot(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.MALE));
            boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEX_LABEL_FIELD, CommonConsts.FEMALE).boost(10.0f));
        }
        if (seasonLabels.size() > 0) {
            seasonLabels.forEach(item -> {
                boolMustQuery.should(QueryBuilders.termQuery(ElasticSearchConsts.SEASON_LABEL_FIELD, item).boost(10.0f));
            });
        }

        //【商品管理V1.0-支持渠道专属商品】必要主站、分销过滤不支持当前渠道的商品
        ESUtil.buildSupportChannel(boolMustQuery,request.getProductPool());

        return boolMustQuery;
    }

}
