package com.biyao.search.bs.server.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * QueryWordTreeNode
 *
 * @author wangbo
 * @version 1.0 2018/6/5
 */
public class QueryWordTreeNode {

    private String word;
    private int score = -1;

    public QueryWordTreeNode(String word) {
        this.word = word;
    }

    private List<QueryWordTreeNode> childs = new ArrayList<>();

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public List<QueryWordTreeNode> getChilds() {
        return childs;
    }

    public void setChilds(List<QueryWordTreeNode> childs) {
        this.childs = childs;
    }
}
