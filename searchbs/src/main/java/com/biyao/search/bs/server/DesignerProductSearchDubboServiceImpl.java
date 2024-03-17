package com.biyao.search.bs.server;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.stereotype.Service;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.service.BSDesignerProductSearchService;
import com.biyao.search.bs.service.model.request.BSSearchRequest;
import com.biyao.search.bs.service.model.response.BSSearchResponse;
import com.biyao.search.common.model.ASDesignerProduct;
import com.biyao.search.common.model.RPCResult;

/**
 * @description: 搜索dubbo服务实现
 * @author: luozhuo
 * @version: V1.0.0
 */
@Service("bsDesignerProductSearchService")
public class DesignerProductSearchDubboServiceImpl implements BSDesignerProductSearchService{
	//es索引名称
	private static final String ES_PRODUCT_INDEX_NAME = "biyaomall";
	//es商品类型名称
	private static final String ES_DESIGNER_PRODUCT_TYPE_NAME = "designer_product";
	//搜索时分词器
	private static final String SEARCH_ANALYZER_TYPE = "ik_smart";

	public RPCResult<BSSearchResponse<ASDesignerProduct>> search(BSSearchRequest bsSearchRequest) {
		long start = System.currentTimeMillis();
		
		BSSearchResponse<ASDesignerProduct> bSSearchResponse = new BSSearchResponse<ASDesignerProduct>();
		TransportClient client = ESClientConfig.getESClient();
		SearchResponse searchResponse = null;
		
		/**
		 * 一级策略	分词均匹配，完全匹配时排名靠前
		 */
		SearchResponse mustMatchResponse = getMustMatchSearchResponse(client, bsSearchRequest);
		searchResponse = mustMatchResponse;
		SearchHits mustMatchHits = mustMatchResponse.getHits();
		/**
		 * 二级策略	分词个别匹配
		 */
		if(mustMatchHits.getTotalHits() == 0) {
			SearchResponse shouldMatchResponse = getShouldMatchSearchResponse(client, bsSearchRequest);
			searchResponse = shouldMatchResponse;
		}
		
		/**
		 * 组装返回结果
		 */
		List<ASDesignerProduct> asProducts = new ArrayList<ASDesignerProduct>();
		SearchHits searchHits = searchResponse.getHits();
		for(SearchHit searchHit : searchHits) {
		    ASDesignerProduct asProduct = new ASDesignerProduct();
			asProduct.setSuId(searchHit.getSource().get("suId_dp").toString());
			asProduct.setDesignId(searchHit.getSource().get("designId_dp").toString());
			asProduct.setPrice(Float.valueOf(searchHit.getSource().get("price_dp").toString()));
			asProduct.setImage(searchHit.getSource().get("image_dp").toString());
			asProduct.setName(searchHit.getSource().get("designName_dp").toString());
			asProducts.add(asProduct);
		}
		bSSearchResponse.setResult(asProducts);
		bSSearchResponse.setHitTotal((int) searchHits.getTotalHits());
		bSSearchResponse.setEsTookTime((int) searchResponse.getTookInMillis());
		
		long end = System.currentTimeMillis();
		//BSSearchResponse.setBsTookTime((int) (end - start));
		
		return new RPCResult<BSSearchResponse<ASDesignerProduct>>(bSSearchResponse);
	}
	
	/**
	 * @description: 搜索词分词之后分词个别匹配搜索结果
	 * @param client
	 * @param asSearchRequest
	 * @return
	 * @author: luozhuo
	 * @date: 2017年4月7日 上午10:49:08
	 */
	private SearchResponse getShouldMatchSearchResponse(TransportClient client,
			BSSearchRequest asSearchRequest) {
		//分词，产生各个多字段匹配搜索builder
		List<QueryBuilder> queryBuilders = analyzeAndBuildQueryBuilder(client, asSearchRequest.getQuery());
		
		BoolQueryBuilder boolShouldQuery = QueryBuilders.boolQuery();
		for(QueryBuilder builder : queryBuilders) {
			boolShouldQuery = boolShouldQuery.should(builder);
		}

		//int queryStart = (asSearchRequest.getPageIndex() - 1) * asSearchRequest.getPageSize();
	
		SearchResponse searchResponse = client.prepareSearch(ES_PRODUCT_INDEX_NAME).setTypes(ES_DESIGNER_PRODUCT_TYPE_NAME)
				.setQuery(QueryBuilders.boolQuery().must(boolShouldQuery))
				.setFetchSource(new String[]{"designName_dp", "suId_dp", "image_dp", "price_dp", "designId_dp"}, null)
				.setFrom(0)
				.setSize(asSearchRequest.getExpectNum())
				.get();
		return searchResponse;
	}

	/**
	 * @description: 搜索词分词之后分词均匹配搜索结果
	 * @param client
	 * @param asSearchRequest
	 * @return
	 * @author: luozhuo
	 * @date: 2017年4月7日 上午10:43:12
	 */
	private SearchResponse getMustMatchSearchResponse(TransportClient client, BSSearchRequest asSearchRequest) {
		//分词，产生各个多字段匹配搜索builder
		List<QueryBuilder> queryBuilders = analyzeAndBuildQueryBuilder(client, asSearchRequest.getQuery());
		
		BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
		for(QueryBuilder builder : queryBuilders) {
			boolMustQuery = boolMustQuery.must(builder);
		}
		//短语匹配精确搜索
		QueryBuilder matchPhraseQuery = QueryBuilders.matchPhraseQuery("_all", asSearchRequest.getQuery()).boost(5.0f);

		//int queryStart = (asSearchRequest.getPageIndex() - 1) * asSearchRequest.getPageSize();
		//利用boolQuery将两种搜索结果取并集，被精确匹配到的排名会更靠前
		SearchResponse searchResponse = client.prepareSearch(ES_PRODUCT_INDEX_NAME).setTypes(ES_DESIGNER_PRODUCT_TYPE_NAME)
				.setQuery(QueryBuilders.boolQuery().must(boolMustQuery).should(matchPhraseQuery))
				.setFetchSource(new String[]{"designName_dp", "suId_dp", "image_dp", "price_dp", "designId_dp"}, null)
				.setFrom(0)
				.setSize(asSearchRequest.getExpectNum())
				.get();
		return searchResponse;
	}

	/**
	 * @description: 将搜索词分词之后建立每个分词的queryBuilder
	 * @param client
	 * @param query
	 * @return
	 * @author: luozhuo
	 * @date: 2017年4月7日 上午10:25:18
	 */
	private List<QueryBuilder> analyzeAndBuildQueryBuilder(
			TransportClient client, String queryWords) {
		List<QueryBuilder> queryBuilders = new ArrayList<QueryBuilder>();

		AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(queryWords)
				.setIndex(ES_PRODUCT_INDEX_NAME).setAnalyzer(SEARCH_ANALYZER_TYPE).get();
		List<AnalyzeToken> tokens = analyzeResponse.getTokens();
		for(AnalyzeToken token : tokens) {
			queryBuilders.add(QueryBuilders.multiMatchQuery(token.getTerm(), "designerName_dp","designName_dp", "title_dp", "shortTitle_dp", 
				"salePoint_dp", "fsalePoint_dp", "fcategory1Names_dp", "fcategory2Names_dp", "fcategory3Names_dp", 
				"category1Name_dp", "category2Name_dp", "category3Name_dp"));
		}
		
		return queryBuilders;
	}
}
