package com.biyao.search.as.server.feature.manager;


import com.biyao.search.as.server.cache.redis.RedisUtil;
import com.biyao.search.as.server.common.util.FileUtil;
import com.biyao.search.as.server.feature.model.FeatureParseConf;
import com.biyao.search.as.server.feature.service.AbstractFeatureExtract;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/18 11:37
 * @description
 */
@Component("featureExtractManager")
public class FeatureExtractManager extends AbstractFeatureExtract {

    private Logger logger = LoggerFactory.getLogger(getClass());

    public static String FEATURE_CONF_URL_PATH;

    @Value("${feature.conf.url.path}")
    public void setFeatureConfUrlPath(String featureConfUrlPath) {
        FEATURE_CONF_URL_PATH = featureConfUrlPath;
    }

    @Autowired
    RedisUtil redisUtil;

    @PostConstruct
    private void init(){
        super.refresh();
    }

    @Override
    protected List<FeatureParseConf> buildFeatureConfList() {

        List<FeatureParseConf> tempConfigList = new ArrayList<>();
        try{
            List<String> lines = new ArrayList<>();
            try{
                //加载线上特征配置文件
                lines = FileUtil.getRemoteFile(FEATURE_CONF_URL_PATH);
            }
            catch(Exception e){
                logger.error("[严重异常][文件异常]读取线上特征配置文件失败，url:{},异常信息：{}", FEATURE_CONF_URL_PATH, e.getMessage());
            }
            //容错
            if(lines == null || lines.size()==0) {
                return tempConfigList;
            }

            Splitter splitter = Splitter.on(":").trimResults();
            for(String line: lines ) {
                if(Strings.isNullOrEmpty(line)||"#".equals(line.substring(0,1))) {
                    continue;
                }

                List<String> items = splitter.splitToList(line);
                if(items.size() != 8 ){
                    logger.error("[一般异常][文件异常]解析在线解析特征配置行出错，url:{},出错数据位置：{} ", FEATURE_CONF_URL_PATH, line);
                    continue;
                }

                FeatureParseConf conf = new FeatureParseConf();
                conf.setFeatureName(items.get(0));
                conf.setFunctionName(getFunctionName(items.get(4)));
                conf.setDefaultValue(items.get(6));
                parseFormulaParams(conf,items.get(4));

                tempConfigList.add(conf);
            }
            logger.info("[操作日志]读取特征配置文件完成，特征配置条数："+tempConfigList.size());
        }catch(Exception e){
            logger.error("[严重异常]读取特征配置文件异常，url:{},异常信息：{}", FEATURE_CONF_URL_PATH, e.getMessage());
        }

        return tempConfigList;
    }

}
