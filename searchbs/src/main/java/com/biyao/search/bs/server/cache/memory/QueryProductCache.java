package com.biyao.search.bs.server.cache.memory;

import com.biyao.search.bs.server.common.util.FileUtil;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2020/9/2 15:43
 * @description
 */
@Component
public class QueryProductCache {

    @Value("${query.product.url}")
    private String QUERY_PRODUCT_URL;

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, String> queryProductMap = new HashMap<>();

    @PostConstruct
    public void init(){
        log.info("[操作日志]加载query产品词缓存开始...");
        refresh();
        log.info("[操作日志]加载query产品词缓存结束");
    }

    public void refresh(){
        Map<String, String> temp = new HashMap<>();
        try{
            //读取文件
            List<String> lines = FileUtil.getRemoteFile(QUERY_PRODUCT_URL);
            if(lines !=null){
                lines.forEach(line->{
                    if(!line.startsWith("#")){
                        String[] arrayTemp = line.split(":");
                        if(arrayTemp.length == 2){
                            String key = arrayTemp[0].trim();
                            String value = arrayTemp[1].trim();
                            if(StringUtils.isNotBlank(key) && StringUtils.isNotBlank(value)){
                                temp.put(key,value);
                            }
                        }
                    }
                });
                queryProductMap = temp;
                log.info("[操作日志]刷新query产品词缓存结束，共更新数据：{}条",queryProductMap.size());
            }

        }catch(Exception e){
            log.error("[严重异常]加载query产品词缓存异常：", e);
        }

    }

    /**
     * 返回query词对应的产品词
     * @param query
     * @return
     */
    public String getQueryProduct(String query){
        return queryProductMap.getOrDefault(query,null);
    }
}
