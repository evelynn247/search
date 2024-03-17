package com.biyao.search.bs.server.mysql.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:20
 * @description
 */
@Data
public class BrandWordPO {
    /**
     * 主键id
     **/
    private Long id;
    /**
     * 品牌词
     **/
    private String brandWord;
    /**
     * 改写词
     **/
    private String rewriteWord;
}
