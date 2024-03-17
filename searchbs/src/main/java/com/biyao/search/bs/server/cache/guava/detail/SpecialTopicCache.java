package com.biyao.search.bs.server.cache.guava.detail;

import com.biyao.search.bs.server.bean.SpecialTopic;
import com.biyao.search.bs.server.bean.SpecialTopicConf;
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
 * 专题
 *
 * @author wangbo
 * @version 1.0 2018/6/11
 */
@Component
public class SpecialTopicCache extends BaseGuavaCache<String, SpecialTopicConf> {

    private Logger logger = LoggerFactory.getLogger(SpecialTopicCache.class);

    private final static String KEY = "search:special_topic";

    private final static int OPERATOR_TYPE = 1;
    private final static int BI_TYPE = 2;

    @Override
    @PostConstruct
    public void loadValueWhenStarted() {
        this.setMaxSize(10000);
        this.setRefreshDuration(1);
        this.setRefreshTimeUnit(TimeUnit.HOURS);
    }

    @Override
    protected SpecialTopicConf getValueWhenExpired(String key) throws Exception {
        SpecialTopicConf conf = new SpecialTopicConf();
        List<SpecialTopic> autoCreateConf = conf.getAutoCreateConf();
        List<SpecialTopic> operatorConf = conf.getOperatorConf();
        List<SpecialTopic> allConf = conf.getAllConf();
        Map<Integer, SpecialTopic> confMap = conf.getConfMap();
        try {
            File file = new File(CommonConsts.SPECIAL_TOPIC_PATH);
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
                            // zhaiweixi 20180711 配置项增加至9项
                            // topicId	type	topic(title)	productNum	imgUrl	products	pImgUrl	subTitle	summary
                            if (ls.length == 9) {
                                try {
                                    SpecialTopic st = new SpecialTopic();
                                    st.setId(Integer.parseInt(ls[0].trim()));
                                    st.setType(Integer.parseInt(ls[1].trim()));
                                    st.setTopic(ls[2].trim());
                                    st.setProductNum(Integer.parseInt(ls[3].trim()));
//                                    st.setUrl(ls[4].trim());
                                    // 主题图
                                    if (ls[4].trim().length() > 0){
                                        List<String> topicImageList = (Arrays.asList(ls[4].trim().split(",")));
                                        Collections.shuffle(topicImageList);
                                        st.setUrl(topicImageList.get(0));
                                    }
                                    // 商品列表
                                    if (ls[5].length() > 0) {
                                        for (String pid : ls[5].trim().split(","))
                                            st.getPids().add(Integer.parseInt(pid));
                                    }

                                    // 商品图
                                    if (ls[6].trim().length() > 0){
                                        st.setProductImageUrlList(Arrays.asList(ls[6].trim().split(",")));
                                    }
                                    // 副标题
                                    st.setSubTitle(ls[7].trim());
                                    // 摘要
                                    st.setSummary(ls[8].trim());

                                    if (st.getType() == OPERATOR_TYPE) {
                                        operatorConf.add(st);
                                    } else if (st.getType() == BI_TYPE){
                                        autoCreateConf.add(st);
                                    }
                                    allConf.add(st);

                                    confMap.put(st.getId(), st);
                                } catch (Exception e) {
                                    logger.error("解析专题配置文件异常:" + l, e);
                                }
                            }
                        }
                    }
                }
            } else {
                throw new Exception("专题配置文件不存在" + CommonConsts.SPECIAL_TOPIC_PATH);
            }
        } catch (Exception e) {
            logger.error("专题配置文件失败：{}", e);
            throw e;
        }

        return conf;
    }

    public List<SpecialTopic> getTopicList(int type, int size) {
        List<SpecialTopic> res = new ArrayList<>();
        try {
            SpecialTopicConf conf = getValue(KEY);
            if (null == conf)
                return res;

            /*List<SpecialTopic> middle = type == OPERATOR_TYPE ? conf.getOperatorConf() : conf.getAutoCreateConf();
            if (middle.size() <= size || size < 0)
                return middle;

            for (int i = 0; i < size; i++)
                res.add(middle.get(i));*/
            
            /*
             *   modify by luozhuo 20180720 
             *   上面部分代码有问题，导致不支持其他类型的topic，先修改成如下方式，后续再优化
             */
            List<SpecialTopic> allConf = conf.getAllConf();
            for (SpecialTopic topic : allConf) {
            	if (res.size() >= size) {
            		break;
            	}
            	
            	if (topic.getType() != type) {
            		continue;
            	}
            	
            	res.add(topic);
            }
            
        } catch (Exception e) {
            logger.error("SpecialTopicCache getTopicList error", e);
        }

        return res;
    }

    public SpecialTopic getDataByTopicId(int topicId) {
        try {
            SpecialTopicConf conf = getValue(KEY);
            if (null == conf)
                return null;

            return conf.getConfMap().get(topicId);
        } catch (Exception e) {
            logger.error("SpecialTopicCache getTopicList error", e);
        }

        return null;
    }


    public void refreshValue() {
        refreshValue(KEY);
    }

    /**
     * 获取全部Map
     * @return
     */
    public Map<Integer, SpecialTopic> getSpecialTopicMap(){
        try {
            SpecialTopicConf conf = getValue(KEY);
            if (null == conf)
                return null;

            return conf.getConfMap();
        } catch (Exception e) {
            logger.error("SpecialTopicCache getSpecialTopicMap error", e);
        }

        return null;
    }
}
