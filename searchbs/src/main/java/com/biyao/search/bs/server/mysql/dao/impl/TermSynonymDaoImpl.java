package com.biyao.search.bs.server.mysql.dao.impl;

import com.biyao.search.bs.server.mysql.dao.ITermSynonymDao;
import com.biyao.search.bs.server.mysql.dao.mapper.TermSynonymMapper;
import com.biyao.search.bs.server.mysql.model.TermSynonymPO;
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
public class TermSynonymDaoImpl implements ITermSynonymDao {

    @Autowired
    private TermSynonymMapper mapper;

    @Override
    public List<TermSynonymPO> getInfoByPage(int pageIndex, int pageSize) {
        //调用mapper接口  mapper.getInfoByPage()
        return mapper.getInfoByPage(pageIndex,pageSize);
    }
}