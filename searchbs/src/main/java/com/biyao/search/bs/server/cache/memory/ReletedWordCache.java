package com.biyao.search.bs.server.cache.memory;

import com.biyao.search.bs.server.mysql.model.ProductWordPO;
import com.biyao.search.bs.server.mysql.service.impl.ProductWordServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/30 14:38
 * @description
 */
@Component
@Slf4j
public class ReletedWordCache {
    @Autowired
    private ProductWordServiceImpl productWordService;

    /**
     * 商品词 -> 相关词 映射
     */
    private Map<String, Set<String>> reletedMap = new HashMap<>();

    @PostConstruct
    public void init() {
        log.info("[操作日志]加载相关词缓存...");
        refresh();
        log.info("[操作日志]加载相关词缓存结束,更新数据量：{}", reletedMap.size());
    }

    public void refresh() {
        List<ProductWordPO> temp;
        Map<String, Set<String>> tempMap = new HashMap<>();

        try {
            temp = productWordService.getAllInfos();
            if (temp.size() > 0) {
                for (ProductWordPO item : temp) {
                    if(StringUtils.isBlank(item.getRelatedWord())){
                        continue;
                    }
                    Set<String> set = convert2List(item.getRelatedWord());
                    if (set.size() > 0) {
                        tempMap.put(item.getProductWord(), set);
                    }
                }
                if (tempMap.size() > 0) {
                    reletedMap = tempMap;
                }
            }
        } catch (Exception e) {
            log.error("[严重异常]加载相关词缓存异常：" + e.getMessage());
        }

    }

    /**
     * 格式转换
     *
     * @param synonym
     * @return
     */
    private Set<String> convert2List(String synonym) {
        Set<String> result = new HashSet<>();
        String[] array = synonym.split(",");
        if (array.length > 0) {
            result.addAll(Arrays.asList(array));
        }
        return result;
    }

    /**
     * 根据商品词获取相关词
     *
     * @param item
     * @return
     */
    public List<String> getReletedList(String item) {
        if (StringUtils.isBlank(item)) {
            return new ArrayList<>();
        }
        Set<String> temp = reletedMap.get(item);
        return temp == null ? new ArrayList<>() : new ArrayList<>(temp);
    }
}
