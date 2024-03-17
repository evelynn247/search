package com.biyao.search.bs.server.remote;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.biyao.search.bs.server.common.util.ESUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.server.service.CommonHelperService;
import com.biyao.search.bs.service.TagProductMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.SearchItem;
import com.google.common.collect.Lists;
@Service("tagProductMatch")
public class TagProductMatchImpl implements TagProductMatch{
	
	@Autowired
	CommonHelperService helperService;
	
	@Autowired
	private RelativeQueryParser relativeQueryParser;

	@Override
	public RPCResult<ProductMatchResult> match(MatchRequest request) {
		// TODO 日志记录
		
		/* 分词并建立must查询条件 */
		List<String> analyzeResult = Lists.newArrayList(request.getQuery()); // tag词不再进行分词了
		QueryBuilder finalQuery = buildFinalQueryBuilder(analyzeResult, request);
		
		SearchResponse searchResponse = ESClientConfig.getESClient().prepareSearch(ESUtil.getESIndexName())
				.setTypes(ElasticSearchConsts.ES_PRODUCT_TYPE_NAME)
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

			// 有传入facet则不再聚合facet
			if (request.getFacets() != null && request.getFacets().size() > 0) {
				continue;
			}
			
			// 收集并集合商品的属性信息
			Object attribute = hit.getSource().get(ElasticSearchConsts.ATTRIBUTE_FIELD);
			if (attribute != null) {
				Map<String, List<Object>> tmp = (Map<String, List<Object>>) attribute;
				for (String attriKey : tmp.keySet()) {
					if (tmp.get(attriKey).size() == 0) {
						continue;
					}
					
					// facet来源更改，此处不要了，只要价格
					/*if (!attributeMap.containsKey(attriKey)) {
						attributeMap.put(attriKey, Sets.newHashSet(tmp.get(attriKey)));
					} else {
						attributeMap.get(attriKey).addAll(tmp.get(attriKey));
					}*/
				}
			}
			// 收集商品价格信息
			Double price = Double.valueOf(hit.getSource().get(ElasticSearchConsts.PRICE_FIELD).toString());
			if (minPrice > price) { // 当前最小价格比商品价格大
				minPrice = price;
			} else if (maxPrice < price) { // 当前最大价格比商品价格小
				maxPrice = price;
			}
			
		}
		
		ProductMatchResult result = new ProductMatchResult();
		result.setQuery(request.getQuery());
		result.setItems(searchResult);
		result.setFacets(helperService.generateFacets(minPrice, maxPrice, attributeMap));
		
		return new RPCResult<ProductMatchResult>(result);
	}

	private QueryBuilder buildFinalQueryBuilder(List<String> terms,
			MatchRequest request) {
		BoolQueryBuilder boolMustQuery = QueryBuilders.boolQuery();
		
		// 分词结果多字段匹配
		for (String term : terms) {
			boolMustQuery.must(QueryBuilders.multiMatchQuery(term, ElasticSearchConsts.TAG_MATCH_FIELDS));
		}
		
		// facet条件匹配
		if (request.getFacets() != null && request.getFacets().size() > 0) {
			for (FacetItem facet : request.getFacets()) {
				// 价格特殊处理
				if (ElasticSearchConsts.PRICE_FACET_KEY.equals(facet.getKey())) {
					BoolQueryBuilder priceRangeQuery = QueryBuilders.boolQuery();
					for (String value : facet.getValues()) {
						String[] prices = value.split(" - ");
						Double from = Double.valueOf(prices[0]);
						Double to = Double.valueOf(prices[1]);
						priceRangeQuery.should(QueryBuilders.rangeQuery(ElasticSearchConsts.PRICE_FIELD).gte(from).lte(to));
					}
					
					boolMustQuery.must(priceRangeQuery);
				} else if (ElasticSearchConsts.ACTIVITY_FACET_KEY.equals(facet.getKey())) { // 活动筛选条件
					BoolQueryBuilder activityQuery = QueryBuilders.boolQuery();
					for (String value : facet.getValues()) {
						activityQuery.should(QueryBuilders.multiMatchQuery(value, ElasticSearchConsts.TAG_MATCH_FIELDS));
					}
					
					boolMustQuery.must(activityQuery);
				} else {
					/*boolMustQuery.must(QueryBuilders.nestedQuery(ElasticSearchConsts.ATTRIBUTE_FIELD, 
							QueryBuilders.termsQuery(ElasticSearchConsts.ATTRIBUTE_FIELD + "." + facet.getKey(),facet.getValues()), 
							ScoreMode.None));*/
					BoolQueryBuilder facetQuery = QueryBuilders.boolQuery();
					for (String value : facet.getValues()) {
						facetQuery.should(QueryBuilders.multiMatchQuery(value, ElasticSearchConsts.MATCH_FIELDS));
					}
					
					boolMustQuery.must(facetQuery);
				}
			}
		}

		// TODO
		System.out.println(boolMustQuery.toString());
		
		return boolMustQuery;
	}

}
