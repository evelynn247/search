package com.biyao.search.bs.server.query;

import org.elasticsearch.client.transport.TransportClient;

public interface QueryParser {

	public void parse(Query query);
}
