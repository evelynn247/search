package com.biyao.search.bs.server.mysql.dao.mapper;

import com.biyao.search.bs.server.mysql.model.BrandWordPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:21
 * @description
 */
public interface BrandWordMapper{

    List<BrandWordPO> getInfoByPage(@Param("pageIndex") int pageIndex, @Param("pageSize") int pageSize);

}