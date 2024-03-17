package com.biyao.search.bs.server.query.impl;

import com.biyao.search.bs.server.bean.ScoreWord;
import com.biyao.search.bs.server.bean.SearchWordComparable;
import com.biyao.search.bs.server.bean.SexWord;
import com.biyao.search.bs.server.cache.guava.detail.ProductModifyWordScoreCache;
import com.biyao.search.bs.server.cache.guava.detail.SearchWordDataCache;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 关联分词
 *
 * @author wangbo
 * @version 1.0 2018/6/6
 */
@Component
public class RelativeQueryParser {

    private static Logger logger = LoggerFactory.getLogger(RelativeQueryParser.class);

    private static int RES_SIZE = 2;

    @Autowired
    private SearchWordDataCache searchWordDataCache;

    @Autowired
    private ProductModifyWordScoreCache productModifyWordScoreCache;

    public List<String> parse(String query) {
        List<String> res = new ArrayList<>();
        try {
            SexWord sexWords = parseSexWord(query);
            query = sexWords.getNewQuery();

            Map<String, ScoreWord> searchRes = searchWordDataCache.searchTree(query);

            List<ScoreWord> productWords = new ArrayList<>();
            List<ScoreWord> modifierWords = new ArrayList<>();

            splitsearchRes(searchRes, query, productWords, modifierWords);

            filterWord(query, productWords);
            filterWord(query, modifierWords);

            List<String> rs;
            if (productWords.size() == 0) {
                rs = getZeroProductWordRes(modifierWords);
            } else if (productWords.size() == 1) {
                rs = getOneProductWordRes(modifierWords, productWords.get(0).getWord(), query);
            } else {
                rs = getTwoProductWordRes(productWords);
            }

            for (String r : rs)
                res.add((r + " " + sexWords.getSexWords()).trim());
        } catch (Exception e) {
            logger.error("RelativeQueryParser parse error", e);
        }

        return res;
    }

    /**
     * 从query中解析出产品词
     * @param query
     * @return
     */
    public String getProductWord(String query){
        String result = "";
        SexWord sexWords = parseSexWord(query);
        query = sexWords.getNewQuery();

        Map<String, ScoreWord> searchRes = searchWordDataCache.searchTree(query);

        List<ScoreWord> productWords = new ArrayList<>();
        List<ScoreWord> modifierWords = new ArrayList<>();

        splitsearchRes(searchRes, query, productWords, modifierWords);
        if (null != productWords && productWords.size() > 0){
            productWords.sort((a, b) -> b.getScore() - a.getScore());
            result = productWords.get(0).getWord();
        }
        return result;
    }

    private List<String> getOneProductWordRes(List<ScoreWord> modifierWords, String productWord, String query) {
        List<String> res = new ArrayList<>();

        //清洗修饰词
        if (modifierWords.size() > 0) {
            for (char c : query.toCharArray()) {
                for (int i = modifierWords.size() - 1; i >= 0; i--) {
                    ScoreWord rs = modifierWords.get(i);
                    if (rs.getWord().contains(String.valueOf(c)))
                        modifierWords.remove(i);
                }
            }
        }

        int maxScore = -1;
        String word = null;
        for (ScoreWord sr : modifierWords) {
            Integer score = productModifyWordScoreCache.getWordScore(productWord, sr.getWord());
            if (null != score && score > maxScore) {
                maxScore = score;
                word = productWord + " " + sr.getWord();
            }
        }

        if (null != word)
            res.add(word);

        res.add(productWord);

        return res;
    }

    private List<String> getTwoProductWordRes(List<ScoreWord> productWords) {
        List<String> res = new ArrayList<>();

        String word1 = productWords.get(0).getWord();
        String word2 = productWords.get(1).getWord();

        res.add(word1);
        res.add(word2);

        boolean repeat = checkRepeat(word1, word2);

        if (repeat) {
            if (productWords.get(1).getScore() > productWords.get(0).getScore())
                Collections.reverse(res);
        } else {
            if (word2.length() > word1.length())
                Collections.reverse(res);
        }

        return res;
    }

