package com.biyao.search.bs.server.mysql.service.impl;

import com.biyao.search.bs.server.mysql.dao.IProductWordDao;
import com.biyao.search.bs.server.mysql.model.ProductWordPO;
import com.biyao.search.bs.server.mysql.service.IProductWordService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/30 13:51
 * @description
 */
@Service
@Slf4j
public class ProductWordServiceImpl implements IProductWordService {

    @Autowired
    private IProductWordDao dao;

    private static int pageSize = 500;
    /**
     * 获取全量有效数据，默认分页数量 500
     * @return
     */
    @Override
    public List<ProductWordPO> getAllInfos() {
        List<ProductWordPO> result = new ArrayList<>();
        int pageIndex = 0;
        //通过dao层接口同步全量数据
        try {
            while (true) {
                List<ProductWordPO> tempList = dao.getInfoByPage(pageIndex, pageSize);
                if (tempList.size() == 0) {
                    break;
                }
                result.addAll(tempList);
                pageIndex = pageIndex + tempList.size();
            }
        } catch (Exception e) {
            log.error("[严重异常]查询产品词&相关词数据异常", e);
            return new ArrayList<>();
        }
        return result;
    }
}
