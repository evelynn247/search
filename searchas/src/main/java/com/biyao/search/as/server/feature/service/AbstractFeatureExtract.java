package com.biyao.search.as.server.feature.service;

import com.alibaba.fastjson.JSON;
import com.biyao.search.as.server.feature.consts.FeatureConsts;
import com.biyao.search.as.server.feature.manager.FeatureExtractManager;
import com.biyao.search.as.server.feature.model.*;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeature;
import com.biyao.search.common.model.SearchItem;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/18 10:53
 * @description
 */
public abstract class AbstractFeatureExtract {

    private Logger logger = LoggerFactory.getLogger(getClass());

    List<FeatureParseConf> featureConfList;

    private DecimalFormat DF = new DecimalFormat("#.0000");

    /**
     * 初始化特征配置文件
     * @return 特征配置解析文件
     */
    protected abstract List<FeatureParseConf> buildFeatureConfList();

    /**
     * 刷新
     */
    public final void refresh(){
        try{
            featureConfList = buildFeatureConfList();
            if (featureConfList == null || featureConfList.size() == 0) {
                logger.info("[操作日志]特征配置列表为空,url：{}", FeatureExtractManager.FEATURE_CONF_URL_PATH);
            }
        }
        catch(Exception e){
            logger.error("[严重异常]特征配置加载异常："+e.getMessage());
        }

    }

    /**
     * 特征抽取主方法
     */
    public final  ByBaseFeature extract(SearchItem searchItem, ProductFeature productFeature, QueryFeature queryFeature, UserFeature userFeature, ContextFeature contextFeature) {

        ByBaseFeature byBaseFeature = new ByBaseFeature();
        for (FeatureParseConf featureConf : featureConfList) {
            String key = featureConf.getFeatureName();
            String value;
            switch (featureConf.getFunctionName()) {
                case FeatureConsts.ES_SCORE:
                    value = esScore(featureConf, searchItem);
                    break;
                case FeatureConsts.GET:
                    value = get(featureConf, productFeature, queryFeature, userFeature, contextFeature);
                    break;
                case FeatureConsts.EQUAL:
                    value = equal(featureConf, byBaseFeature);
                    break;
                case FeatureConsts.VALUEOF:
                    value = valueOf(featureConf, byBaseFeature);
                    break;
                case FeatureConsts.ISIN:
                    value = isIn(featureConf, byBaseFeature);
                    break;
                case FeatureConsts.LOG2:
                    value = log2(featureConf, byBaseFeature);
                    break;
                case FeatureConsts.LOG10:
                    value = log10(featureConf, byBaseFeature);
                    break;
                default:
                    value = featureConf.getDefaultValue();
            }
            byBaseFeature.put(key, value);
        }
        return byBaseFeature;
    }

    /**
     * 取log10对数
     * @param featureConf
     * @param byBaseFeature
     * @return
     */
    private String log10(FeatureParseConf featureConf, ByBaseFeature byBaseFeature) {
        String result = featureConf.getDefaultValue();
        try{
            double valueA = Double.parseDouble(byBaseFeature.get(featureConf.getParamA()));
            if (valueA <= 1.0){
                return "0.0";
            }
            double log = Math.log10(valueA);
            return DF.format(log);

        }catch(Exception e){
            logger.error("log10取值异常："+e.getMessage());
        }
        return result;
    }

    /**
     * 取log2对数
     * @param featureConf
     * @param byBaseFeature
     * @return
     */
    private String log2(FeatureParseConf featureConf, ByBaseFeature byBaseFeature) {
        String result = featureConf.getDefaultValue();
        try{
            double valueA = Double.parseDouble(byBaseFeature.get(featureConf.getParamA()));
            if (valueA <= 1.0){
                return "0.0";
            }
            double log = Math.log(valueA)/Math.log(2.0);
            return DF.format(log);

        }catch(Exception e){
            logger.error("log2取值异常："+e.getMessage());
        }
        return result;
    }

    /**
     *  valueB数据格式为key1:value1,key1:value1，paramA为key。
     *  返回valueB的key中是否有valueA。0:不包含 1:包含
     * @param featureConf
     * @param byBaseFeature
     * @return
     */
    private String isIn(FeatureParseConf featureConf, ByBaseFeature byBaseFeature) {
        String result = featureConf.getDefaultValue();
        try{
            String paramA = byBaseFeature.get(featureConf.getParamA());
            String paramB = byBaseFeature.get(featureConf.getParamB());
            Set<String> keySet = new HashSet<>();
            Arrays.asList(paramB.split(",")).forEach(item->{
                String[] array = item.split(":");
                /*商议固定格式*/
                if (array.length ==1 || array.length ==2) {
                    keySet.add(array[0]);
                }
            });
           if(keySet.size()>0){
               if(keySet.contains(paramA)){
                   return "1";
               }else{
                   return "0";
               }

           }
        }catch(Exception e){
            logger.error("isIn取值异常："+e.getMessage());
        }
        return result;
    }

