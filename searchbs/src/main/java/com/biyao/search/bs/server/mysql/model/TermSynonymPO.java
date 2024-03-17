package com.biyao.search.bs.server.mysql.model;

import lombok.Data;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:21
 * @description
 */
@Data
public class TermSynonymPO {
    /**
     * 主键id
     **/
    private Long id;
    /**
     * term词
     **/
    private String term;
    /**
     * 同义词
     **/
    private String synonym;
}
