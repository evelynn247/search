package com.biyao.search.bs.server.mysql.dao.impl;

import com.biyao.search.bs.server.mysql.dao.ISearchTermDao;
import com.biyao.search.bs.server.mysql.dao.mapper.SearchTermMapper;
import com.biyao.search.bs.server.mysql.model.CombineSeasonOrSexCondition;
import com.biyao.search.bs.server.mysql.model.SearchTermPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @desc:
 * @author: xiafang
 * @date: 2020/1/14
 */
@Repository("searchTermDaoImpl")
public class SearchTermDaoImpl implements ISearchTermDao {

    @Autowired
    private SearchTermMapper searchTermMapper;

    @Override
    public List<SearchTermPO> getSearchTermByCombineSeasonOrSexCondition(CombineSeasonOrSexCondition combineSeasonOrSexCondition){
        //与数据库交互,查询条件通过mapperConditionParam对象传给mapper.xml文件
        return searchTermMapper.getSearchTermByCombineSeasonOrSexCondition(combineSeasonOrSexCondition);
    }

}
