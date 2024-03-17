package com.biyao.search.bs.server.match;

import com.biyao.search.bs.server.query.Query;
import com.biyao.search.bs.service.model.request.BSSearchRequest;

public class MatchRequest {

	// BS请求
	private BSSearchRequest bsSearchRequest;
	// Query
	private Query query;

	public BSSearchRequest getBsSearchRequest() {
		return bsSearchRequest;
	}

	public void setBsSearchRequest(BSSearchRequest bsSearchRequest) {
		this.bsSearchRequest = bsSearchRequest;
	}

	public Query getQuery() {
		return query;
	}

	public void setQuery(Query query) {
		this.query = query;
	}

	public String getStringFlag(String flagKey) {
		return this.bsSearchRequest.getStringFlag(flagKey);
	}
}
