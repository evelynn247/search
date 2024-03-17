package com.biyao.search.bs.server.bean;

/**
 * SearchTreeRes
 *
 * @author wangbo
 * @version 1.0 2018/6/5
 */
public class ScoreWord {

    private String word;
    private int score;
    private boolean isProductWord;

    public ScoreWord(String word, int score) {
        this.word = word;
        this.score = score;
    }

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

    public boolean isProductWord() {
        return isProductWord;
    }

    public void setProductWord(boolean productWord) {
        isProductWord = productWord;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ScoreWord) {
            ScoreWord sr = (ScoreWord) obj;
            return this.word.equals(sr.getWord());
        }

        return false;
    }

    @Override
    public int hashCode() {
        String id = this.word;
        return id.hashCode();
    }
}
