package com.biyao.search.bs.server.mysql.dao;

import com.biyao.search.bs.server.mysql.model.CategoryPredictionPO;

import java.util.List;

/**
 * @Description:
 * @Date: 2020/2/25
 * @Author: xiafang
 */
public interface ICategoryPredictionDao {
    /**
     * dao层接口，通过mapper接口查询数据库
     * @param pageIndex
     * @param pageSize
     * @return
     */
    List<CategoryPredictionPO> getCategoryPredictionByPage(int pageIndex, int pageSize);
}
