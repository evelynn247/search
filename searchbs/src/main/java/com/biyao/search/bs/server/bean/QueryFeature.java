package com.biyao.search.bs.server.bean;

import com.alibaba.fastjson.JSONObject;

public class QueryFeature extends JSONObject{
	private static final long serialVersionUID = -2375599695812703279L;
	
	public double getPidPosterScore(Integer pid) {
		JSONObject posterior = this.getJSONObject("posterior");
		if (posterior == null || pid == null) {
			return 0;
		}
		
		return posterior.getDoubleValue(pid.toString());
	}
}
