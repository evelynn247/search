package com.biyao.search.bs.server.cache.guava.detail;

import com.alibaba.fastjson.JSONObject;
import com.biyao.search.bs.server.cache.guava.BaseGuavaCache;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.common.model.FacetItem;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class ProductWordFacetCache extends BaseGuavaCache<String, Map<String, List<FacetItem>>> {

	private Logger logger = LoggerFactory.getLogger(ProductWordFacetCache.class);
	private final static String KEY = "search:product_word_facet";

	@Override
	public void loadValueWhenStarted() {
		this.setMaxSize(10000);
		this.setRefreshDuration(1);
		this.setRefreshTimeUnit(TimeUnit.DAYS);
	}

	@Override
	protected Map<String, List<FacetItem>> getValueWhenExpired(String key) throws Exception {
		Map<String, List<FacetItem>> result = new HashMap<>();
		try {
			File file = new File(CommonConsts.PRODUCT_WORD_FACET_PATH);
			if (file.exists()) {
				try (
						FileReader fileReader = new FileReader(file);
						BufferedReader bufferedReader = new BufferedReader(fileReader);
				) {
					String line;
					while ((line = bufferedReader.readLine()) != null) {
						String l = line.trim();
						if (StringUtils.isBlank(l) || l.startsWith("#")) {
							continue;
						}

						String[] wordFacet = l.split(" ");
						if (wordFacet.length != 2){
							continue;
						}
						String productWord = wordFacet[0];
						try{
							JSONObject facetObj = JSONObject.parseObject(wordFacet[1]);
							List<FacetItem> facetItemList = new ArrayList<>();
							for (String facetKey : facetObj.keySet()){
								String facetValueStr = (String) facetObj.get(facetKey);
								if (StringUtils.isBlank(facetValueStr)){
									continue;
								}
								List<String> facetValue = Arrays.asList(facetValueStr.split(","));
								FacetItem facetItem = new FacetItem();
								facetItem.setKey(facetKey);
								facetItem.setValues(facetValue);
								facetItemList.add(facetItem);
							}
							result.put(productWord, facetItemList);
						}catch (Exception e){
							logger.error("产品词-facet行格式不正确: word={}", productWord);
						}
					}
				}
			} else {
				throw new Exception("产品词-facet配置文件不存在" + CommonConsts.PRODUCT_TAGS_PATH);
			}
		} catch (Exception e) {
			logger.error("产品词-facet内存缓存失败：{}", e);
			throw e;
		}

		return result;
	}

	/**
	 * 根据产品词获取facet
	 * @param productWord
	 * @return
	 */
	public List<FacetItem> getFacet(String productWord){
		Map<String, List<FacetItem>> productWordFacet = getValueOrDefault(KEY, new HashMap<>());
		return productWordFacet.getOrDefault(productWord, new ArrayList<>());
	}

	public void refreshValue() {
		refreshValue(KEY);
	}
}
