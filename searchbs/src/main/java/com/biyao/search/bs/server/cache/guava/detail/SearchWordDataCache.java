package com.biyao.search.bs.server.cache.guava.detail;

import com.biyao.search.bs.server.bean.QueryWord;
import com.biyao.search.bs.server.bean.QueryWordTreeNode;
import com.biyao.search.bs.server.bean.ScoreWord;
import com.biyao.search.bs.server.cache.guava.BaseGuavaCache;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 搜索词
 *
 * @author wangbo
 * @version 1.0 2018/6/6
 */
@Component
public class SearchWordDataCache extends BaseGuavaCache<String, Map<String, QueryWordTreeNode>> {

    private Logger logger = LoggerFactory.getLogger(SearchWordDataCache.class);

    private final static String KEY = "search:search_word_data";

    @Override
    @PostConstruct
    public void loadValueWhenStarted() {
        this.setMaxSize(10000);
        this.setRefreshDuration(1);
        this.setRefreshTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected Map<String, QueryWordTreeNode> getValueWhenExpired(String key) throws Exception {
        Map<String, QueryWordTreeNode> res = new ConcurrentHashMap<>();

        List<QueryWord> list = getQueryWords();
        for (QueryWord q : list) {
            buildTree(q, res);
        }

        return res;
    }

    public Map<String, ScoreWord> searchTree(String query) {
        Map<String, ScoreWord> res = new HashMap<>();
        try {
            if (null == query || query.trim().length() == 0)
                return res;

            int length = query.toCharArray().length;
            for (int i = 0; i < length - 1; i++) {
                for (ScoreWord sr : searchTree(query.substring(i, length).toCharArray()))
                    res.put(sr.getWord(), sr);
            }
        } catch (Exception e) {
            logger.error("QueryWordTreeCache searchTree error", e);
        }

        return res;
    }

    private Set<ScoreWord> searchTree(char[] queryChar) throws Exception {
        Set<ScoreWord> res = new HashSet<>();
        String key = String.valueOf(queryChar[0]);
        QueryWordTreeNode root = getValue(KEY).get(key);
        if (null != root) {
            if (queryChar.length == 1) {
                if (root.getScore() > 0) {
                    res.add(new ScoreWord(key, root.getScore()));
                }
            } else
                searchTree(root, queryChar, 1, res);
        }

        return res;
    }

    private void searchTree(QueryWordTreeNode node, char[] queryChar, int index, Set<ScoreWord> res) {
        String key = String.valueOf(queryChar[index]);
        QueryWordTreeNode next = null;
        for (QueryWordTreeNode n : node.getChilds()) {
            if (n.getWord().equals(key)) {
                next = n;
                break;
            }
        }

        if (null != next) {
            if (next.getScore() > 0)
                res.add(new ScoreWord(getMatchWord(queryChar, index + 1), next.getScore()));

            if (queryChar.length != index + 1) {
                searchTree(next, queryChar, index + 1, res);
            }
        }
    }

    private String getMatchWord(char[] queryChar, int index) {
        char[] q = new char[index];
        for (int i = 0; i < index; i++)
            q[i] = queryChar[i];

        return new String(q);
    }

    public void buildTree(QueryWord queryWord, Map<String, QueryWordTreeNode> tempMap) {
        try {
            char[] queryChar = queryWord.getWord().toCharArray();

            String firstWord = String.valueOf(queryChar[0]);

            QueryWordTreeNode node = tempMap.computeIfAbsent(firstWord, w -> new QueryWordTreeNode(String.valueOf(w)));

            if (queryChar.length == 1) {
                //单字降权
                node.setScore(queryWord.getScore() / 2);
            } else {
                buildTree(node, queryChar, 1, queryWord.getScore());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void buildTree(QueryWordTreeNode node, char[] words, int index, int score) {
        String word = String.valueOf(words[index]);
        QueryWordTreeNode next = null;
        for (QueryWordTreeNode n : node.getChilds()) {
            if (n.getWord().equals(word)) {
                next = n;
                break;
            }
        }

        if (null != next) {
            if (words.length != index + 1)
                buildTree(next, words, index + 1, score);
            else
                next.setScore(next.getScore() == -1 ? score : next.getScore() + score);
        } else {
            QueryWordTreeNode child = new QueryWordTreeNode(word);
            node.getChilds().add(child);

            if (words.length != index + 1)
                buildTree(child, words, index + 1, score);
            else
                child.setScore(score);
        }
    }

    public List<QueryWord> getQueryWords() throws Exception {
        List<QueryWord> res = new ArrayList<>();

        try {
            File file = new File(CommonConsts.USER_SEARCH_WORD_PATH);
            if (file.exists()) {
                try (
                        FileReader fileReader = new FileReader(file);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                ) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String l = line.trim();
                        if (StringUtils.isNotBlank(l) && !l.startsWith("#")) {
                            String[] lines = line.split("\t");
                            if (lines.length != 5 || lines[0].trim().length() == 0)
                                continue;

                            QueryWord queryWord = new QueryWord();
                            queryWord.setWord(lines[0]);
                            queryWord.setDisplay(Integer.parseInt(lines[1]));
                            queryWord.setClick(Integer.parseInt(lines[2]));
                            queryWord.setBuy(Integer.parseInt(lines[3]));
                            queryWord.setScore(Integer.parseInt(lines[4]));
                            res.add(queryWord);
                        }
                    }
                }
            } else {
                throw new Exception("产品词+修饰词分数文件不存在" + CommonConsts.USER_SEARCH_WORD_PATH);
            }
        } catch (Exception e) {
            logger.error("产品词+修饰词分数文件失败：{}", e);
            throw e;
        }

        return res;
    }

    public void refreshValue() {
        refreshValue(KEY);
    }
}
