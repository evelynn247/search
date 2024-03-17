package com.biyao.search.bs.server.mysql.dao.mapper;

import com.biyao.search.bs.server.mysql.model.TermSynonymPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/27 17:22
 * @description
 */
public interface TermSynonymMapper {
    List<TermSynonymPO> getInfoByPage(@Param("pageIndex") int pageIndex, @Param("pageSize") int pageSize);
}
