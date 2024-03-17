package com.biyao.search.bs.server.experiment;

import com.biyao.experiment.ExperimentRequest;
import com.biyao.experiment.ExperimentSpace;
import com.biyao.experiment.ExperimentSpaceBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 实验空间
 *
 * @author biyao
 * @date long long ago
 */
@Component
public class BSExperimentSpace {
    private Logger logger = LoggerFactory.getLogger(BSExperimentSpace.class);

    /**
     * 实验空间属性
     */
    private ExperimentSpace experimentSpace;

    /**
     * 实验层配置地址
     */
    private final static String LAYER_CONF_URL = "http://conf.nova.biyao.com/nova/searchbslayer.conf";
    /**
     * 实验配置地址
     */
    private final static String EXP_CONF_URL = "http://conf.nova.biyao.com/nova/searchbsexp.conf";

    @PostConstruct
    public void initExperimentSpace() {
        build(LAYER_CONF_URL, EXP_CONF_URL);
        logger.error("实验空间初始化完成");
    }

    /**
     * 每10分钟刷新一次实验
     */
    @Scheduled(cron = "12 0/10 * * * ?")
    public void refresh(){
        logger.error("刷新新实验配置开始");
        build(LAYER_CONF_URL, EXP_CONF_URL);
        logger.error("刷新新实验配置结束");
    }

    /**
     * @param layerConfUrl
     * @param expConfUrl
     */
    private void build(String layerConfUrl, String expConfUrl){
        try {
            ExperimentSpace tempExperimentSpace = new ExperimentSpaceBuilder()
                    .setLayer(layerConfUrl)
                    .setExps(expConfUrl)
                    .build();
            this.experimentSpace = tempExperimentSpace;
        }catch (Exception e){
            logger.error("实验空间构建失败: layerConfUrl={}, expConfUrl={}", layerConfUrl, expConfUrl, e);
        }
    }

    /**
     * 获取实验空间
     * @return
     */
    public ExperimentSpace getExperimentSpace() {
        return experimentSpace;
    }

    /**
     * 切分流量
     * @param request
     */
    public void divert(ExperimentRequest request){
        if (this.experimentSpace != null){
            this.experimentSpace.divert(request);
        }
    }

    /**
     * 是否命中实验
     * @param flagName
     * @param expFlagValue
     * @param request
     * @return
     */
    public boolean hitExp(String flagName, String expFlagValue, ExperimentRequest request){
        if (expFlagValue == null || flagName == null || request == null){
            return false;
        }

        if (expFlagValue.equals(request.getStringFlag(flagName))){
            return true;
        }

        return false;
    }
}
