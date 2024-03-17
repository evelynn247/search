package com.biyao.search.bs.server.cache.guava.detail;

import com.biyao.search.bs.server.cache.guava.BaseGuavaCache;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 标签配置文件缓存
 *
 * @author wangbo
 * @version 1.0 2018/5/25
 */
@Component
public class ProductTagsCache extends BaseGuavaCache<String, Set<String>> {

    private Logger logger = LoggerFactory.getLogger(ProductTagsCache.class);

    private final static String KEY = "search:product_tags";

    @Override
    @PostConstruct
    public void loadValueWhenStarted() {
        this.setMaxSize(5000);
        this.setRefreshDuration(5);
        this.setRefreshTimeUnit(TimeUnit.MINUTES);
    }

    @Override
    protected Set<String> getValueWhenExpired(String key) throws Exception {
        Set<String> result = new HashSet<>();
        try {
            File file = new File(CommonConsts.PRODUCT_TAGS_PATH);
            if (file.exists()) {
                try (
                        FileReader fileReader = new FileReader(file);
                        BufferedReader bufferedReader = new BufferedReader(fileReader);
                ) {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        String l = line.trim();
                        if (StringUtils.isNotBlank(l) && !l.startsWith("#")) {
                            result.add(l);
                        }
                    }
                }
            } else {
                throw new Exception("商品标签配置文件不存在" + CommonConsts.PRODUCT_TAGS_PATH);
            }
        } catch (Exception e) {
            logger.error("商品标签配置文件失败：{}", e);
            throw e;
        }

        return result;
    }

    public void refreshValue() {
        refreshValue(KEY);
    }

    private Set<String> getProductTags(){
        return getValueOrDefault(KEY, new HashSet<>());
    }

    public boolean isTagProduct(String query){
        if (getProductTags().contains(query)){
            return true;
        }else {
            return false;
        }
    }

}
