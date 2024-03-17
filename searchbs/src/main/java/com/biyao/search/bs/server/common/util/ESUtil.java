package com.biyao.search.bs.server.common.util;

import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.cluster.metadata.AliasMetaData;
import org.elasticsearch.common.collect.ImmutableOpenMap;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2019/10/9 13:56
 * @description ES工具类
 */
public class ESUtil {

    /**
     * 待替换
     * @return
     */
    public static String getESIndexName(){
        //根据索引别名获取索引名称
        ImmutableOpenMap<String, List<AliasMetaData>> map = ESClientConfig.getESClient().admin().indices().prepareGetAliases(ElasticSearchConsts.BY_MALL_ALIAS).get().getAliases();
        List<String> allIndices = new ArrayList<>();
        map.keysIt().forEachRemaining(allIndices::add);
        if(allIndices.size()>0){
            return allIndices.get(0);
        }
        return "";
    }

    /**
     * 【商品管理V1.0-支持渠道专属商品】
     * 必要主站、分销过滤不支持当前渠道的商品
     * 2022-04-07
     * @param boolMustQuery
     * @param productPool 空表示必要主站搜索 1表示必要分销搜索
     */
    public static void buildSupportChannel(BoolQueryBuilder boolMustQuery, String productPool){
        if(StringUtils.isEmpty(productPool)){
            //过滤不支持必要主站渠道的商品
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.SUPPORT_CHANNEL_CONS,ElasticSearchConsts.SUPPORT_CHANNEL_CONS_BYZZ));
        }
        if("1".equals(productPool)){
            //过滤不支持必要分销的商品
            boolMustQuery.must(QueryBuilders.termQuery(ElasticSearchConsts.SUPPORT_CHANNEL_CONS,ElasticSearchConsts.SUPPORT_CHANNEL_CONS_BYFX));
        }
    }
}
