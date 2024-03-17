package com.biyao.search.bs.server.match;


import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.ES_PRODUCT_TYPE_NAME;
import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.FETCH_SOURCE;
import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.MODEL_TYPE;
import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.PID_FIELD;
import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.SEX_LABEL_FIELD;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.biyao.search.bs.server.common.util.ESUtil;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.SexLabelConsts;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.LabelTag;

public abstract class Match {

	/**
	 * 召回方法
	 * @param matchRequest
	 * @return
	 */
	public abstract MatchResponse match(MatchRequest matchRequest);

	/**
	 * 根据ES的匹配结果构造MatchResponse
	 * @param searchResponseList
	 * @param matchRequest
	 * @return
	 */
	protected MatchResponse buildMatchResponse(List<SearchResponse> searchResponseList, MatchRequest matchRequest){
		MatchResponse result = new MatchResponse();
		List<List<ASProduct>> multiAsProductList = new ArrayList<>();
		Set<String> suidSet = new HashSet<>();
		for (SearchResponse searchResponse : searchResponseList){
			List<ASProduct> asProductList = new ArrayList<>();
			if (searchResponse.getHits().getTotalHits() > 0){
				for (SearchHit searchHit : searchResponse.getHits()){
					String suid = searchHit.getSource().get("suId").toString();
					if (suidSet.contains(suid)){
						continue;
					}
					suidSet.add(suid);
					ASProduct asProduct = new ASProduct();
					asProduct.setSuId(searchHit.getSource().get("suId").toString());
					asProduct.setShortTitle(searchHit.getSource().get("shortTitle").toString());
					asProduct.setFullTitle(searchHit.getSource().get("title").toString());
					asProduct.setPrice(Float.valueOf(searchHit.getSource().get("price").toString()));
					asProduct.setImage(searchHit.getSource().get("image").toString());
					asProduct.setSaleMode(1);
					asProduct.setGroupPrice(Float.valueOf(searchHit.getSource().get("groupPrice").toString()) );
					asProduct.setScore(Float.valueOf(searchHit.getScore()));
					asProduct.setSalePoint(searchHit.getSource().get("salePoint").toString());
					asProduct.setSupplierBackground(searchHit.getSource().get("supplierBackground").toString());
					asProduct.setCommentNum(Integer.valueOf(searchHit.getSource().get("commentNum").toString()));
					asProduct.setGoodCommentNum(Integer.valueOf(searchHit.getSource().get("goodCommentNum").toString()));

					List<String> labelStr = (List<String>) searchHit.getSource().get("labels");
					List<LabelTag> labels = labelStr.stream().map(i -> {LabelTag tag = new LabelTag(); tag.setContent(i); return tag;})
							.collect(Collectors.toList());
					asProduct.setLabels(labels);
					// 增加商品参与活动信息
					List<String> activities = (List<String>) searchHit.getSource().get("activities");
					asProduct.setActivities(activities);
					
					asProduct.setWeekSaleNum(Integer.valueOf(searchHit.getSource().getOrDefault("weekSaleNum", "0").toString()));
					
					asProductList.add(asProduct);
				}
			}
			if (asProductList.size() > 0){
				multiAsProductList.add(asProductList);
			}
		}

		result.setMultiAsProductList(multiAsProductList);
		result.setSearchResponseList(searchResponseList);

		return result;
	}

	/**
	 * 根据BoolQueryBuilder召回商品
	 * @param queryBuilder
	 * @param matchRequest
	 * @return
	 */
	protected SearchResponse recall(QueryBuilder queryBuilder, MatchRequest matchRequest){
		BoolQueryBuilder finalBoolQuery = QueryBuilders.boolQuery().must(queryBuilder);
		// 小程序去掉高模商品
		if (matchRequest.getBsSearchRequest().getPlatform() != null && matchRequest.getBsSearchRequest().getPlatform() == PlatformEnum.MINI) {
			finalBoolQuery.mustNot(QueryBuilders.termQuery(MODEL_TYPE, 0));
		}
		// 性别处理
		if (matchRequest.getQuery().getSexLabel() == SexLabelConsts.FEMALE){
			finalBoolQuery.mustNot(QueryBuilders.termQuery(SEX_LABEL_FIELD, SexLabelConsts.MALE));
		}else if (matchRequest.getQuery().getSexLabel() == SexLabelConsts.MALE){
			finalBoolQuery.mustNot(QueryBuilders.termQuery(SEX_LABEL_FIELD, SexLabelConsts.FEMALE));
		}

		//int queryStart = (matchRequest.getAsSearchRequest().getPageIndex() - 1) * matchRequest.getAsSearchRequest().getPageSize();

		TransportClient client = ESClientConfig.getESClient();
		SearchResponse searchResponse = client.prepareSearch(ESUtil.getESIndexName()).setTypes(ES_PRODUCT_TYPE_NAME)
				.setQuery(finalBoolQuery)
				.setFetchSource(FETCH_SOURCE, null)
				.setFrom(0)
				.setSize(matchRequest.getBsSearchRequest().getExpectNum())
				.get();
		return searchResponse;
	}

	/**
	 * 从ES的response中获取productId列表
	 * @param searchResponse
	 * @return
	 */
	protected List<String> getPidListFromESResponse(SearchResponse searchResponse){
		List<String> suidList = new ArrayList<>();
		if (searchResponse.getHits().getTotalHits() > 0){
//			for (SearchHit searchHit : searchResponse.getHits()){
//				suidList.add(searchHit.getSource().get(PID_FIELD).toString());
//			}
			searchResponse.getHits().forEach(t -> {suidList.add(t.getSource().get(PID_FIELD).toString());});
		}
		return suidList;
	}
}
