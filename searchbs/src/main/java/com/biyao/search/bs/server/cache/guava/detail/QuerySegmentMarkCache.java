package com.biyao.search.bs.server.cache.guava.detail;

import com.biyao.search.bs.server.cache.guava.BaseGuavaCache;
import com.biyao.search.bs.server.cache.redis.RedisUtil;
import com.biyao.search.bs.server.common.consts.CommonConsts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Component
public class QuerySegmentMarkCache extends BaseGuavaCache<String, Set<String>> {

	@Autowired
	RedisUtil redisCache;

	private Logger logger = LoggerFactory.getLogger(QuerySegmentMarkCache.class);

	private final String PRODUCT_KEY = "product";
	private final String BRAND_KEY = "brand";
	private final String ATTRIBUTE_KEY = "attribute";
	private final String FEATURE_KEY = "feature";

	@Override
	@PostConstruct
	public void loadValueWhenStarted() {
		this.setRefreshDuration(-1);
		this.setRefreshTimeUnit(TimeUnit.MINUTES);
	}

	@Override
	protected Set<String> getValueWhenExpired(String key) throws Exception {
		Set<String> result = new HashSet<>();
		try {
			String fileName = "";
			switch (key){
				case PRODUCT_KEY:
					fileName = CommonConsts.PRODUCT_DIC_PATH;
					break;
				case BRAND_KEY:
					fileName = CommonConsts.BRAND_DIC_PATH;
					break;
				case ATTRIBUTE_KEY:
					fileName = CommonConsts.ATTRIBUTE_DIC_PATH;
					break;
				case FEATURE_KEY:
					fileName = CommonConsts.FEATURE_DIC_PATH;
					break;
				default:
					fileName = "";
					break;
			}
			File file = new File(fileName);
			if (file.exists()){
				try(
					FileReader fileReader = new FileReader(fileName);
					BufferedReader bufferedReader = new BufferedReader(fileReader);
				) {
					String term;
					while ((term = bufferedReader.readLine()) != null) {
						if (!"".equals(term.trim()) && !term.trim().startsWith("#")) {
							result.add(term.trim());
						}
					}
				}
//				bufferedReader.close();
//				fileReader.close();
			}else{
				throw new Exception("字典文件不存在" + fileName);
			}

		}catch (Exception e){
			logger.error("获取分词类型失败{}：{}", key, e);
			// 从缓存中删除
			throw e;
		}
		return result;
	}

	// 获取产品词分词集合
	public Set<String> getProductTerm(){
		return getValueOrDefault(PRODUCT_KEY, new HashSet<>());
	}

	// 判断分词是否属于是产品词
	public boolean isProductTerm(String term){
		if (getProductTerm().contains(term)){
			return true;
		}else {
			return false;
		}
	}

	// 获取品牌词词分词集合
	public Set<String> getBrandTerm(){
		return getValueOrDefault(BRAND_KEY, new HashSet<>());
	}

	// 判断分词是否属于是品牌词
	public boolean isBrandTerm(String term){
		if (getBrandTerm().contains(term)){
			return true;
		}else {
			return false;
		}
	}

	// 获取属性词分词集合
	public Set<String> getAttributeTerm(){
		return getValueOrDefault(ATTRIBUTE_KEY, new HashSet<>());
	}

	// 判断分词是否属于是属性词
	public boolean isAttributeTerm(String term){
		if (getAttributeTerm().contains(term)){
			return true;
		}else {
			return false;
		}
	}

	// 获取功能词分词集合
	public Set<String> getFeatureTerm(){
		return getValueOrDefault(FEATURE_KEY, new HashSet<>());
	}

	// 判断分词是否属于是功能词
	public boolean isFeatureTerm(String term){
		if (getFeatureTerm().contains(term)){
			return true;
		}else {
			return false;
		}
	}

	public void refreshAll(){
		refreshValue(PRODUCT_KEY);
		refreshValue(BRAND_KEY);
		refreshValue(ATTRIBUTE_KEY);
		refreshValue(FEATURE_KEY);
	}
}
