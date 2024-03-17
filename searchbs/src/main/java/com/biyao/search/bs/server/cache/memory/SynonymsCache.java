package com.biyao.search.bs.server.cache.memory;

import com.biyao.search.bs.server.mysql.model.TermSynonymPO;
import com.biyao.search.bs.server.mysql.service.impl.TermSynonymServiceImpl;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/12/31 10:13
 * @description
 */
@Component
public class SynonymsCache {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    private TermSynonymServiceImpl termSynonymService;

    private Map<String, Set<String>> synonymsMap = new HashMap<>();

    @PostConstruct
    public void init(){
        log.error("加载同义词缓存...");
        refresh();
        log.error("加载同义词缓存结束");
    }

    public void refresh(){
        List<TermSynonymPO> temp;
        Map<String, Set<String>> tempMap = new HashMap<>();

        try{
            temp = termSynonymService.getAllInfos();
            if(temp.size() > 0){
                for (TermSynonymPO item : temp) {
                    Set<String> set = convert2List(item.getSynonym());
                    if(set.size()>0){
                        tempMap.put(item.getTerm(),set);
                    }
                }
                if(tempMap.size() > 0){
                    synonymsMap = tempMap;
                }
            }

        }catch(Exception e){
            log.error("[严重异常]加载同义词缓存异常："+e.getMessage());
        }

    }

    /**
     * 格式转换
     * @param synonym
     * @return
     */
    private Set<String> convert2List(String synonym) {
        Set<String> result = new HashSet<>();
        String[] array = synonym.split(",");
        if(array.length > 0){
            result.addAll(Arrays.asList(array));
        }
        return result;
    }

    /**
     * 根据term获取同义词
     * @param item
     * @return
     */
    public List<String> getSynonymsList(String item){
        List<String> result = new ArrayList<>();

        if(StringUtils.isBlank(item)){
            return result;
        }
        Set<String> temp = synonymsMap.get(item);
        if(temp != null){
            temp.add(item);
        }
        return temp == null ? new ArrayList<String>() {{ add(item); }}:new ArrayList<>(temp);
    }

}
