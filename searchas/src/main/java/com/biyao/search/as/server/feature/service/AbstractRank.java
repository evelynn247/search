package com.biyao.search.as.server.feature.service;

import ciir.umass.edu.learning.DenseDataPoint;
import ciir.umass.edu.learning.Ranker;
import com.biyao.search.as.server.bean.KeyValue;
import com.biyao.search.as.server.bean.RankItem;
import com.biyao.search.as.server.feature.model.ByBaseFeature;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeature;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.common.model.SearchItem;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author: xiafang
 * @date: 2019/11/15
 */
@Slf4j
public abstract class AbstractRank {
    /**
     * key是特征Index，value是特征名称
     */
    protected Map<String, Integer> featureMap;

    /**
     * 特征List
     */
    protected List<KeyValue<String, Integer>> featureMapList;
    /**
     * RankLib包中的Rank类
     */
    protected Ranker ranker;


    /**
     * 刷新featureMapList featureMap和ranker
     */
    public void refresh(){
        try {
            Map<String, Integer> tempFeatureMap = buildFeatureMap();
            Ranker tempRanker = buildRanker();
            List<KeyValue<String, Integer>> tempFeatureMapList = new ArrayList<>();
            if (tempFeatureMap.size() > 0) {
                for (String key : tempFeatureMap.keySet()) {
                    KeyValue<String, Integer> keyValue = new KeyValue<>();
                    keyValue.setKey(key);
                    keyValue.setValue(tempFeatureMap.get(key));
                    tempFeatureMapList.add(keyValue);
                }
                tempFeatureMapList.sort(Comparator.comparing(KeyValue::getValue));

            }
            if (tempRanker != null && tempFeatureMapList.size() > 0) {
                this.ranker = tempRanker;
                this.featureMap = tempFeatureMap;
                this.featureMapList = tempFeatureMapList;
            }
        }catch (Exception e){
            log.error("[严重异常]排序特征或者模型更新失败,异常信息:", e);
        }
    }

    /**
     * 模型特征配置【lambdaMartFeatureMap.txt】，即算法提供的训练样本中使用到的特征配置文件
     * 1.加载配置文件
     * 2.解析到featureMap中
     * @return 特征ID -> 特征index
     */
    protected abstract Map<String, Integer> buildFeatureMap();

    /**
     * 通过配置文件LambdaMART.txt加载排序模型的抽象方法，加载模型的实现在子类实现
     * @return rankLib implement
     */
    protected abstract Ranker buildRanker();

    /**
     * 新的排序模型在工程中的排序实现
     *
     * @param searchItemList bs召回结果
     * @param query
     * @return
     */
    public abstract List<SearchItem> sort(List<SearchItem> searchItemList, String query, SearchRequest request, int group);

    /**
     * 把特征数据组织成排序模型要求的格式
     * @param byBaseFeature pid下全量特征集合
     * @return 将排序模型要求的特征组织成调用RankLib包要求的数据格式
     */
    protected final DenseDataPoint buildDenseDataPointText(ByBaseFeature byBaseFeature, RankItem rankItem) {
        StringBuilder text = new StringBuilder("0 qid:0");
        StringBuilder desc = new StringBuilder();
        featureMapList.forEach(kv->{
            //todo 判断byBaseFeature.getOrDefault(kv.getKey(), "0.0"))是否是数字，非数字不做parseFloat转换
            text.append(String.format(" %d:%f", kv.getValue(), Float.parseFloat(byBaseFeature.getOrDefault(kv.getKey(), "0.0"))));
            desc.append(kv.getKey()).append(" ");
        });

        text.append(" #排序特征:").append(desc.toString());
        if (ThreadLocalFeature.IS_WHITE_LIST_UUID.get()) {
            rankItem.setFeaturesUsedInModel(text.toString());
        }
        return new DenseDataPoint(text.toString());
    }
}
