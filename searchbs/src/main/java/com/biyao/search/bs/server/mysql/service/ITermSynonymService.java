package com.biyao.search.bs.server.mysql.service;

import com.biyao.search.bs.server.mysql.model.TermSynonymPO;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2020/11/30 13:50
 * @description
 */
public interface ITermSynonymService {
    List<TermSynonymPO> getAllInfos();
}
