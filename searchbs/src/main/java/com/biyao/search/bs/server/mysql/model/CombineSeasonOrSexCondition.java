package com.biyao.search.bs.server.mysql.model;

import lombok.Data;

/**
 * @desc: 和性别季节结合的term查询条件对象
 * @author: xiafang
 * @date: 2020/1/16
 */
@Data
public class CombineSeasonOrSexCondition {
    /**
     * term_id起始偏移量
     */
    private long termId = 0L;
    /**
     * 查询条件,对应词库表search_term中combine_season字段，通过combineSeason传递查询条件
     */
    private Boolean combineSeason;
    /**
     * 查询条件,对应词库表search_term中combine_sex字段，通过combineSex传递查询条件
     */
    private Boolean combineSex;
}
