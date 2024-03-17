package com.biyao.search.bs.server.mysql.dao.impl;

import com.biyao.search.bs.server.mysql.dao.IBrandWordDao;
import com.biyao.search.bs.server.mysql.dao.mapper.BrandWordMapper;
import com.biyao.search.bs.server.mysql.model.BrandWordPO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:25
 * @description
 */
@Repository
public class BrandWordDaoImpl implements IBrandWordDao {

    @Autowired
    private BrandWordMapper mapper;

    @Override
    public List<BrandWordPO> getInfoByPage(int pageIndex, int pageSize) {
        //调用mapper接口  mapper.getInfoByPage()
        return mapper.getInfoByPage(pageIndex,pageSize);
    }
}