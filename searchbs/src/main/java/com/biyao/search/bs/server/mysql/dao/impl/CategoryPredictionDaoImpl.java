package com.biyao.search.bs.server.mysql.dao.impl;


import com.biyao.search.bs.server.mysql.dao.ICategoryPredictionDao;
import com.biyao.search.bs.server.mysql.dao.mapper.CategoryPredictionMapper;
import com.biyao.search.bs.server.mysql.model.CategoryPredictionPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Description:
 * @Date: 2020/2/24
 * @Author: xiafang
 */
@Repository
public class CategoryPredictionDaoImpl implements ICategoryPredictionDao {

    @Autowired
    private CategoryPredictionMapper categoryPredictionMapper;


    @Override
    public List<CategoryPredictionPO> getCategoryPredictionByPage(int pageIndex, int pageSize) {
        return categoryPredictionMapper.getCategoryPredictionByPage(pageIndex,pageSize);
    }
}
