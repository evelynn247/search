package com.biyao.search.bs.server.cache.memory;

import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.bs.server.common.util.FileUtil;
import com.biyao.search.common.enums.QueryTermTypeEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * @date 2019/11/1 11:43
 * @description query词属性词库缓存
 */
@Component
@EnableScheduling
public class DictionaryCache {

    private Logger log = LoggerFactory.getLogger(getClass());

    private Map<String, Map<String,Double>> allWordMap;

    public Map<String,Double> getWordMap(String term){
        return allWordMap.get(term) == null ? new HashMap<>() : allWordMap.get(term);
    }


    @PostConstruct
    protected void init(){
        log.error("加载term词典缓存...");
        refresh();
        log.error("加载term词典缓存结束");
    }

    //@Scheduled(cron = "0 0 0/1 * * ? ")
    public void refresh() {

        Map<String,Map<String,Double>> temp = new HashMap<>();

        //读取文件
        List<String> lines = FileUtil.getRemoteFile(CommonConsts.THESAURUS_URL);
        if(lines !=null){
            lines.forEach(line->{
                // 读到之后的操作
                String[] array = line.split("\\t");
                /*商议固定格式*/
                if (array.length >= 2) {
                    String key = array[0];
                    Map<String,Double> value = convert(array[1]);
                    temp.put(key, value);
                }
            });
        }

        if(temp.size()>0){
            allWordMap = temp;
        }
        log.error("刷新term词典缓存结束");

    }

    private Map<String, Double> convert(String termMapStr) {

        Map<String, Double> result = new HashMap<>();
        if (!StringUtils.isBlank(termMapStr)) {
            JSONObject jsonObject = JSON.parseObject(termMapStr);
            for (Map.Entry<String, Object> item : jsonObject.entrySet()) {
                try {
                    String key = QueryTermTypeEnum.getByCode(item.getKey()).getCode();
                    Double value = Double.parseDouble(item.getValue().toString());
                    if (key != null) {
                        result.put(key, value);
                    }
                } catch (Exception e) {
                    log.error("分词term属性解析失败,失败属性：" + item.getKey() + "，失败原因：" + e.getMessage());
                }
            }
        }
        return result;
    }
}
