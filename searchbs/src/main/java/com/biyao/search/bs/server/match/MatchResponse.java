package com.biyao.search.bs.server.match;

import com.biyao.search.common.model.ASProduct;
import org.elasticsearch.action.search.SearchResponse;

import java.util.ArrayList;
import java.util.List;

public class MatchResponse {
	// 分级召回结果需要分级排序，所以用两层List
	private List<List<ASProduct>> multiAsProductList = new ArrayList<>();
	// 为外层计算hits和tookInMillis使用。分级召回，所以用List
	private List<SearchResponse> searchResponseList = new ArrayList<>();

	public List<List<ASProduct>> getMultiAsProductList() {
		return multiAsProductList;
	}

	public void setMultiAsProductList(List<List<ASProduct>> multiAsProductList) {
		this.multiAsProductList = multiAsProductList;
	}

	public List<SearchResponse> getSearchResponseList() {
		return searchResponseList;
	}

	public void setSearchResponseList(List<SearchResponse> searchResponseList) {
		this.searchResponseList = searchResponseList;
	}
}
