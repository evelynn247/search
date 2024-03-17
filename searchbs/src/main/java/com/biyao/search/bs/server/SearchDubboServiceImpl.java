package com.biyao.search.bs.server;

import com.biyao.dclog.service.DCLogger;
import com.biyao.search.bs.server.bean.ASQuerySegment;
import com.biyao.search.bs.server.cache.guava.detail.ProductTagsCache;
import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.common.util.ESUtil;
import com.biyao.search.bs.server.experiment.BSExperimentSpace;
import com.biyao.search.bs.server.match.MatchRequest;
import com.biyao.search.bs.server.match.MatchResponse;
import com.biyao.search.bs.server.match.impl.TagsMushMatch;
import com.biyao.search.bs.server.query.Query;
import com.biyao.search.bs.server.query.QueryParser;
import com.biyao.search.bs.service.BSSearchService;
import com.biyao.search.bs.service.model.request.BSSearchRequest;
import com.biyao.search.bs.service.model.response.BSHiResponse;
import com.biyao.search.bs.service.model.response.BSSearchResponse;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.LabelTag;
import com.biyao.search.common.model.RPCResult;
import com.google.common.base.Joiner;
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

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;



/**
 * @description: 搜索dubbo服务实现
 * @author: luozhuo
 * @version: V1.0.0
 * <p>
 * 2018-05-15 注释掉扩展词实验、分词日志
 */
@Service("bsSearchService")
public class SearchDubboServiceImpl implements BSSearchService {

    @Autowired
    private ProductTagsCache productTagsCache;

    //es商品类型名称
    private static final String ES_PRODUCT_TYPE_NAME = "product";
    //搜索时分词器
    private static final String SEARCH_ANALYZER_TYPE = "ik_smart";

    private Logger logger = LoggerFactory.getLogger(getClass());

    // bs标准日志
    private DCLogger matchLogger = DCLogger.getLogger("searchbs_match_request");

    private static final Joiner commaJoiner = Joiner.on(',').skipNulls();
    private static final Joiner underLineJoiner = Joiner.on('_').skipNulls();

    @Resource(name = "segmentMarkQueryParser")
    private QueryParser segmentMarkQueryParser;

    @Autowired
    TagsMushMatch tagsMushMatch;

    @Autowired
    BSExperimentSpace bsExperimentSpace;


