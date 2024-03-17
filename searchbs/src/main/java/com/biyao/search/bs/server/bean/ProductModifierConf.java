package com.biyao.search.bs.server.bean;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 产品词修饰词分数配置
 *
 * @author wangbo
 * @version 1.0 2018/6/7
 */
public class ProductModifierConf {

    private Set<String> productWords = new HashSet<>();
    private Map<String, Integer> productModifierScore = new HashMap<>();
    private Map<String, Set<ScoreWord>> productModifierMap = new HashMap<>();

    public Set<String> getProductWords() {
        return productWords;
    }

    public void setProductWords(Set<String> productWords) {
        this.productWords = productWords;
    }

    public Map<String, Integer> getProductModifierScore() {
        return productModifierScore;
    }

    public void setProductModifierScore(Map<String, Integer> productModifierScore) {
        this.productModifierScore = productModifierScore;
    }

    public Map<String, Set<ScoreWord>> getProductModifierMap() {
        return productModifierMap;
    }

    public void setProductModifierMap(Map<String, Set<ScoreWord>> productModifierMap) {
        this.productModifierMap = productModifierMap;
    }
}
