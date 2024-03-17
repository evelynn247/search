package com.biyao.search.bs.server.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.biyao.search.bs.server.common.util.ESUtil;
import com.biyao.search.bs.server.experiment.BSExperimentSpace;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse.AnalyzeToken;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.common.enums.QueryAnalyzerEnum;
import com.biyao.search.common.model.FacetItem;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.springframework.util.CollectionUtils;

@Service
@Slf4j
public class CommonHelperService {
	@Autowired
	private RelativeQueryParser relativeQueryParser;
	@Autowired
	private BSExperimentSpace experimentSpace;
	
	public List<String> analyzeQuery(MatchRequest request, QueryAnalyzerEnum analyzer) {
		List<String> result = new ArrayList<>();

		//　特殊处理 20180622 有facet时，剔除修饰词，用产品词搜索
		boolean productWordExist = false;
		if (request.getFacets() != null && request.getFacets().size() > 0) {
			String prodcutWord = relativeQueryParser.getProductWord(request.getQuery());
			if (!Strings.isNullOrEmpty(prodcutWord)) {
				result.add(prodcutWord);
				productWordExist = true;
			}
		} 
		
		if (!productWordExist) { // 没有替换产品词
			TransportClient client = ESClientConfig.getESClient();
			AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(request.getQuery())
					.setIndex(ESUtil.getESIndexName()).setAnalyzer(analyzer.getCode()).get();
			
			for(AnalyzeToken token : analyzeResponse.getTokens()) {
				result.add(token.getTerm());
			}
		}
		
		return result;
	}
	
	public List<FacetItem> convertMap2Facets(Map<String, Set<Object>> attributeMap) {
		if (attributeMap == null || attributeMap.size() == 0) {
			return new ArrayList<>();
		}
		
		List<FacetItem> result = new ArrayList<>();
		for (String attriKey : attributeMap.keySet()) {
			FacetItem facet = new FacetItem();
			facet.setKey(attriKey);
			facet.setValues(attributeMap.get(attriKey).stream().map(i -> i.toString()).collect(Collectors.toList()));
			
			result.add(facet);
		}
		
		return result;
	}

	public List<FacetItem> generateFacets(Double minPrice, Double maxPrice,
			Map<String, Set<Object>> attributeMap) {
		List<FacetItem> result = new ArrayList<>();
		
		// 处理价格facet
		if (maxPrice != minPrice) {
			FacetItem priceItem = new FacetItem();
			int priceDiff = (int) (maxPrice - minPrice);
			int levelDiff = priceDiff % 3 == 0 ? priceDiff / 3 : priceDiff / 3 + 1;
			
			String level1 = String.valueOf(minPrice.intValue() + levelDiff * 0);
			String level2 = String.valueOf(minPrice.intValue() + levelDiff * 1);
			String level3 = String.valueOf(minPrice.intValue() + levelDiff * 2);
			String level4 = String.valueOf(minPrice.intValue() + levelDiff * 3);
			List<String> values = Lists.newArrayList(level1 + " - " + level2, level2 + " - " + level3, level3 + " - " + level4);
			
			priceItem.setKey(ElasticSearchConsts.PRICE_FACET_KEY);
			priceItem.setValues(values);
			result.add(priceItem);
		}
		
		result.addAll(convertMap2Facets(attributeMap));
		
		return result;
	}

}
