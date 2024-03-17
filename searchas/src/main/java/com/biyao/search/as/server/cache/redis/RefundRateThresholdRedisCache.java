package com.biyao.search.as.server.cache.redis;

import com.biyao.search.as.server.common.consts.RedisKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;


/**
 * @desc: 从redis更新退货退款率惩罚阈值
 * @author: xiafang
 * @date: 2020/8/10
 */
@Slf4j
@Component
@EnableScheduling
public class RefundRateThresholdRedisCache {
    @Autowired
    RedisUtil redisUtil;
    /**
     * 退货退款率惩罚阈值，默认值：0.2， 即商品退货退货退款率大于refundRateThreshold，模型排序分才重新计算
     */
    private  double refundRateThreshold = 0.2;

    @PostConstruct
    protected void init() {
        log.info("[操作日志]redis退货退款率惩罚阈值缓存初始化开始");
        refresh();
        log.info("[操作日志]redis退货退款率惩罚阈值缓存初始化结束");
    }

    public void refresh() {
        try {
            String refundRateThresholdTemp = redisUtil.getString(RedisKeyConsts.REFUND_RATE_THRESHOLD_KEY);
            if (StringUtils.isNotBlank(refundRateThresholdTemp) && Double.valueOf(refundRateThresholdTemp) >= 0 && Double.valueOf(refundRateThresholdTemp) <= 1) {
                refundRateThreshold = Double.valueOf(refundRateThresholdTemp);
            }
        } catch (Exception e) {
            log.error("[严重异常][redis异常]redis退货退款率惩罚阈值缓存刷新异常，key:{}", RedisKeyConsts.REFUND_RATE_THRESHOLD_KEY, e);
        }
    }
    public double getRefundRateThreshold() {
        return refundRateThreshold;
    }
}
