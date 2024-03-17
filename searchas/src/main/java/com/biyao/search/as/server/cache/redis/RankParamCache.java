package com.biyao.search.as.server.cache.redis;

import com.biyao.search.as.server.common.consts.RedisKeyConsts;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/19 14:49
 * @description 新模型算法参数缓存
 */
@Slf4j
@Component
@EnableScheduling
public class RankParamCache {

    @Resource
    RedisUtil redisUtil;

    /**
     * 参数列表
     */
    @Getter
    private List<Double> paramList = new ArrayList<>();

    @PostConstruct
    protected void init() {
        log.info("[操作日志]新模型算法参数缓存初始化开始");
        refresh();
        log.info("[操作日志]新模型算法参数缓存初始化结束");
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void refresh() {
        List<Double> temp = new ArrayList<>();
        if(paramList.size() == 0){
            paramList.add(1d);
            paramList.add(1d);
            paramList.add(1d);
        }
        try {
            log.info("[操作日志]新模型算法参数缓存刷新开始");
            String uuidStr = redisUtil.getString(RedisKeyConsts.SEARCH_RANKPARAM);
            if (StringUtils.isNotBlank(uuidStr)) {
                String[] array = uuidStr.split(",");
                for (String s : array) {
                    temp.add(Double.valueOf(s));
                }
            }
            if(temp.size() >= 3){
                paramList = temp;
                log.info("[操作日志]新模型算法参数缓存刷新结束, params：{}", paramList);
            }
        } catch (Exception e) {
            log.error("[严重异常][redis异常]新模型算法参数缓存刷新异常，key:{}", RedisKeyConsts.SEARCH_RANKPARAM, e);
        }
    }
}