    private List<String> getZeroProductWordRes(List<ScoreWord> modifierWords) {
        List<String> res = new ArrayList<>();
        for (ScoreWord sr : modifierWords) {
            res.add(sr.getWord());
            if (res.size() >= RES_SIZE)
                break;
        }

        return res;
    }

    /**
     * 解析性别词
     *
     * @param query
     * @return
     */
    public SexWord parseSexWord(String query) {
        SexWord res = new SexWord(query);
        StringBuilder sb = new StringBuilder();
        boolean find = false;
        for (String word : CommonConsts.MEN_SEX_WORDS) {
            if (query.contains(word)) {
                res.setNewQuery(query.replaceAll(word, " "));
                find = true;
            }
        }

        if (find)
            sb.append(CommonConsts.MEN_SEX_WORDS[CommonConsts.MEN_SEX_WORDS.length - 1]);

        find = false;
        for (String word : CommonConsts.WOMEN_SEX_WORDS) {
            if (query.contains(word)) {
                res.setNewQuery(query.replaceAll(word, " "));
                find = true;
            }
        }

        if (find)
            sb.append(" ").append(CommonConsts.WOMEN_SEX_WORDS[CommonConsts.WOMEN_SEX_WORDS.length - 1]);

        res.setNewQuery(query.trim());
        res.setSexWords(sb.toString());

        return res;
    }

    /**
     * 解析产品词
     *
     * @param searchRes
     * @param query
     * @param productWords
     * @param modifierWords
     */
    private void splitsearchRes(Map<String, ScoreWord> searchRes, String query, List<ScoreWord> productWords, List<ScoreWord> modifierWords) {
        Set<String> productTerms = productModifyWordScoreCache.getProductWords();
        Set<Integer> endingIndex = getEndingIndex(query);

        for (String pt : productTerms) {
            ScoreWord sr = searchRes.get(pt.trim());
            if (null != sr) {
                sr.setProductWord(true);
                //提权
                int lastIndex = getLastIndex(query, sr.getWord());
                if (endingIndex.contains(lastIndex) && sr.getScore() != -1)
                    sr.setScore(sr.getScore() * 10);
            }
        }

        for (ScoreWord sr : searchRes.values()) {
            if (sr.isProductWord())
                productWords.add(sr);
            else
                modifierWords.add(sr);
        }

        sortScoreWord(productWords);
        sortScoreWord(modifierWords);
    }

    public void sortScoreWord(List<ScoreWord> list) {
        if (list.size() == 0)
            return;

        Comparator comparable = new SearchWordComparable();
        Collections.sort(list, comparable);
    }

    /**
     * 过滤跟原始词相同的词
     *
     * @param query
     * @param list
     */
    private void filterWord(String query, List<ScoreWord> list) {
        for (int i = list.size() - 1; i >= 0; i--) {
            if (query.equals(list.get(i).getWord()))
                list.remove(i);
        }
    }

    private boolean checkRepeat(String word1, String word2) {
        boolean repeat = false;
        for (char c : word1.toCharArray()) {
            for (char b : word2.toCharArray()) {
                if (c == b) {
                    repeat = true;
                    break;
                }
            }
            if (repeat)
                break;
        }

        return repeat;
    }

    /**
     * 获取结尾index
     *
     * @param query
     * @return
     */
    private Set<Integer> getEndingIndex(String query) {
        Set<Integer> res = new HashSet<>();
        char[] charArray = query.toCharArray();
        for (int i = 0; i < charArray.length; i++) {
            if (charArray[i] == 32)
                res.add(i);
        }

        res.add(charArray.length);

        return res;
    }

    private int getLastIndex(String total, String match) {
        int lastIndex = total.lastIndexOf(match);
        if (lastIndex < 0)
            return lastIndex;

        return lastIndex + match.length();
    }
}
