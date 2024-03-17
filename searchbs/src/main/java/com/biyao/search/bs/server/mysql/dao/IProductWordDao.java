package com.biyao.search.bs.server.mysql.dao;

import com.biyao.search.bs.server.mysql.model.ProductWordPO;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:25
 * @description
 */
public interface IProductWordDao {

    List<ProductWordPO> getInfoByPage(int pageIndex, int pageSize);
}
