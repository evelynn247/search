package com.biyao.search.as.server.bean;

import com.alibaba.fastjson.JSONObject;

public class ProductFeature extends JSONObject{
	private static final long serialVersionUID = -5631871740863036487L;

	public double getDsc() {
		return this.containsKey("dsc") ? this.getDoubleValue("dsc") : 0;
	}

	public double getDoubleValueByKey(String key, double defaultValue) {
		return this.containsKey(key) ? this.getDoubleValue(key) : defaultValue;
	}
	
}
