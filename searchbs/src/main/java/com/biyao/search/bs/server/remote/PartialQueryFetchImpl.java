package com.biyao.search.bs.server.remote;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.bs.server.cache.guava.detail.ProductTagsCache;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.service.PartialQueryFetch;
import com.biyao.search.common.model.RPCResult;
@Service("partQueryFetch")
public class PartialQueryFetchImpl implements PartialQueryFetch{
	 @Autowired
	 private ProductTagsCache productTagsCache;
	
	@Autowired
	private RelativeQueryParser queryParser;

	@Override
	public RPCResult<List<String>> analyze(String query) {
		List<String> partQuerys = new ArrayList<String>();
		if (!productTagsCache.isTagProduct(query)) { // 白名单就不用分词了
			partQuerys = queryParser.parse(query);
		}

		return new RPCResult<List<String>>(partQuerys);
	}

	@Override
	public RPCResult<Boolean> isTagQuery(String query) {
		return new RPCResult<Boolean>(productTagsCache.isTagProduct(query));
	}

}
