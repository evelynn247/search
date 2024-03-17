package com.biyao.search.bs.server.bean;

public class ASQuerySegment {

	private String query;
	// 分词之间英文逗号分隔
	private String querySegments;
	// 扩展词，多个扩展词之间 | 分隔
	private String queryExtension;
	// 扩展词类型，多个类型，则 | 分隔
	private String queryExtensionType;
	// 扩展词分词，每个扩展词的分词之间英文逗号分隔，扩展词与扩展词之间 | 分隔
	private String queryExtensionSegments;

	public String getQuerySegments() {
		return querySegments;
	}

	public void setQuerySegments(String querySegments) {
		this.querySegments = querySegments;
	}

	public String getQueryExtension() {
		return queryExtension;
	}

	public void setQueryExtension(String queryExtension) {
		this.queryExtension = queryExtension;
	}

	public String getQueryExtensionType() {
		return queryExtensionType;
	}

	public void setQueryExtensionType(String queryExtensionType) {
		this.queryExtensionType = queryExtensionType;
	}

	public String getQueryExtensionSegments() {
		return queryExtensionSegments;
	}

	public void setQueryExtensionSegments(String queryExtensionSegments) {
		this.queryExtensionSegments = queryExtensionSegments;
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}
}
