package com.biyao.search.bs.server.mysql.dao.impl;

import com.biyao.search.bs.server.mysql.dao.IProductWordDao;
import com.biyao.search.bs.server.mysql.dao.mapper.ProductWordMapper;
import com.biyao.search.bs.server.mysql.model.ProductWordPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:26
 * @description
 */
@Repository
public class ProductWordDaoImpl implements IProductWordDao {

    @Autowired
    private ProductWordMapper mapper;

    @Override
    public List<ProductWordPO> getInfoByPage(int pageIndex, int pageSize) {
        //调用mapper接口  mapper.getInfoByPage()
        return mapper.getInfoByPage(pageIndex,pageSize);
    }
}