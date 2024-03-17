package com.biyao.search.as.server.bean;

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

	public double getDoubleValueOfPid(Integer pid, String key, double defaultValue) {
		JSONObject json = this.getJSONObject(key);
		if (json == null || pid == null) {
			return defaultValue;
		}

		return json.getDoubleValue(pid.toString());
	}
}
