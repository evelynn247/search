package com.biyao.search.bs.server.mysql.service;

import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;

import java.util.List;

/**
 * @desc: 词库系统service层接口
 * @author: xiafang
 * @date: 2020/1/14
 */
public interface ISearchTermService {

    /**
     * 查询符合条件的所有term
     *
     * @param combineSeasonOrSexCondition 查询条件
     * @return 返回符合查询条件的所有term
     */
    List<SearchTermPO> getSearchTermAllByCombineSeasonOrSexCondition(CombineSeasonOrSexCondition combineSeasonOrSexCondition);
}
