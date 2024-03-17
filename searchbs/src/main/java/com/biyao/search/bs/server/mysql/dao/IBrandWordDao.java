package com.biyao.search.bs.server.mysql.dao;

import com.biyao.search.bs.server.mysql.model.BrandWordPO;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:24
 * @description
 */
public interface IBrandWordDao {
    List<BrandWordPO> getInfoByPage(int pageIndex, int pageSize);
}
