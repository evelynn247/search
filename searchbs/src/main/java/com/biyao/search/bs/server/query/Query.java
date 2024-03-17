package com.biyao.search.bs.server.query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Query {
	// 搜索词
	private String query;
	// 搜索词性别标签
	private Integer sexLabel = 0;
	// 产品词分词
	private List<String> productTerms = new ArrayList<>();
	// 品牌词分词
	private List<String> brandTerms = new ArrayList<>();
	// 属性词分词
	private List<String> attributeTerms = new ArrayList<>();
	// 功能词分词
	private List<String> featureTerms = new ArrayList<>();
	// 其他分词
	private List<String> otherTerms = new ArrayList<>();
	// 被改写的分词
	private Map<String, List<String>> rewriteTerms = new HashMap<>();

	public Query(String query){
		this.query = query;
//		this.sexLabel = 0;
//		this.productTerms = new ArrayList<>();
//		this.brandTerms = new ArrayList<>();
//		this.attributeTerms = new ArrayList<>();
//		this.featureTerms = new ArrayList<>();
//		this.otherTerms = new ArrayList<>();
	}

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public Integer getSexLabel() {
		return sexLabel;
	}

	public void setSexLabel(Integer sexLabel) {
		this.sexLabel = sexLabel;
	}

	public List<String> getProductTerms() {
		return productTerms;
	}

	public void setProductTerms(List<String> productTerms) {
		this.productTerms = productTerms;
	}

	public List<String> getBrandTerms() {
		return brandTerms;
	}

	public void setBrandTerms(List<String> brandTerms) {
		this.brandTerms = brandTerms;
	}

	public List<String> getAttributeTerms() {
		return attributeTerms;
	}

	public void setAttributeTerms(List<String> attributeTerms) {
		this.attributeTerms = attributeTerms;
	}

	public List<String> getFeatureTerms() {
		return featureTerms;
	}

	public void setFeatureTerms(List<String> featureTerms) {
		this.featureTerms = featureTerms;
	}

	public List<String> getOtherTerms() {
		return otherTerms;
	}

	public void setOtherTerms(List<String> otherTerms) {
		this.otherTerms = otherTerms;
	}

	public void addProductTerm(String term){
		if (this.productTerms == null){
			this.productTerms = new ArrayList<>();
		}
		this.productTerms.add(term);
	}

	public void addBrandTerm(String term){
		if (this.brandTerms == null){
			this.brandTerms = new ArrayList<>();
		}
		this.brandTerms.add(term);
	}

	public void addAttributeTerm(String term){
		if (this.attributeTerms == null){
			this.attributeTerms = new ArrayList<>();
		}
		this.attributeTerms.add(term);
	}

	public void addFeatureTerm(String term){
		if (this.featureTerms == null){
			this.featureTerms = new ArrayList<>();
		}
		this.featureTerms.add(term);
	}

	public void addOtherTerm(String term){
		if (this.otherTerms == null){
			this.otherTerms = new ArrayList<>();
		}
		this.otherTerms.add(term);
	}

	public Map<String, List<String>> getRewriteTerms() {
		return rewriteTerms;
	}

	public void setRewriteTerms(Map<String, List<String>> rewriteTerms) {
		this.rewriteTerms = rewriteTerms;
	}

	public void putRewriteTerms(String key, List<String> terms){
		this.rewriteTerms.put(key, terms);
	}

	public List<String> getAllTerms(){
		List<String> result = new ArrayList<>();
		result.addAll(this.productTerms.stream().filter(t -> !"".equals(t)).collect(Collectors.toList()));
		result.addAll(this.brandTerms.stream().filter(t -> !"".equals(t)).collect(Collectors.toList()));
		result.addAll(this.attributeTerms.stream().filter(t -> !"".equals(t)).collect(Collectors.toList()));
		result.addAll(this.featureTerms.stream().filter(t -> !"".equals(t)).collect(Collectors.toList()));
		result.addAll(this.otherTerms.stream().filter(t -> !"".equals(t)).collect(Collectors.toList()));

		return result;
	}
}
