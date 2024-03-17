package com.biyao.search.as.server.feature.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.as.server.cache.redis.RedisUtil;
import com.biyao.search.as.server.common.consts.RedisKeyConsts;
import com.biyao.search.as.server.feature.model.QueryFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/18 10:52
 * @description query特征缓存
 */
@Component
@EnableScheduling
public class QueryFeatureCache {

    @Autowired
    RedisUtil redisUtil;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<String, QueryFeature> queryFeatureMap = new HashMap<>();

    public QueryFeature getQueryFeatureByQuery(String query) {
        return queryFeatureMap.getOrDefault(query, new QueryFeature());
    }

    @PostConstruct
    private void init(){
        logger.info("[操作日志]初始化Query特征缓存开始");
        refresh();
        logger.info("[操作日志]初始化Query特征缓存结束");
    }

    public void refresh() {
        logger.info("[操作日志]query特征缓存刷新开始");
        Map<String, QueryFeature> tempMap = new HashMap<>();
        try {
            Map<String, String> redisResult = redisUtil.hscan(RedisKeyConsts.SEARCH_QUERY_FEATURE);
            if (redisResult != null) {
                for (Map.Entry<String, String> item : redisResult.entrySet()) {
                    try {
                        if (item.getKey() == null || item.getValue() == null) {
                            continue;
                        }
                        QueryFeature queryFeature = new QueryFeature();
                        queryFeature.setQuery(item.getKey());
                        JSONObject jsonObject = JSON.parseObject(item.getValue());
                        for (Map.Entry<String, Object> jsonItem : jsonObject.entrySet()) {
                            queryFeature.getFeatures().put(jsonItem.getKey(), jsonItem.getValue().toString());
                        }
                        tempMap.put(queryFeature.getQuery(), queryFeature);
                    }catch (Exception e){
                        logger.error("[未知异常]Query特征处理失败: query={}, value={}", item.getKey(), item.getValue(), e.getMessage());
                    }
                }
            }
            if(tempMap.size()>0){
                queryFeatureMap = tempMap;
            }
        } catch (Exception e) {
            logger.error("[严重异常]刷新query特征缓存异常，key：{},异常信息：" ,RedisKeyConsts.SEARCH_QUERY_FEATURE, e.getMessage());
        }
        logger.info("[操作日志]query特征缓存刷新结束,刷新query词条数：" + queryFeatureMap.size());
    }
}
