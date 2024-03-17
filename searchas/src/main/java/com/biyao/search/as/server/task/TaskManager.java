package com.biyao.search.as.server.task;

import com.biyao.search.as.server.cache.ProductCache;
import com.biyao.search.as.server.cache.redis.RefundRateThresholdRedisCache;
import com.biyao.search.as.server.cache.redis.UuidWhiteListCache;
import com.biyao.search.as.server.experiment.ASExperimentSpace;
import com.biyao.search.as.server.feature.cache.ProductFeatureCacheV2;
import com.biyao.search.as.server.feature.cache.QueryFeatureCache;
import com.biyao.search.as.server.feature.manager.FeatureExtractManager;
import com.biyao.search.as.server.feature.manager.LambdaMARTRankerManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/11/22
 **/
@Component
@Slf4j
@EnableScheduling
public class TaskManager {

    @Autowired
    LambdaMARTRankerManager lambdaMARTRankerManager;

    @Autowired
    FeatureExtractManager featureExtractManager;

    @Autowired
    ProductFeatureCacheV2 productFeatureCacheV2;

    @Autowired
    QueryFeatureCache queryFeatureCache;

    @Autowired
    ASExperimentSpace asExperimentSpace;

    @Resource
    UuidWhiteListCache uuidWhiteListCache;

    @Autowired
    ProductCache productCache;

    @Autowired
    private RefundRateThresholdRedisCache refundRateThresholdRedisCache;

    /**
     * 定时任务30分钟执行一次
     */
    @Scheduled(cron = "10 0/30 * * * ?")
    private void refreshModel() {
        try {
            lambdaMARTRankerManager.refresh();
            featureExtractManager.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新模型和排序特征文件失败:", e);
        }
    }

    /**
     * 定时任务每天凌晨5点执行
     */
    @Scheduled(cron = "30 0 5 * * ?")
    private void refreshProductAndQueryFeature() {
        try {
            productFeatureCacheV2.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新商品特征缓存失败:", e);
        }
        try {
            queryFeatureCache.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新Query特征缓存失败:", e);
        }
    }

    /**
     * 每10分钟刷新一次实验
     */
    @Scheduled(cron = "15 0/10 * * * ?")
    private void refreshExperiment() {
        try {
            asExperimentSpace.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新实验空间失败:", e);
        }
    }

    /**
     * 30分钟刷新一次
     */
    @Scheduled(cron = "20 0/30 * * * ?")
    public void refreshUuidWhiteList() {
        try {
            uuidWhiteListCache.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新uuid白名单失败:", e);
        }
    }

    /**
     * 每2分钟刷新一次
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    public void refreshProductCache() {
        try {
            productCache.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新商品信息失败:", e);
        }
    }

    /**
     * 退货退款率惩罚阈值更新定时任务，每2分钟刷新一次
     */
    @Scheduled(cron = "0 0/2 * * * ?")
    public void refreshRefundRateThreshold() {
        try {
            refundRateThresholdRedisCache.refresh();
        } catch (Exception e) {
            log.error("[严重异常][任务报告]定时更新退货退款率惩罚阈值失败:", e);
        }
    }
}
