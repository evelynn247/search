package com.biyao.search.bs.server.mysql.service;

import com.biyao.search.bs.server.mysql.model.CategoryPredictionPO;

import java.util.List;

/**
 * @Description:
 * @Date: 2020/2/24
 * @Author: xiafang
 */
public interface ICategoryPrediction {
    /**
     * 全量同步类目预测表category_prediction中的数据，对外提供服务
     * @param pageIndex 表示每次查询的游标起始位置
     * @param pageSize 表示每次查询的记录数量
     * @return  返回query对应的相关类目信息，其中一个CategoryPredictionPO实例对应数据库中一条记录
     */
    List<CategoryPredictionPO> getCategoryPredictionByPage(int pageIndex, int pageSize);
}
