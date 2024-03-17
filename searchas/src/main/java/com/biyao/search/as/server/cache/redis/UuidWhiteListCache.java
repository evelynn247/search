package com.biyao.search.as.server.cache.redis;

import com.biyao.search.as.server.common.consts.RedisKeyConsts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

/**
 * author: xiafang
 * date: 2019/12/6
 *
 * @author xiafang
 */
@Slf4j
@Component
@EnableScheduling
public class UuidWhiteListCache {

    @Resource
    RedisUtil redisUtil;

    /**
     * 白名单uuid
     */
    private Set<String> uuidWhiteListSet = new HashSet<>();

    @PostConstruct
    protected void init() {
        log.info("[操作日志]redis白名单缓存初始化开始");
        refresh();
        log.info("[操作日志]redis白名单缓存初始化结束");
    }

    public void refresh() {
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
                uuidWhiteListSet = uuidWhiteListSetTemp;
                log.info("[操作日志]redis白名单缓存刷新结束, 白名单：{}", uuidWhiteListSet);
            }
        } catch (Exception e) {
            log.error("[严重异常][redis异常]redis白名单缓存刷新异常，key:{}", RedisKeyConsts.SEARCH_UUID_WHITE_LIST, e);
        }
    }

    /**
     * 判断uuid是否在白名单中
     *
     * @param uuid
     * @return
     */
    public boolean isWhiteListUuid(String uuid) {
        if (uuidWhiteListSet.contains(uuid)) {
            return true;
        }
        return false;
    }
}
