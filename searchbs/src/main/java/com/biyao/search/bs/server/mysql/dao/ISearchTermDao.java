package com.biyao.search.bs.server.mysql.dao;

import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;

import java.util.List;

/**
 * @desc: dao层接口,给service层提供服务
 * @author: xiafang
 * @date: 2020/1/14
 */
public interface ISearchTermDao {

    /**
     * 根据传入条件查询词库系统中的数据
     *
     * @return 返回满足条件的所有term
     */
     List<SearchTermPO> getSearchTermByCombineSeasonOrSexCondition(CombineSeasonOrSexCondition combineSeasonOrSexCondition);

    }
