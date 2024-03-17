package com.biyao.search.bs.server.mysql.service.impl;

import com.biyao.search.bs.server.mysql.dao.ICategoryPredictionDao;
import com.biyao.search.bs.server.mysql.model.CategoryPredictionPO;
import com.biyao.search.bs.server.mysql.service.ICategoryPrediction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description:
 * @Date: 2020/2/25
 * @Author: xiafang
 */
@Service
@Slf4j
public class CategoryPredictionImpl implements ICategoryPrediction {
    @Autowired
    private ICategoryPredictionDao iCategoryPredictionDao;

    @Override
    public List<CategoryPredictionPO> getCategoryPredictionByPage(int pageIndex, int pageSize) {
        //首先校验参数
        if (pageIndex < 0 || pageSize > 1000) {
            log.error("参数错误！pageIndex={},pageSize={}", pageIndex, pageSize);
            return new ArrayList<>();
        }

        List<CategoryPredictionPO> categoryPredictionPOList = new ArrayList<>();
        //通过dao层接口同步全量数据
        try {
            while (true) {
                List<CategoryPredictionPO> tempList = iCategoryPredictionDao.getCategoryPredictionByPage(pageIndex, pageSize);
                if (tempList.size() == 0) {
                    break;
                }
                categoryPredictionPOList.addAll(tempList);
                pageIndex = pageIndex + tempList.size();
            }
        } catch (Exception e) {
            log.error("查询类目预测数据异常", e);
        }
        return categoryPredictionPOList;
    }

}
