package com.biyao.search.as.server.feature.model;

import com.biyao.search.as.server.feature.model.ByBaseFeature;
import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/15 17:55
 * @description
 */
@Data
public class QueryFeature {

    /**
     * query词
     */
    private String query;

    /**
     * query词特征
     */
    private ByBaseFeature features = new ByBaseFeature();

}
