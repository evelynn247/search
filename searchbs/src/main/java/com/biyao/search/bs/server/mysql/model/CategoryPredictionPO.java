package com.biyao.search.bs.server.mysql.model;

import lombok.Data;
import org.apache.commons.net.ntp.TimeStamp;

/**
 * @Description: 用于存储搜索词和对应的相关类目
 * @Date: 2020/2/24
 * @Author: xiafang
 */
@Data
public class CategoryPredictionPO {
    /**
     * query id
     **/
    private Long id;
    /**
     * 搜索词
     **/
    private String query;
    /**
     * query的相关类目及权重，格式为类目:权重
     **/
    private String categories;
    /**
     * 0：禁用，1：可用
     */
    private boolean status;
    /**
     *  创建时间
     */
    private TimeStamp createTime;
    /**
     *  更新时间
     */
    private TimeStamp updateTime;
    /**
     *  创建人
     */
    private String createBy;
    /**
     *  修改人
     */
    private String updateBy;
}
