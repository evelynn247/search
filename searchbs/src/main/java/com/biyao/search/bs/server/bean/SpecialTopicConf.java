package com.biyao.search.bs.server.bean;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 专题配置
 *
 * @author wangbo
 * @version 1.0 2018/6/11
 */
public class SpecialTopicConf {

    /**
     * 自动生成
     */
    private List<SpecialTopic> autoCreateConf = new ArrayList<>();

    /**
     * 运营配置
     */
    private List<SpecialTopic> operatorConf = new ArrayList<>();

    private Map<Integer, SpecialTopic> confMap = new HashMap<>();
    
    private List<SpecialTopic> allConf = new ArrayList<>();

    public List<SpecialTopic> getAutoCreateConf() {
        return autoCreateConf;
    }

    public void setAutoCreateConf(List<SpecialTopic> autoCreateConf) {
        this.autoCreateConf = autoCreateConf;
    }

    public List<SpecialTopic> getOperatorConf() {
        return operatorConf;
    }

    public List<SpecialTopic> getAllConf() {
		return allConf;
	}

	public void setAllConf(List<SpecialTopic> allConf) {
		this.allConf = allConf;
	}

	public void setOperatorConf(List<SpecialTopic> operatorConf) {
        this.operatorConf = operatorConf;
    }

    public Map<Integer, SpecialTopic> getConfMap() {
        return confMap;
    }

    public void setConfMap(Map<Integer, SpecialTopic> confMap) {
        this.confMap = confMap;
    }
}
