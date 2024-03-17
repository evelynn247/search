package com.biyao.search.bs.server.mysql.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:21
 * @description
 */
@Data
public class ProductWordPO {
    /**
     * 主键id
     **/
    private Long id;
    /**
     * 产品词
     **/
    private String productWord;
    /**
     * 相关词
     **/
    private String relatedWord;
}
