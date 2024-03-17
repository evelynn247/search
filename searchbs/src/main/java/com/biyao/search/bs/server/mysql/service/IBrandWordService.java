package com.biyao.search.bs.server.mysql.service;

import com.biyao.search.bs.server.mysql.model.BrandWordPO;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/30 13:48
 * @description
 */
public interface IBrandWordService {
    List<BrandWordPO> getAllInfos();
}
