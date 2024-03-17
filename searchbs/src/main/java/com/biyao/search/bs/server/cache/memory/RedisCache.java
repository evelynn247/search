package com.biyao.search.bs.server.cache.memory;

import com.biyao.search.bs.server.cache.redis.RedisUtil;
import com.biyao.search.bs.server.common.consts.RedisKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashSet;
import java.util.Set;

/**
 * @author zj
 * @version 1.0
 * @date 2020/9/8 14:21
 * @description
 */
@Component
@Slf4j
public class RedisCache {

    @Autowired
    private RedisUtil redisUtil;

    /**
     * 搜索召回日志白名单
     */
    private Set<String> searchDetailLogList = new HashSet<>();

    @PostConstruct
    public void init(){
        log.info("[任务报告]系统初始化同步Redis数据到本地缓存--》start");
        refresh();
        log.info("[任务报告]系统初始化同步Redis数据到本地缓存--》end");
    }

    public void refresh(){
        refreshSearchDetailLogList();
    }

    /**
     * 获取搜索召回日志白名单（uuid）
     * @return
     */
    public void refreshSearchDetailLogList() {
        try {
            log.info("[操作日志]redis白名单缓存刷新开始");
            Set<String> uuidWhiteListSetTemp = new HashSet<>();
            String uuidStr = redisUtil.getString(RedisKeyConsts.SEARCH_UUID_WHITE_LIST);
            if (StringUtils.isNotBlank(uuidStr)) {
                String[] uuidArray = uuidStr.split(",");
                for (String uuid : uuidArray) {
                    if (StringUtils.isNotBlank(uuid)) {
                        uuidWhiteListSetTemp.add(uuid);
                    }
                }
            }
            if(uuidWhiteListSetTemp.size() > 0){
                searchDetailLogList = uuidWhiteListSetTemp;
                log.info("[操作日志]redis白名单缓存刷新结束, 白名单：{}", searchDetailLogList);
            }
        } catch (Exception e) {
            log.error("[严重异常][redis异常]redis白名单缓存刷新，异常key:{}，异常信息:", RedisKeyConsts.SEARCH_UUID_WHITE_LIST, e);
        }
    }


    /**
     * 验证uuid是否为搜索流程日志白名单用户
     * @param uuid
     * @return
     */
    public boolean isSearchDetailLogUuid(String uuid){
        if(StringUtils.isBlank(uuid)){
            return false;
        }
        return searchDetailLogList.contains(uuid);
    }
}
