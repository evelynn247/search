package com.biyao.search.bs.server.match.impl;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.util.ESUtil;
import com.biyao.search.bs.server.match.Match;
import com.biyao.search.bs.server.match.MatchRequest;
import com.biyao.search.bs.server.match.MatchResponse;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.LabelTag;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.*;

/**
 * 标签match
 *
 * @author wangbo
 * @version 1.0 2018/5/27
 */
@Component
public class TagsMushMatch extends Match {


    @Override
    public MatchResponse match(MatchRequest matchRequest) {
        BoolQueryBuilder boolMustQueryBuilder = QueryBuilders.boolQuery();

        QueryBuilder queryBoolMust = QueryBuilders.multiMatchQuery(matchRequest.getQuery().getQuery(), "alias");

        QueryBuilder finalQuery;

        //过滤不支持必要主站渠道的商品
        ESUtil.buildSupportChannel(boolMustQueryBuilder ,null);

        // 小程序去掉高模商品
        if (matchRequest.getBsSearchRequest().getPlatform() != null && matchRequest.getBsSearchRequest().getPlatform() == PlatformEnum.MINI) {
            finalQuery = boolMustQueryBuilder.must(queryBoolMust).mustNot(QueryBuilders.termQuery("modelType", 0));
        } else {
            finalQuery = boolMustQueryBuilder.must(queryBoolMust);
        }


        TransportClient client = ESClientConfig.getESClient();
        SearchResponse searchResponse = client.prepareSearch(ESUtil.getESIndexName()).setTypes(ES_PRODUCT_TYPE_NAME)
                .setQuery(finalQuery)
                .setFetchSource(FETCH_SOURCE, null)
                .setFrom(0)
                .setSize(matchRequest.getBsSearchRequest().getExpectNum())
                .get();

        return buildMatchResponse(searchResponse, matchRequest);
    }

    private MatchResponse buildMatchResponse(SearchResponse searchResponse, MatchRequest matchRequest) {
        MatchResponse result = new MatchResponse();
        List<List<ASProduct>> multiAsProductList = new ArrayList<>();
        Set<String> suidSet = new HashSet<>();

        List<ASProduct> asProductList = new ArrayList<>();
        if (searchResponse.getHits().getTotalHits() > 0) {
            for (SearchHit searchHit : searchResponse.getHits()) {
                String suid = searchHit.getSource().get("suId").toString();
                if (suidSet.contains(suid)) {
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
                asProduct.setGroupPrice(Float.valueOf(searchHit.getSource().get("groupPrice").toString()));
                asProduct.setScore(Float.valueOf(searchHit.getScore()));
                asProduct.setSalePoint(searchHit.getSource().get("salePoint").toString());
                asProduct.setSupplierBackground(searchHit.getSource().get("supplierBackground").toString());
                asProduct.setCommentNum(Integer.valueOf(searchHit.getSource().get("commentNum").toString()));
                asProduct.setGoodCommentNum(Integer.valueOf(searchHit.getSource().get("goodCommentNum").toString()));

                List<String> labelStr = (List<String>) searchHit.getSource().get("labels");
                List<LabelTag> labels = labelStr.stream().map(i -> {
                    LabelTag tag = new LabelTag();
                    tag.setContent(i);
                    return tag;
                })
                        .collect(Collectors.toList());
                asProduct.setLabels(labels);
                // 增加商品参与活动信息
                List<String> activities = (List<String>) searchHit.getSource().get("activities");
                asProduct.setActivities(activities);

                asProduct.setWeekSaleNum(Integer.valueOf(searchHit.getSource().getOrDefault("weekSaleNum", "0").toString()));

                asProductList.add(asProduct);
            }
        }
        if (asProductList.size() > 0) {
            multiAsProductList.add(asProductList);
        }

        result.setMultiAsProductList(multiAsProductList);
        result.getSearchResponseList().add(searchResponse);

        return result;
    }
}
