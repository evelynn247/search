package com.biyao.search.bs.server.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * 专题
 *
 * @author wangbo
 * @version 1.0 2018/6/11
 */
public class SpecialTopic {

    private int id;
    private int type;
    private String topic;
    private int productNum;
    private String url;
    private List<Integer> pids = new ArrayList<>();
    // zhaiweixi 20180711 增加主题配置内容
    /**
     * 商品图
     */
    private List<String> productImageUrlList = new ArrayList<>();
    /**
     * 副标题
     */
    private String subTitle;
    /**
     * 摘要
     */
    private String summary;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getProductNum() {
        return productNum;
    }

    public void setProductNum(int productNum) {
        this.productNum = productNum;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<Integer> getPids() {
        return pids;
    }

    public void setPids(List<Integer> pids) {
        this.pids = pids;
    }

    public List<String> getProductImageUrlList() {
        return productImageUrlList;
    }

    public void setProductImageUrlList(List<String> productImageUrlList) {
        this.productImageUrlList = productImageUrlList;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }
}
