package com.biyao.search.bs.server.common.consts;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.List;

public class CommonConsts {

    public final static String REMOTE_CONF_PATH = "http://conf.nova.biyao.com/search/as/conf/";

    public final static String NEW_REMOTE_CONF_PATH = "http://conf.nova.biyao.com/nova/";

    // 产品词分词远程下载地址
    public final static String PRODUCT_TERMS_URL = REMOTE_CONF_PATH + "product_term.dic";
    // 品牌词分词远程下载地址
    public final static String BRAND_TERMS_URL = REMOTE_CONF_PATH + "brand_term.dic";
    // 属性词分词远程下载地址
    public final static String ATTRIBUTE_TERMS_URL = REMOTE_CONF_PATH + "attribute_term.dic";
    // 功能词分词远程下载地址
    public final static String FEATURE_TERMS_URL = REMOTE_CONF_PATH + "feature_term.dic";
    // 性别判断规则配置文件远程下载地址
    public final static String GENDER_MARK_RULES_URL = REMOTE_CONF_PATH + "gender_mark_rules.conf";
    // 性别词分词重写规则配置文件远程下载地址
    public final static String GENDER_REWRITE_RULES_URL = REMOTE_CONF_PATH + "gender_rewrite_rules.conf";
    // 产品词分词改写内容远程下载地址
    public final static String BRAND_QUERY_REWRITE_URL = REMOTE_CONF_PATH + "brand_query_rewrite.conf";
    // 商品标签配置远程下载地址
    public final static String PRODUCT_TAGS_URL = REMOTE_CONF_PATH + "product_tags.conf";
    // 产品修饰词分数下载地址
    public final static String PRODUCT_MODIFY_SCORE_URL = REMOTE_CONF_PATH + "product_modifier.conf";
    // 用户搜索词数据下载地址
    public final static String USER_SEARCH_WORD_URL = REMOTE_CONF_PATH + "trie_query.conf";
    // 专题配置下载地址
    public final static String SPECIAL_TOPIC_URL = REMOTE_CONF_PATH + "special_topic.conf";
    // 产品词-facet对应文件下载地址
    public final static String PRODUCT_WORD_FACET_URL = REMOTE_CONF_PATH + "product_word_facet.conf";

    public final static String BRAD_WORD_URL = REMOTE_CONF_PATH + "brand_word.conf";
    //term词典下载地址
    public final static String THESAURUS_URL = NEW_REMOTE_CONF_PATH + "by_mark_dict.dic";

    //性别、季节词典下载地址
    public final static String SEXSEASON_URL = NEW_REMOTE_CONF_PATH + "by_term_combined_with_sexorseason.dic";

    //同义词词典下载地址
    public final static String SYNONYMS_URL = NEW_REMOTE_CONF_PATH + "synonyms.txt";


    // 本地配置文件路径
    public final static String CONF_PATH = Thread.currentThread().getContextClassLoader().getResource("").getPath() + "/conf/";

    // 产品词分词字典本地地址
    public final static String PRODUCT_DIC_PATH = CONF_PATH + "product_term.dic";
    // 品牌词分词字典本地地址
    public final static String BRAND_DIC_PATH = CONF_PATH + "brand_term.dic";
    // 属性词分词字典本地地址
    public final static String ATTRIBUTE_DIC_PATH = CONF_PATH + "attribute_term.dic";
    // 功能词分词字典本地地址
    public final static String FEATURE_DIC_PATH = CONF_PATH + "feature_term.dic";
    // 性别判断规则配置文件本地地址
    public final static String GENDER_MARK_RULES_PATH = CONF_PATH + "gender_mark_rules.conf";
    // 性别词分词重写规则配置文件本地地址
    public final static String GENDER_REWRITE_RULES_PATH = CONF_PATH + "gender_rewrite_rules.conf";
    // 产品词分词改写内容本地地址
    public final static String BRAND_QUERY_REWRITE_PATH = CONF_PATH + "brand_query_rewrite.conf";
    // 商品标签配置远程本地地址
    public final static String PRODUCT_TAGS_PATH = CONF_PATH + "product_tags.conf";
    // 产品修饰词分数本地地址
    public final static String PRODUCT_MODIFY_SCORE_PATH = CONF_PATH + "product_modifier.conf";
    // 用户搜索词数据本地地址
    public final static String USER_SEARCH_WORD_PATH = CONF_PATH + "trie_query.conf";
    // 专题配置本地地址
    public final static String SPECIAL_TOPIC_PATH = CONF_PATH + "special_topic.conf";
    // 产品词-facet文件本地地址
    public final static String PRODUCT_WORD_FACET_PATH = CONF_PATH + "product_word_facet.conf";
    // 品牌词本地地址
    public final static String BRAN_WORD_PATH = CONF_PATH + "brand_word.conf";

    // guava缓存的配置文件定时刷新时间
    public final static long GUAVA_REFRESH_DELAY = 10 * 60 * 1000;
    public final static long GUAVA_REFRESH_PERIOD = 10 * 60 * 1000;

    // 性别词
    public final static String[] MEN_SEX_WORDS = new String[]{"男士", "男式", "男"};
    public final static String[] WOMEN_SEX_WORDS = new String[]{"女士", "女式", "女"};

    /**
     * 逗号分隔符
     */
    public static final Joiner commaJoiner = Joiner.on(',').skipNulls();
    /**
     * 下划线分隔符
     */
    public static final Joiner underLineJoiner = Joiner.on('_').skipNulls();


    //性别
    public static final List<String> femaleList = new ArrayList<String>() {
        {
            add("女");
            add("女士");
            add("女性");
            add("女生");
            add("女人");
            add("女孩");
        }
    };

    public static final List<String> maleList = new ArrayList<String>() {
        {
            add("男");
            add("男性");
            add("男生");
            add("男人");
            add("男士");
            add("男孩");
        }
    };
    public static final String MALE = "男";
    public static final String FEMALE = "女";

    /**
     * 是否支持必要造物商品： 2：版本支持必要造物，但活动页不支持
     */
    public static final int IS_SUPPORT_CREATION_CON_2 = 2;

}
