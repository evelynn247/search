package com.biyao.search.as.server.cache;

import com.biyao.search.as.server.common.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @desc: query关联类目缓存
 * @author: xiafang
 * @date: 2020/9/3
 */
@Component
@Slf4j
public class QueryCategoryCache {

    /**
     * query和关联类目本地缓存，key为query,value为关联类目及评分，value数据格式：id1:score1,id2:score2,...
     * query id1:score1,id2:score2,...
     */
    private Map<String, Map<Long, Float>> queryCategoryMap = new HashMap<>();

    /**
     * query预测类目文件地址
     */
    @Value("${query.category.url}")
    private String QUERY_CATEGORY_URL;

    /**
     * query和关联类目之间的分隔符
     */
    private static final String QUERY_CATEGORY_SPLITERATOR = "\\t";

    /**
     * 类目id和分数之间的分隔符
     */
    private static final String ID_SCORE_SPLITERATOR = ":";
    /**
     * 多个类目之间的分隔符
     */
    private static final String MULTI_CATEGORY_SPLITERATOR = ",";

    /**
     * query预测类目文件开头注释符号
     */
    private static final String NOTE = "#";

    /**
     * query预测类目文件每行数据都为query和关联类目集合两个元素，用常量2进行校验数据格式
     */
    private static final Integer ELEMENT_NUM = 2;

    @PostConstruct
    public void init() {
        log.info("[操作日志]加载query和关联类目缓存开始");
        refresh();
        log.info("[操作日志]加载query和关联类目结束");
    }

    /**
     * 每6个小时刷新一次
     */
    @Scheduled(cron = "0 0 4/6 * * ?")
    public void refresh() {
        Map<String, Map<Long, Float>> tempQueryCategoryMap = new HashMap<>();
        try {
            //读取文件
            List<String> lines = FileUtil.getRemoteFile(QUERY_CATEGORY_URL);
            if (lines == null) {
                log.error("[一般异常]query和关联类目远程文件加载为null,异常数据地址：{}", QUERY_CATEGORY_URL);
                return;
            }
            for (String line : lines) {
                if (line.startsWith(NOTE)) {
                    continue;
                }
                String[] arrayTemp = line.split(QUERY_CATEGORY_SPLITERATOR);
                if (arrayTemp.length != ELEMENT_NUM) {
                    log.error("[严重异常][数据格式异常]query和关联类目数据格式异常,异常数据文件地址：{},异常数据：{}", QUERY_CATEGORY_URL, arrayTemp);
                    continue;
                }
                String key = arrayTemp[0].trim();
                if (StringUtils.isEmpty(key)) {
                    continue;
                }
                String[] categoryScoreArray = arrayTemp[1].trim().split(MULTI_CATEGORY_SPLITERATOR);
                Map<Long, Float> categoryScoreMap = new HashMap<>();
                for (int i = 0; i < categoryScoreArray.length; i++) {
                    String[] categoryScore = categoryScoreArray[i].split(ID_SCORE_SPLITERATOR);
                    try {
                        categoryScoreMap.put(Long.parseLong(categoryScore[0]), Float.parseFloat(categoryScore[1]));
                    } catch (Exception e) {
                        log.error("[位置异常][数据解析异常]query和关联类目数据格式异常,异常数据文件地址：{},异常信息：", QUERY_CATEGORY_URL, e);
                        continue;
                    }
                }
                if (tempQueryCategoryMap.get(key) != null) {
                    categoryScoreMap.putAll(tempQueryCategoryMap.get(key));
                }
                tempQueryCategoryMap.put(key, categoryScoreMap);
            }
            queryCategoryMap = tempQueryCategoryMap;
            log.info("[操作日志]刷新query和关联类目数据缓存结束, 数据量：{}", tempQueryCategoryMap.size());
        } catch (Exception e) {
            log.error("[严重异常][数据解析异常]query和关联类目数据格式异常,异常数据地址：{}，异常信息：", QUERY_CATEGORY_URL, e);
        }
    }


    /**
     * 根据query查询关联类目
     *
     * @param query 搜索词
     * @return 本地缓存有结果返回对应关联类目及分数，没有结果返回空Map
     */
    public Map<Long, Float> getCategoryScoreMap(String query) {
        return queryCategoryMap.getOrDefault(query, new HashMap<>());
    }
}