    @Override
    public RPCResult<BSSearchResponse<ASProduct>> match(BSSearchRequest bsSearchRequest) {
        long start = System.currentTimeMillis();

        // 实验初始化
        try {
            bsSearchRequest = bsExperimentSpace.getExperimentSpace().divert(bsSearchRequest);
        } catch (Exception e) {
            logger.error("流量切分失败: ", e);
        }

        ASQuerySegment asQuerySegment = new ASQuerySegment();
        BSSearchResponse<ASProduct> bsSearchResponse = new BSSearchResponse<ASProduct>();
        TransportClient client = ESClientConfig.getESClient();
        SearchResponse searchResponse = null;

        // 构造MatchRequest
        MatchRequest matchRequest = new MatchRequest();
        matchRequest.setBsSearchRequest(bsSearchRequest);

        // 构造Query对象
        Query query = new Query(bsSearchRequest.getQuery());
        if (isTagsProduct(query)) {
            //标签召回
            matchRequest.setQuery(query);
            MatchResponse matchResponse = tagsMushMatch.match(matchRequest);
            List<ASProduct> asProductResult = new ArrayList<>();
            for (List<ASProduct> asProductList : matchResponse.getMultiAsProductList()) {
                asProductResult.addAll(asProductList);
            }

            bsSearchResponse.setResult(asProductResult);
            bsSearchResponse.setHitTotal(asProductResult.size());

            int esTookInMillis = 0;
            for (SearchResponse matchSearchResponse : matchResponse.getSearchResponseList()) {
                esTookInMillis += matchSearchResponse.getTookInMillis();
            }

            bsSearchResponse.setEsTookTime(esTookInMillis);
        } else {
            // 分词、标记分词类型
            segmentMarkQueryParser.parse(query);

            matchRequest.setQuery(query);

            /**
             * 一级策略	分词均匹配，完全匹配时排名靠前
             */
            SearchResponse mustMatchResponse = getMustMatchSearchResponse(client, bsSearchRequest, asQuerySegment);
            searchResponse = mustMatchResponse;
            SearchHits mustMatchHits = mustMatchResponse.getHits();
            /**
             * 二级策略	分词个别匹配
             */
            if (mustMatchHits.getTotalHits() == 0) {
                SearchResponse shouldMatchResponse = getShouldMatchSearchResponse(client, bsSearchRequest);
                searchResponse = shouldMatchResponse;
            }

            /**
             * 组装返回结果
             */
            List<ASProduct> asProducts = new ArrayList<ASProduct>();
            SearchHits searchHits = searchResponse.getHits();
            for (SearchHit searchHit : searchHits) {
                ASProduct asProduct = new ASProduct();
                asProduct.setSuId(searchHit.getSource().get("suId").toString());
                asProduct.setShortTitle(searchHit.getSource().get("shortTitle").toString());
                asProduct.setFullTitle(searchHit.getSource().get("title").toString());
                asProduct.setPrice(Float.valueOf(searchHit.getSource().get("price").toString()));
                asProduct.setImage(searchHit.getSource().get("image").toString());
                // 默认设为1
                asProduct.setSaleMode(1);
                asProduct.setGroupPrice(Float.valueOf(searchHit.getSource().get("groupPrice").toString()));
                asProduct.setScore(Float.valueOf(searchHit.getScore()));
                asProduct.setSalePoint(searchHit.getSource().get("salePoint").toString());
                asProduct.setSupplierBackground(searchHit.getSource().get("supplierBackground").toString());
                asProduct.setCommentNum(Integer.valueOf(searchHit.getSource().get("commentNum").toString()));
                asProduct.setGoodCommentNum(Integer.valueOf(searchHit.getSource().get("goodCommentNum").toString()));

                List<String> strs = (List<String>) searchHit.getSource().get("labels");
                List<LabelTag> labels = strs.stream().map(i -> {
                    LabelTag tag = new LabelTag();
                    tag.setContent(i);
                    return tag;
                })
                        .collect(Collectors.toList());
                asProduct.setLabels(labels);

                List<String> activities = (List<String>) searchHit.getSource().get("activities");
                asProduct.setActivities(activities);

                asProduct.setWeekSaleNum(Integer.valueOf(searchHit.getSource().getOrDefault("weekSaleNum", "0").toString()));

                asProducts.add(asProduct);
            }

            bsSearchResponse.setResult(asProducts);
            bsSearchResponse.setHitTotal((int) searchHits.getTotalHits());
            bsSearchResponse.setEsTookTime((int) searchResponse.getTookInMillis());
        }

        // 搜索matchRequest日志
        printMatchRequestLog(matchRequest);

        long end = System.currentTimeMillis();
        bsSearchResponse.setBsTookTime((int) (end - start));

        return new RPCResult<BSSearchResponse<ASProduct>>(bsSearchResponse);
    }

