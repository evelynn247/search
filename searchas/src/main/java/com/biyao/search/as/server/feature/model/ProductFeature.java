package com.biyao.search.as.server.feature.model;

import com.biyao.search.as.server.feature.model.ByBaseFeature;
import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/15 17:39
 * @description
 */
@Data
public class ProductFeature {

    /**
     * 商品Id
     */
    private Long productId;

    /**
     * 商品特征
     */
    private ByBaseFeature features = new ByBaseFeature();
}
