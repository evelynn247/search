package com.biyao.search.bs.server.mysql.dao;

import com.biyao.search.bs.server.mysql.model.TermSynonymPO;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:25
 * @description
 */
public interface ITermSynonymDao {
    List<TermSynonymPO> getInfoByPage(int pageIndex, int pageSize);
}