    /**
     * @param client
     * @param bsSearchRequest
     * @return
     * @description: 搜索词分词之后分词个别匹配搜索结果
     * @author: luozhuo
     * @date: 2017年4月7日 上午10:49:08
     */
    private SearchResponse getShouldMatchSearchResponse(TransportClient client,
                                                        BSSearchRequest bsSearchRequest) {
        String[] matchFields = ElasticSearchConsts.MATCH_FIELDS;
        //分词，产生各个多字段匹配搜索builder
        List<QueryBuilder> queryBuilders = analyzeAndBuildQueryBuilder(client, bsSearchRequest.getQuery(), matchFields);

        BoolQueryBuilder boolShouldQuery = QueryBuilders.boolQuery();
        for (QueryBuilder builder : queryBuilders) {
            boolShouldQuery = boolShouldQuery.should(builder);
        }

        //过滤不支持必要主站渠道的商品
        BoolQueryBuilder finalBoolQuery =  QueryBuilders.boolQuery();
        ESUtil.buildSupportChannel(finalBoolQuery ,null);

        QueryBuilder finalQuery = null;
        if (bsSearchRequest.getPlatform() != null && bsSearchRequest.getPlatform() == PlatformEnum.MINI) {
            // 小程序去掉高模商品
            finalQuery = finalBoolQuery.must(boolShouldQuery)
                    .mustNot(QueryBuilders.termQuery("modelType", 0));
        } else {
            finalQuery = finalBoolQuery.must(boolShouldQuery);
        }

        SearchResponse searchResponse = client.prepareSearch(ESUtil.getESIndexName()).setTypes(ES_PRODUCT_TYPE_NAME)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(bsSearchRequest.getExpectNum())
                .get();
        return searchResponse;
    }

