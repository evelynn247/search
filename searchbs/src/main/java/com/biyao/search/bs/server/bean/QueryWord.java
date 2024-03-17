package com.biyao.search.bs.server.bean;

/**
 * 原始搜索词
 *
 * @author wangbo
 * @version 1.0 2018/6/5
 */
public class QueryWord {

    private String word;
    private int display;
    private int click;
    private int buy;
    private int score;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public int getDisplay() {
        return display;
    }

    public void setDisplay(int display) {
        this.display = display;
    }

    public int getClick() {
        return click;
    }

    public void setClick(int click) {
        this.click = click;
    }

    public int getBuy() {
        return buy;
    }

    public void setBuy(int buy) {
        this.buy = buy;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
