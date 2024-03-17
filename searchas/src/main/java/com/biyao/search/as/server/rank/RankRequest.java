package com.biyao.search.as.server.rank;

import java.util.List;

import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.common.model.ASProduct;

public class RankRequest {
	/**
	 * 待排序商品列表
	 */
	private List<ASProduct> products;
	
	private ASSearchRequest baseRequest;

	public List<ASProduct> getProducts() {
		return products;
	}

	public void setProducts(List<ASProduct> products) {
		this.products = products;
	}

	public ASSearchRequest getBaseRequest() {
		return baseRequest;
	}

	public void setBaseRequest(ASSearchRequest baseRequest) {
		this.baseRequest = baseRequest;
	}

}
