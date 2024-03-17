package com.biyao.search.bs.server.bean;

/**
 * SexWord
 *
 * @author wangbo
 * @version 1.0 2018/6/7
 */
public class SexWord {

    private String oldQuery;
    private String newQuery;
    private String sexWords;

    public SexWord(String oldQuery) {
        this.oldQuery = oldQuery;
    }

    public String getOldQuery() {
        return oldQuery;
    }

    public void setOldQuery(String oldQuery) {
        this.oldQuery = oldQuery;
    }

    public String getNewQuery() {
        return newQuery;
    }

    public void setNewQuery(String newQuery) {
        this.newQuery = newQuery;
    }

    public String getSexWords() {
        return sexWords;
    }

    public void setSexWords(String sexWords) {
        this.sexWords = sexWords;
    }
}
