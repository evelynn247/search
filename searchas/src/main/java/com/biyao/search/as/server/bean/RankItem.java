package com.biyao.search.as.server.bean;


import lombok.Data;

/**
 * @desc: 记录各阶段排序分数打印日志
 * @author: xiafang
 * @date: 2020/9/21
 */
@Data
public class RankItem {
    /**
     * 商品id
     */
    private Integer productId;
    /**
     * 特征值
     */
    private String featuresUsedInModel;
    /**
     * ES召回分
     */
    private Double matchScore = 0.0D;
    /**
     * rankScoreBasedModel,综合模型排序分
     */
    private Double rankScoreBasedModel = 0.0;
    /**
     * scoreLikeNormalization, 类归一化分数，范围p[1,100]
     */
    private Double scoreLikeNormalization = 1.0;
    /**
     * categoryScore, query预测类目分数
     */
    private Float categoryScore = 1.0f;
    /**
     * rankBasedCategoryScore,基于类目提权计算后的排序分
     */
    private Double rankBasedCategoryScore = 1.0;
    /**
     * refundRate,退货退款率
     */
    private Double refundRate = 1.0;

    /**
     * rankScoreBasedRefundRate，基于退货退款率计算排序分
     */
    private Double rankScoreBasedRefundRate = 0.0D;
}
