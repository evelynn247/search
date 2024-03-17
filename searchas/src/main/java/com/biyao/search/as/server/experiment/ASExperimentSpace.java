package com.biyao.search.as.server.experiment;

import javax.annotation.PostConstruct;

import com.biyao.experiment.ExperimentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.biyao.experiment.ExperimentSpace;
import com.biyao.experiment.ExperimentSpaceBuilder;

/**
 * @author
 * @date
 */
@Component
public class ASExperimentSpace {
    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 实验空间属性
     */
    private ExperimentSpace experimentSpace;

    /**
     * 实验层配置地址
     */
    @Value("${layer.conf.url}")
    private String LAYER_CONF_URL;
    /**
     * 实验配置地址
     */
    @Value("${exp.conf.url}")
    private String EXP_CONF_URL;


    @PostConstruct
    public void initExperimentSpace() {
        build(LAYER_CONF_URL, EXP_CONF_URL);
        logger.info("[操作日志]实验系统初始化完成");
    }

    public void refresh() {
        logger.info("[任务报告]10分钟刷新新实验配置开始，layer文件地址:{}，实验配置文件地址：{}", LAYER_CONF_URL, EXP_CONF_URL);
        build(LAYER_CONF_URL, EXP_CONF_URL);
        logger.info("[任务报告]10分钟刷新新实验配置结束，layer文件地址:{}，实验配置文件地址：{}", LAYER_CONF_URL, EXP_CONF_URL);
    }

    /**
     * @param layerConfUrl
     * @param expConfUrl
     */
    private void build(String layerConfUrl, String expConfUrl) {
        try {
            ExperimentSpace tempExperimentSpace = new ExperimentSpaceBuilder()
                    .setLayer(layerConfUrl)
                    .setExps(expConfUrl)
                    .build();
            this.experimentSpace = tempExperimentSpace;
        } catch (Exception e) {
            logger.error("[严重异常]实验空间构建失败: layerConfUrl={}, expConfUrl={}", layerConfUrl, expConfUrl, e);
        }
    }
    /**
     * 切分流量
     *
     * @param request
     */
    public void divert(ExperimentRequest request) {
        if (this.experimentSpace != null) {
            this.experimentSpace.divert(request);
        }
    }

    /**
     * 是否命中实验
     *
     * @param flagName
     * @param expFlagValue
     * @param request
     * @return
     */
    public boolean hitExp(String flagName, String expFlagValue, ExperimentRequest request) {
        if (expFlagValue == null || flagName == null || request == null) {
            return false;
        }

        if (expFlagValue.equals(request.getStringFlag(flagName))) {
            return true;
        }
        return false;
    }
}
