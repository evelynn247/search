package com.biyao.search.bs.server.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2019/12/30 15:27
 * @description
 */
@Data
public class Expression {

    /**
     * 必须条件
     */
    private List<List<String>> parseList = new ArrayList<>();

    /**
     * 提权条件
     */
    private List<List<String>> boostList = new ArrayList<>();

    /**
     * 产品词条件
     */
    private List<String> productWord = new ArrayList<>();
}
