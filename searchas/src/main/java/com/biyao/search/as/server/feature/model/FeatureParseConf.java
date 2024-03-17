package com.biyao.search.as.server.feature.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/15 14:25
 * @description
 */
@Data
public class FeatureParseConf {

    /**
     * 特征名称
     */
    private String featureName;

    /**
     * 解析函数名
     */
    private String functionName;

    /**
     * 缺失默认值
     */
    private String defaultValue;

    /**
     * 解析用的参数A
     */
    private String paramA;

    /**
     * 解析用的参数B
     */
    private String paramB;


    @Override
    public String toString() {
        return "OnlineParseFeaConf [feaName=" + featureName + ", formula=" + functionName + ", defaultValue="
                + defaultValue + ", paramA=" + paramA + ", paramB=" + paramB
                + "]";
    }
}
