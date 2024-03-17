package com.biyao.search.bs.server.cache.memory;

import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;
import com.biyao.search.bs.server.mysql.service.ISearchTermService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author zj
 * @version 1.0
 * @date 2019/12/24 10:49
 * @description 性别、季节词库缓存
 */
@Component
@EnableScheduling
public class TermCombinedWithSexOrSeasonCache {

    @Resource
    private ISearchTermService searchTermService;

    private Logger log = LoggerFactory.getLogger(getClass());


    private Set<String> combinedWithSexTermSet = new HashSet<>();

    private Set<String> combinedWithSeasonTermSet = new HashSet<>();

    @PostConstruct
    public void init() {
        log.info("[操作日志][定时任务]加载性别、季节缓存...");
        refresh();
        log.info("[操作日志][定时任务]加载性别、季节缓存结束");
    }

    public void refresh() {

        Set<String> tempSex = new HashSet<>();
        Set<String> tempSeason = new HashSet<>();
        try {
            refreshCombineSexOrSeasonTermByMysql(tempSex, tempSeason);
            combinedWithSexTermSet = tempSex;
            combinedWithSeasonTermSet = tempSeason;
            log.info("[操作日志][定时任务]刷新性别、季节词库数据缓存结束,可与性别词结合term数量：{}，可与季节词结合term数量：{}", tempSex.size(), tempSeason.size());
        } catch (Exception e) {
            log.error("[严重异常][定时任务]刷新性别、季节词库数据缓存异常,异常信息：{}", e);
        }

    }

    private void refreshCombineSexOrSeasonTermByMysql(Set<String> tempSex, Set<String> tempSeason) {

        //初始化查询条件
        CombineSeasonOrSexCondition combineSeasonOrSexCondition = new CombineSeasonOrSexCondition();
        //combineSeasonOrSexCondition.setTermId(1);
        combineSeasonOrSexCondition.setCombineSeason(true);
        combineSeasonOrSexCondition.setCombineSex(true);

        //查询符合以上条件的term词
        List<SearchTermPO> searchTermPOList = searchTermService.getSearchTermAllByCombineSeasonOrSexCondition(combineSeasonOrSexCondition);

        //解析
        for (SearchTermPO po : searchTermPOList) {
            if (po.getCombineSex()) {
                tempSex.add(po.getTerm());
            }
            if (po.getCombineSeason()) {
                tempSeason.add(po.getTerm());
            }
        }
    }

    public boolean canCombinedWithSex(String query) {
        return combinedWithSexTermSet.contains(query);
    }

    public boolean canCombinedWithSeason(String query) {
        return combinedWithSeasonTermSet.contains(query);
    }
}
