package com.biyao.search.bs.server.mysql.model;

import lombok.Data;

import java.util.Date;

/**
 * @desc: 数据库表search_term字段对应持久化对象
 * @author: xiafang
 * @date: 2020/1/13
 */
@Data
public class SearchTermPO {
    //数据库表字段对应持久化对象属性
    /**
     * termId
     */
    private Long termId;
    /**
     * term
     */
    private String term;
    /**
     * 同义词
     */
    private String synonyms;
    /**
     * 上位词，例子：菜刀的上位词厨具
     */
    private String hypernyms;
    /**
     * 下位词，例子：菜刀的下位词张小泉菜刀
     */
    private String hyponyms;
    /**
     * 0:是商品词，1：不是商品词
     */
    private Boolean productTag;
    /**
     * 0:是品牌词，1：不是品牌词
     */
    private Boolean brandTag;
    /**
     * 0:是性别词，1：不是性别词
     */
    private Boolean sexTag;
    /**
     * 0:是季节词，1：不是季节词
     */
    private Boolean seasonTag;
    /**
     * 0:是功能词，1：不是功能词
     */
    private Boolean functionTag;
    /**
     * 0:是属性词，1：不是属性词
     */
    private Boolean attributeTag;
    /**
     * 0：可以和性别词组合，1：不可以
     */
    private Boolean combineSex;
    /**
     * 0：可以和季节词组合，1：不可以
     */
    private Boolean combineSeason;
    /**
     * 0：禁用，1：可用
     */
    private Boolean status;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 修改人
     */
    private String updateBy;
}
