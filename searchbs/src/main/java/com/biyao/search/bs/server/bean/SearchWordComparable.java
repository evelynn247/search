package com.biyao.search.bs.server.bean;

import java.util.Comparator;

/**
 * SearchWordComparable
 *
 * @author wangbo
 * @version 1.0 2017/7/25
 */
public class SearchWordComparable implements Comparator<ScoreWord> {

    @Override
    public int compare(ScoreWord o1, ScoreWord o2) {
        return o1.getScore() < o2.getScore() ? 1 : -1;
    }
}
