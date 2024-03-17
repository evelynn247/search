package com.biyao.search.bs.server.mysql.service.impl;

import com.biyao.search.bs.server.mysql.dao.ISearchTermDao;
import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;
import com.biyao.search.bs.server.mysql.service.ISearchTermService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @desc: 词库系统集合
 * @author: xiafang
 * @date: 2020/1/14
 */
@Service
@Slf4j
public class SearchTermServiceImpl implements ISearchTermService {

    @Autowired
    private ISearchTermDao searchTermDao;

    /**
     * 和季节、性别结合的term查询
     *
     * @return
     */
    @Override
    public List<SearchTermPO> getSearchTermAllByCombineSeasonOrSexCondition(CombineSeasonOrSexCondition combineSeasonOrSexCondition) {

        List<SearchTermPO> searchTermPOList = new ArrayList<>();
        if (combineSeasonOrSexCondition.getCombineSeason() == null || combineSeasonOrSexCondition.getCombineSex() == null) {
            log.error("必须指定查询条件");
            return new ArrayList<>();
        }
        while (true) {
            //通过dao层接口查询符合条件的term,每次最多查询1000条
            List<SearchTermPO> tempList = searchTermDao.getSearchTermByCombineSeasonOrSexCondition(combineSeasonOrSexCondition);
            //tempList.size() == 0，查询结束，跳出循环
            if (tempList.size() == 0) {
                break;
            }
            searchTermPOList.addAll(tempList);
            //更新起始偏移量
            combineSeasonOrSexCondition.setTermId(tempList.get(tempList.size() - 1).getTermId());
        }
        return searchTermPOList;
    }
}
