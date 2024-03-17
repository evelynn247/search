package com.biyao.search.as.server.bean;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QueryGenderMarkRules {
	// 性别标识
	private Integer sexLabel;
	// 需要包含的词
	private String include;
	// 需要剔除的词
	private Set<String> excludes;

	public Integer getSexLabel() {
		return sexLabel;
	}

	public void setSexLabel(Integer sexLabel) {
		this.sexLabel = sexLabel;
	}

	public String getInclude() {
		return include;
	}

	public void setInclude(String include) {
		this.include = include;
	}

	public Set<String> getExcludes() {
		return excludes;
	}

	public void setExcludes(Set<String> excludes) {
		this.excludes = excludes;
	}

	public void setExcludes(String excludeStr){
		this.excludes = new HashSet<>();
		if (excludeStr != null && !"".equals(excludeStr)){
			String[] excludes = excludeStr.split(",");
			for (int i=0; i<excludes.length; i++) {
				this.excludes.add(excludes[i]);
			}
		}
	}
}
