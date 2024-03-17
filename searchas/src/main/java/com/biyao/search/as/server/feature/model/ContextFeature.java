package com.biyao.search.as.server.feature.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/18 14:09
 * @description
 */
@Data
public class ContextFeature {
    /**
     * 上下文特征
     */
    private ByBaseFeature features = new ByBaseFeature();
}
