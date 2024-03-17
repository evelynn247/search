package com.biyao.search.bs.server.controller;


import java.util.List;
import java.util.Map;

/**
 * 运维监测接口
 */
public interface TestController {

    Map<String, String> testonline();

    List<String> getRelatedWordsByProductWord(String productWord);

    Map<String, String> refreshRelatedWordCache();


}