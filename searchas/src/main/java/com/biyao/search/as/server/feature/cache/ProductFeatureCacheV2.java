package com.biyao.search.as.server.feature.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.as.server.cache.redis.RedisUtil;
import com.biyao.search.as.server.common.consts.RedisKeyConsts;
import com.biyao.search.as.server.feature.model.ProductFeature;
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
 * @description 商品特征缓存
 */
@Component
@EnableScheduling
public class ProductFeatureCacheV2 {

    @Autowired
    RedisUtil redisUtil;

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Map<Long, ProductFeature> productFeatureMap = new HashMap<>();

    public ProductFeature getProductFeatureById(Long productId) {
        return productFeatureMap.get(productId);
    }

    @PostConstruct
    private void init(){
        logger.info("[操作日志]初始化商品特征缓存开始");
        refresh();
        logger.info("[操作日志]初始化商品特征缓存结束");
    }

    public void refresh() {
        logger.info("[操作日志]商品特征缓存刷新开始");
        Map<Long, ProductFeature> tempMap = new HashMap<>();
        try {
            Map<String, String> redisResult = redisUtil.hscan(RedisKeyConsts.SEARCH_PRODUCT_FEATURE);
            if (redisResult != null) {
                for (Map.Entry<String, String> item : redisResult.entrySet()) {
                    if (item.getKey() == null || item.getValue() == null) {
                        continue;
                    }
                    ProductFeature productFeature = new ProductFeature();
                    productFeature.setProductId(Long.parseLong(item.getKey()));
                    JSONObject jsonObject = JSON.parseObject(item.getValue());
                    for (Map.Entry<String, Object> jsonItem : jsonObject.entrySet()) {
                        productFeature.getFeatures().put(jsonItem.getKey(), jsonItem.getValue().toString());
                    }
                    tempMap.put(productFeature.getProductId(), productFeature);
                }
            }
            if(tempMap.size()>0){
                productFeatureMap = tempMap;
            }
        } catch (Exception e) {
            logger.error("[严重异常][redis异常]刷新商品特征缓存异常，key：{},异常：", RedisKeyConsts.SEARCH_PRODUCT_FEATURE, e);
        }
        logger.info("[操作日志]商品特征缓存刷新结束,刷新商品条数：" + productFeatureMap.size());
    }

}
