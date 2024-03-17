package com.biyao.search.bs.server.query.impl;

import com.biyao.search.bs.server.cache.guava.detail.QuerySegmentMarkCache;
import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.util.ESUtil;
import com.biyao.search.bs.server.query.Query;
import com.biyao.search.bs.server.query.QueryParser;

import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static com.biyao.search.bs.server.common.consts.ElasticSearchConsts.SEARCH_ANALYZER_TYPE;

@Component(value = "segmentMarkQueryParser")
public class SegmentMarkQueryParser implements QueryParser {

	@Autowired
	private QuerySegmentMarkCache querySegmentMarkCache;

	@Override
	public void parse(Query query) {
		TransportClient client = ESClientConfig.getESClient();
		AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(query.getQuery())
				.setIndex(ESUtil.getESIndexName()).setAnalyzer(SEARCH_ANALYZER_TYPE).get();
		List<AnalyzeResponse.AnalyzeToken> tokens = analyzeResponse.getTokens();
		List<String> terms = tokens.stream().map(t -> t.getTerm()).collect(Collectors.toList());

		for (String term : terms){
			if (querySegmentMarkCache.isProductTerm(term)){
				query.addProductTerm(term);
			}else if (querySegmentMarkCache.isBrandTerm(term)){
				query.addBrandTerm(term);
			}else if (querySegmentMarkCache.isAttributeTerm(term)){
				query.addAttributeTerm(term);
			}else if (querySegmentMarkCache.isFeatureTerm(term)){
				query.addFeatureTerm(term);
			}else {
				query.addOtherTerm(term);
			}
		}


	}
}
