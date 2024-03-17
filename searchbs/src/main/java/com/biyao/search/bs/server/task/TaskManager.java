package com.biyao.search.bs.server.task;

import com.biyao.search.bs.server.cache.memory.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @author zj
 * @version 1.0
 * @date 2019/12/31 10:10
 * @description
 */
@Component
@EnableScheduling
public class TaskManager {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    BrandWordCache brandWordCache;

    @Autowired
    DictionaryCache dictionaryCache;

    @Autowired
    SynonymsCache synonymsCache;

    @Autowired
    TermCombinedWithSexOrSeasonCache termCombinedWithSexOrSeasonCache;

    @Autowired
    RedisCache redisCache;

    @Autowired
    QueryProductCache queryProductCache;

    @Autowired
    ReletedWordCache reletedWordCache;
    /**
     * 每10分钟刷新（5秒延迟）
     */
    @Scheduled(cron = "5 0/10 * * * ? ")
    private void refreshTenMinutes(){

        try {
            redisCache.refresh();
        } catch (Exception e) {
            log.error("[严重异常]定时更新品牌词、同义词、性别&季节词库、类目预测失败:", e);
        }
    }

    /**
     * 每一小时刷新
     */
    @Scheduled(cron = "0 0 0/1 * * ? ")
    private void refreshOneHour(){

        try {
            brandWordCache.refresh();
            synonymsCache.refresh();
            queryProductCache.refresh();
            reletedWordCache.refresh();
            termCombinedWithSexOrSeasonCache.refresh();
            dictionaryCache.refresh();

        } catch (Exception e) {
            log.error("[严重异常]定时更新term字典失败", e);
        }
    }

}
