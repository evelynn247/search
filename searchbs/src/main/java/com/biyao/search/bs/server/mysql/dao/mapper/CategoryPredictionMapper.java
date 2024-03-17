package com.biyao.search.bs.server.mysql.dao.mapper;

import com.biyao.search.bs.server.mysql.model.CategoryPredictionPO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Description:
 * @Date: 2020/2/24
 * @Author: xiafang
 */
public interface CategoryPredictionMapper {
    /**
     * mapper接口，与数据库交互, 分页查询, 每次最多支持查询1000条
     *
     * @param pageIndex
     * @param pageSize
     * @return
     */
    List<CategoryPredictionPO> getCategoryPredictionByPage(@Param("pageIndex") int pageIndex, @Param("pageSize") int pageSize);
}
