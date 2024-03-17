package com.biyao.search.bs.server.mysql.dao.mapper;

import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;

import java.util.List;

/**
 * @desc:
 * @author: xiafang
 * @date: 2020/1/13
 */
public interface SearchTermMapper {
    /**
     * 每次最多支持查询1000条
     *
     * @param combineSeasonOrSexCondition 和性别季节结合term查询条件
     * @return 返回符合查询条件的所有term
     */
    List<SearchTermPO> getSearchTermByCombineSeasonOrSexCondition(CombineSeasonOrSexCondition combineSeasonOrSexCondition);

}
