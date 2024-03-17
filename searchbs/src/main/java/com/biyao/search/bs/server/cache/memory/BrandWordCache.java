package com.biyao.search.bs.server.cache.memory;

import com.alibaba.fastjson.JSONArray;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.bs.server.common.util.FileUtil;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

import com.biyao.search.bs.server.mysql.model.BrandWordPO;
import com.biyao.search.bs.server.mysql.service.impl.BrandWordServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.PostConstruct;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

/**
* @Description 品牌词缓存
* @date 2019年8月23日下午7:54:23
* @version V1.0 
* @author 邹立强 (zouliqiang@idstaff.com)
* <p>Copyright (c) Department of Research and Development/Beijing.</p>
 */
@Component
@EnableScheduling
public class BrandWordCache {
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private BrandWordServiceImpl brandWordService;

	/**
	 * 品牌词 -> 改写词 映射
 	 */
    private Map<String, List<String>> brandWordMap;


    @PostConstruct
    protected void init(){
		log.error("加载品牌词缓存...");
        refresh();
		log.error("加载品牌词缓存结束");
    }

	public void refresh() {
		List<BrandWordPO> temp;
		Map<String, List<String>> tempMap = new HashMap<>();

		try{
			temp = brandWordService.getAllInfos();
			if(temp.size() > 0){
				for (BrandWordPO item : temp) {
					List<String> list = convert2List(item.getRewriteWord());
					if(list.size()>0){
						tempMap.put(item.getBrandWord(),list);
					}
				}
				if(tempMap.size() > 0){
					brandWordMap = tempMap;
				}
			}

		}catch(Exception e){
			log.error("[严重异常]加载品牌词缓存异常："+e.getMessage());
		}
	}

	/**
	 * 格式转换
	 * @param rewriterWord
	 * @return
	 */
	private List<String> convert2List(String rewriterWord) {
		List<String> result = new ArrayList<>();
		String[] array = rewriterWord.split(",");
		if(array.length > 0){
			result=Arrays.asList(array);
		}
		return result;
	}

	/**
	* @Description 通过查询词获取查询结果 
	* @param query
	* @return List<String> 
	* @version V1.0
	* @auth 邹立强 (zouliqiang@idstaff.com)
	 */
    public List<String> getBrandWordList(String query){
        if (brandWordMap == null || brandWordMap.size() == 0){
            return new ArrayList<>();
        }
        List<String> list=null;
        if(StringUtils.isNotBlank(query)) {
        	list = brandWordMap.get(query.toLowerCase());
        }else {
        	list=new ArrayList<>();
        }
        return list;
    }
}