    /**
     *  valueB数据格式为key1:value1,key1:value1，valueA为key。
     *  返回valueB中valueA为key对应的value
     * @param featureConf
     * @param byBaseFeature
     * @return
     */
    private String valueOf(FeatureParseConf featureConf, ByBaseFeature byBaseFeature) {
        String result = featureConf.getDefaultValue();
        try{
            String paramA = byBaseFeature.get(featureConf.getParamA());
            String paramB = byBaseFeature.get(featureConf.getParamB());
            Map<String,String> paramBMap = new HashMap<>();
            Arrays.asList(paramB.split(",")).forEach(item->{
                String[] array = item.split(":");
                /*商议固定格式*/
                if (array.length == 2) {
                    String key = array[0];
                    String value = array[1];
                    paramBMap.put(key, value);
                }
            });
            String temp = paramBMap.get(paramA);
            if(StringUtils.isNotBlank(temp)){
                result = temp;
            }
        }catch(Exception e){
            logger.error("valueOf取值异常："+e.getMessage());
        }
        return result;
    }

    /**
     * 返回valueA和valueB是否相等。0:不相等 1:相等
     * @param featureConf
     * @param byBaseFeature
     * @return
     */
    private String equal(FeatureParseConf featureConf, ByBaseFeature byBaseFeature) {

        String paramA = byBaseFeature.get(featureConf.getParamA());
        String paramB = byBaseFeature.get(featureConf.getParamB());
        if(StringUtils.isBlank(paramA) || StringUtils.isBlank(paramB)){
            return featureConf.getDefaultValue();
        }
        if(paramA.equals(paramB)){
            return "1";
        }else{
            return "0";
        }
    }

    /**
     * 获取es匹配分
     * @param featureConf
     * @param searchItem
     * @return
     */
    private String esScore(FeatureParseConf featureConf, SearchItem searchItem) {
        String result = searchItem.getMatchScore().toString();
        if(StringUtils.isBlank(result)){
            result = featureConf.getDefaultValue();
        }
        return result;
    }

    /**
     *  根据特征名称，从特征Map中获取到相应的特征值。约定：
     *  p_开头的从productFeature中获取
     *  q_开头的从queryFeature中获取
     *  u_和uu_开头的从userFeature中获取
     *  c_开头的从contextFeature中获取
     * @param featureConf
     * @param productFeature
     * @param queryFeature
     * @param userFeature
     * @param contextFeature
     * @return
     */
    private String get(FeatureParseConf featureConf, ProductFeature productFeature, QueryFeature queryFeature, UserFeature userFeature, ContextFeature contextFeature) {
        String result = featureConf.getDefaultValue();
        try{
            String param = featureConf.getParamA();
            if(param.startsWith("p_")){
                if(productFeature!=null){
                    String temp = productFeature.getFeatures().get(featureConf.getFeatureName());
                    if(StringUtils.isNotBlank(temp)){
                        result = temp;
                    }
                }
            }else if(param.startsWith("q_")){
                if(queryFeature!=null){
                    String temp = queryFeature.getFeatures().get(featureConf.getFeatureName());
                    if(StringUtils.isNotBlank(temp)){
                        result = temp;
                    }
                }
            }else if(param.startsWith("u_") || param.startsWith("uu_")){
                if(userFeature!=null){
                    String temp = userFeature.getFeatures().get(featureConf.getFeatureName());
                    if(StringUtils.isNotBlank(temp)){
                        result = temp;
                    }
                }
            }else if(param.startsWith("c_")){
                if(contextFeature!=null){
                    String temp = contextFeature.getFeatures().get(featureConf.getFeatureName());
                    if(StringUtils.isNotBlank(temp)){
                        result = temp;
                    }
                }
            }
        }catch(Exception e){
            logger.error("get取值异常："+e.getMessage());
        }

        return result;
    }

    /**
     * 解析函数名
     * @param s
     * @return
     */
    protected String getFunctionName(String s) {
        String result = s.substring(0,s.indexOf('('));
        if(StringUtils.isNotBlank(result)){
            return result;
        }
        else{
            return s;
        }
    }

    /**
     * 解析特征
     * @param conf
     * @param s
     */
    protected void parseFormulaParams(FeatureParseConf conf, String s) {

        String params = s.substring(s.indexOf('(') + 1, s.length() - 1);
        if(s.startsWith(FeatureConsts.GET) || s.startsWith(FeatureConsts.LOG2) ||s.startsWith(FeatureConsts.LOG10)) {
            conf.setParamA(params);
        }else if(s.startsWith(FeatureConsts.VALUEOF) || s.startsWith(FeatureConsts.EQUAL) || s.startsWith(FeatureConsts.ISIN)) {
            int indexOfComma = params.indexOf(',');
            conf.setParamA(params.substring(0, indexOfComma).trim());
            conf.setParamB(params.substring(indexOfComma + 1).trim());
        }else {
            return;
        }
    }
}