    /**
     * @param client
     * @param bsSearchRequest
     * @param asQuerySegment  外部传进来，处理搜索词分词信息
     * @return
     * @description: 搜索词分词之后分词均匹配搜索结果
     * @author: luozhuo
     * @date: 2017年4月7日 上午10:43:12
     */
    private SearchResponse getMustMatchSearchResponse(TransportClient client, BSSearchRequest bsSearchRequest, ASQuerySegment asQuerySegment) {
        String[] matchFields = ElasticSearchConsts.MATCH_FIELDS;
        //分词，产生各个多字段匹配搜索builder
        BoolQueryBuilder queryBoolMust = QueryBuilders.boolQuery();

        List<QueryBuilder> queryBuilders = analyzeAndBuildQueryBuilder(client, bsSearchRequest.getQuery(), matchFields);
        BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
        for (QueryBuilder builder : queryBuilders) {
            boolMustQuery = boolMustQuery.must(builder);
        }
        queryBoolMust.should(boolMustQuery);

        //短语匹配精确搜索
        BoolQueryBuilder queryBoolShould = QueryBuilders.boolQuery();

        QueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery("_all", bsSearchRequest.getQuery()).boost(5.0f);
        queryBoolShould.should(matchPhraseQuery);

        String queryExtensionQuery = "", queryExtensionSegments = "", queryExtensionTypes = "";
        // 设置分词信息
        asQuerySegment.setQuery(bsSearchRequest.getQuery());
        asQuerySegment.setQuerySegments(getQuerySegments(client, bsSearchRequest.getQuery()));
        asQuerySegment.setQueryExtension(queryExtensionQuery);
        asQuerySegment.setQueryExtensionSegments(queryExtensionSegments);
        asQuerySegment.setQueryExtensionType(queryExtensionTypes);

        //过滤不支持必要主站渠道的商品
        BoolQueryBuilder finalBoolQuery =  QueryBuilders.boolQuery();
        ESUtil.buildSupportChannel(finalBoolQuery ,null);

        QueryBuilder finalQuery = null;
        if (bsSearchRequest.getPlatform() != null && bsSearchRequest.getPlatform() == PlatformEnum.MINI) {
            // 小程序去掉高模商品
            finalQuery = finalBoolQuery.must(queryBoolMust).should(queryBoolShould)
                    .mustNot(QueryBuilders.termQuery("modelType", 0));
        } else {
            finalQuery = finalBoolQuery.must(queryBoolMust).should(queryBoolShould);
        }

        SearchResponse searchResponse = client.prepareSearch(ESUtil.getESIndexName()).setTypes(ES_PRODUCT_TYPE_NAME)
                .setQuery(finalQuery)
                .setFetchSource(ElasticSearchConsts.FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(bsSearchRequest.getExpectNum())
                .get();
        return searchResponse;
    }


    /**
     * @param client
     * @param queryWords
     * @return
     * @description: 将搜索词分词之后建立每个分词的queryBuilder
     * @author: luozhuo
     * @date: 2017年4月7日 上午10:25:18
     */
    private List<QueryBuilder> analyzeAndBuildQueryBuilder(
            TransportClient client, String queryWords, String[] matchFields) {
        List<QueryBuilder> queryBuilders = new ArrayList<QueryBuilder>();

        AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(queryWords)
                .setIndex(ESUtil.getESIndexName()).setAnalyzer(SEARCH_ANALYZER_TYPE).get();
        List<AnalyzeToken> tokens = analyzeResponse.getTokens();
        for (AnalyzeToken token : tokens) {
            queryBuilders.add(QueryBuilders.multiMatchQuery(token.getTerm(), matchFields));
        }

        return queryBuilders;
    }

    /**
     * 获取搜索词分词
     *
     * @param client
     * @param query
     * @return
     */
    private String getQuerySegments(TransportClient client, String query) {
        String queryTerms = "";
        AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(query)
                .setIndex(ESUtil.getESIndexName()).setAnalyzer(SEARCH_ANALYZER_TYPE).get();
        List<AnalyzeToken> tokens = analyzeResponse.getTokens();
        queryTerms = commaJoiner.join(tokens.stream().map(t -> t.getTerm().trim()).collect(Collectors.toList()));
        return queryTerms;
    }

    private void printMatchRequestLog(MatchRequest matchRequest) {
        StringBuilder log = new StringBuilder();
        Query query = matchRequest.getQuery();
        BSSearchRequest bsSearchRequest = matchRequest.getBsSearchRequest();
        log.append("lt=searchbs_match_request\t");
        log.append("lv=1.0\t");
        // sid
        log.append("sid=" + bsSearchRequest.getSid() + "\t");
        // 实验号
        log.append("asexp=" + underLineJoiner.join(bsSearchRequest.getExpIds()) + "\t");
        // uuid
        log.append("uuid=" + bsSearchRequest.getUuid() + "\t");
        // 搜索词
        log.append("q=" + bsSearchRequest.getQuery() + "\t");
        String qrw = bsSearchRequest.getQuery().equals(query.getQuery()) ? "" : query.getQuery();
        // 搜索词改写
        log.append("qrw=" + qrw + "\t");
        // 搜索词分词
        log.append("qt=" + commaJoiner.join(query.getAllTerms()) + "\t");
        // 搜索词分词改写内容
        StringBuilder qtrw = new StringBuilder();
        for (String key : query.getRewriteTerms().keySet()) {
            qtrw.append(key + "(" + commaJoiner.join(query.getRewriteTerms().get(key)) + "),");
        }
        if (qtrw.length() > 0) {
            qtrw.setLength(qtrw.length() - 1);
        }
        log.append("qtrw=" + qtrw + "\t");
        // 产品词分词
        log.append("qpt=" + commaJoiner.join(query.getProductTerms()) + "\t");
        // 品牌词分词
        log.append("qbt=" + commaJoiner.join(query.getBrandTerms()) + "\t");
        // 属性词分词
        log.append("qat=" + commaJoiner.join(query.getAttributeTerms()) + "\t");
        // 功能词分词
        log.append("qft=" + commaJoiner.join(query.getFeatureTerms()) + "\t");
        // 其他词分词
        log.append("qot=" + commaJoiner.join(query.getOtherTerms()) + "\t");
        // 搜索词性别判断
        log.append("qsex=" + query.getSexLabel() + "\t");

        matchLogger.printDCLog(log.toString());
    }

    /**
     * BS Hi 接口
     */
    @Override
    public RPCResult<BSHiResponse> hi() {
        BSHiResponse bsHiResponse = new BSHiResponse();
        return new RPCResult<BSHiResponse>(bsHiResponse);
    }

    private boolean isTagsProduct(Query query) {
        return productTagsCache.isTagProduct(query.getQuery());
    }
}
