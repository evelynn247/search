package com.biyao.search.as.server.bean;

import lombok.Builder;
import lombok.Data;

/**
 * @desc: 定义一个对象存储pdc同步的数据，当前只需同步pid,suid,价格上下架状态、以及是否支持定制
 * @author: xiafang
 * @date: 2020/6/5
 */
@Data
@Builder
public class ProductInfoFromPdc {
    /**
     * 商品id
     */
    private Long productId;
    /**
     * skuid
     */
    private Long suId;
    /**
     * 商品价格
     */
    private Long price;
    /**
     * 上下架状态
     */
    private Byte shelfStatus;
    /**
     * 商品退货退款率
     */
    private Double refundRate = 0.0;

    /**
     * 商品三级类目id
     */
    private Long thirdCategoryId;
    /*鸿源V2.4-交互视觉优化  新增前台三级类目 author：huangyq，modifyDate:2021/10/11*/
    /**
     * 前台三级类目ID集合，多个以英文逗号分隔
     */
    private String frontThirdCategoryIds;
    /**
     * 前台三级类目名称集合，多个以英文逗号分隔
     */
    private String frontThirdCategoryNames;
}
