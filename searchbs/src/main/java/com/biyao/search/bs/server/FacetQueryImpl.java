package com.biyao.search.bs.server;

import com.biyao.search.bs.server.cache.guava.detail.ProductWordFacetCache;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.service.FacetQuery;
import com.biyao.search.common.model.FacetItem;
import com.biyao.search.common.model.RPCResult;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("facetQuery")
public class FacetQueryImpl implements FacetQuery{

	@Autowired
	private ProductWordFacetCache productWordFacetCache;
	@Autowired
	private RelativeQueryParser relativeQueryParser;

	@Override
	public RPCResult<List<FacetItem>> match(String query) {
		List<FacetItem> facetItemList = new ArrayList<>();
		String productWord = relativeQueryParser.getProductWord(query);
		if (StringUtils.isNotBlank(productWord)){
			facetItemList = productWordFacetCache.getFacet(productWord);
		}
		return new RPCResult<List<FacetItem>>(facetItemList);
	}
}
