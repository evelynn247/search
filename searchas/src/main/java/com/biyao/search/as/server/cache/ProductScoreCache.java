package com.biyao.search.as.server.cache;

import com.biyao.search.as.server.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/19 17:38
 * @description
 */
@Slf4j
@Component
@EnableScheduling
public class ProductScoreCache {

    /**
     * query预测类目文件地址
     */
    @Value("${product.score.url}")
    private String PRODUCT_SCORE_URL;

    /**
     * query和关联类目之间的分隔符
     */
    private static final String SPLITERATOR = ":";
    /**
     * 参数列表
     */
    private Map<Long,Double> productScoreMap = new HashMap<>();

    @PostConstruct
    protected void init() {
        log.info("刷新商品热度分缓存开始...");
        refresh();
        log.info("刷新商品热度分缓存结束!");
    }

    @Scheduled(cron = "0 0/10 * * * ?")
    public void refresh() {
        //从hadoop对应地址中读取数据
        //必要校验之后，存入缓存
        Map<Long, Double> tempMap = new HashMap<>();
        try {
            //读取文件
            List<String> lines = FileUtil.getRemoteFile(PRODUCT_SCORE_URL);
            if (lines == null) {
                log.error("[一般异常]商品热度分远程文件加载为null,异常数据地址：{}", PRODUCT_SCORE_URL);
                return;
            }
            for (String line : lines) {
                try{
                    if (line.startsWith("#")) {
                        continue;
                    }
                    String[] arrayTemp = line.split(SPLITERATOR);
                    if (arrayTemp.length != 2) {
                        log.error("[严重异常][数据格式异常]商品热度分数据格式异常,异常数据文件地址：{},异常数据：{}", PRODUCT_SCORE_URL, line);
                        continue;
                    }
                    String key = arrayTemp[0].trim();
                    String value = arrayTemp[1].trim();
                    if (StringUtils.isEmpty(key) || StringUtils.isEmpty(value)) {
                        continue;
                    }
                    tempMap.put(Long.parseLong(key),Double.parseDouble(value));
                }
                catch(Exception e){
                    log.error("[严重异常][数据格式异常]商品热度分数据格式异常,异常数据文件地址：{},异常数据：{}", PRODUCT_SCORE_URL, line);
                }
            }
            log.info("[操作日志]刷新商品热度分数据缓存结束, 数据量：{}", tempMap.size());
            if(tempMap.size() > 0){
                productScoreMap = tempMap;
            }
        } catch (Exception e) {
            log.error("[严重异常][数据解析异常]商品热度分缓存异常,异常数据地址：{}，异常信息：", PRODUCT_SCORE_URL, e);
        }
    }

    /**
     * 根据商品pid获取商品热度分
     * 没有返回0
     */
    public Double getProductScore(Long pid){

        return productScoreMap.getOrDefault(pid,0.00d);
    }
}
