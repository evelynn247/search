package com.biyao.search.bs.server.cache.guava.detail;

import com.biyao.search.bs.server.bean.ProductModifierConf;
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
import java.util.concurrent.TimeUnit;

/**
 * 产品词+修饰词分数
 *
 * @author wangbo
 * @version 1.0 2018/6/6
 */
@Component
public class ProductModifyWordScoreCache extends BaseGuavaCache<String, ProductModifierConf> {

    private Logger logger = LoggerFactory.getLogger(ProductModifyWordScoreCache.class);

    private final static String KEY = "search:product_modify_word_score";

    @Override
    @PostConstruct
    public void loadValueWhenStarted() {
        this.setMaxSize(10000);
        this.setRefreshDuration(1);
        this.setRefreshTimeUnit(TimeUnit.DAYS);
    }

    @Override
    protected ProductModifierConf getValueWhenExpired(String key) throws Exception {
        ProductModifierConf conf = new ProductModifierConf();
        Map<String, Integer> productModifierScore = conf.getProductModifierScore();
        Set<String> productWords = conf.getProductWords();
        Map<String, Set<ScoreWord>> productModifierMap = conf.getProductModifierMap();
        try {
            File file = new File(CommonConsts.PRODUCT_MODIFY_SCORE_PATH);
            if (file.exists()) {
                try (
                        FileReader fileReader = new FileReader(file);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                ) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String l = line.trim();
                        if (StringUtils.isNotBlank(l) && !l.startsWith("#")) {
                            String[] ls = l.split("\t");
                            if (ls.length == 3) {
                                try {
                                    String productWord = ls[0].trim();
                                    String modifyWord = ls[1].trim();
                                    int score = Integer.parseInt(ls[2]);
                                    productWords.add(productWord);
                                    productModifierScore.put(productWord + " " + modifyWord, score);

                                    Set<ScoreWord> set = productModifierMap.computeIfAbsent(productWord, k -> new HashSet<>());
                                    set.add(new ScoreWord(modifyWord, score));
                                } catch (Exception e) {
                                    logger.error("解析产品词+修饰词分数异常:" + l, e);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new Exception("产品词+修饰词分数文件不存在" + CommonConsts.PRODUCT_MODIFY_SCORE_PATH);
            }
        } catch (Exception e) {
            logger.error("产品词+修饰词分数文件失败：{}", e);
            throw e;
        }

        return conf;
    }

    public Integer getWordScore(String productWord, String modifyWord) {
        try {
            ProductModifierConf conf = getValue(KEY);
            if (null == conf)
                return null;

            return conf.getProductModifierScore().get(productWord + " " + modifyWord);
        } catch (Exception e) {
            logger.error("ProductModifyWordScoreCache getWordScore error", e);
        }

        return null;
    }

    public Set<String> getProductWords() {
        Set<String> res = new HashSet<>();
        try {
            ProductModifierConf conf = getValue(KEY);
            if (null == conf)
                return null;

            return conf.getProductWords();
        } catch (Exception e) {
            logger.error("ProductModifyWordScoreCache getProductWords error", e);
        }

        return res;
    }

    public List<ScoreWord> getModifyWordsByProductWord(String productWord) {
        List<ScoreWord> res = new ArrayList<>();
        try {
            ProductModifierConf conf = getValue(KEY);
            if (null == conf)
                return res;

            Set<ScoreWord> set = conf.getProductModifierMap().get(productWord);
            if (null == set)
                return res;

            res.addAll(set);
        } catch (Exception e) {
            logger.error("ProductModifyWordScoreCache getModifyWordsByProductWord error", e);
        }

        return res;
    }

    public void refreshValue() {
        refreshValue(KEY);
    }
}
