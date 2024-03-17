package com.biyao.search.as.server.feature.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/18 14:04
 * @description
 */
@Data
public class UserFeature {
    /**
     * uid
     */
    private Integer uid;

    /**
     * uuid
     */
    private String uuid;

    /**
     * user特征
     */
    private ByBaseFeature features = new ByBaseFeature();
}
