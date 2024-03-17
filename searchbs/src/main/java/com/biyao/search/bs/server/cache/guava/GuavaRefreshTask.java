package com.biyao.search.bs.server.cache.guava;

import com.biyao.search.bs.server.cache.guava.detail.*;
import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.bs.server.common.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class GuavaRefreshTask {

	@Autowired
	private QuerySegmentMarkCache querySegmentMarkCache;
	@Autowired
	private ProductTagsCache productTagsCache;
	@Autowired
	private ProductModifyWordScoreCache productModifyWordScoreCache;
	@Autowired
	private SearchWordDataCache searchWordDataCache;
	@Autowired
	private SpecialTopicCache specialTopicCache;
	@Autowired
	private ProductWordFacetCache productWordFacetCache;

	private Logger logger = LoggerFactory.getLogger(getClass());

	@PostConstruct
	public void init(){
		// 启动时先刷新一次，然后设置定时任务
		refreshSegmentMark();
		logger.error("**********搜索词分词类型字典数据初始化完成*********");
        refreshProductTags();
        logger.error("**********商品标签配置重写配置文件初始化完成*********");
        refreshProductModifyWord();
        logger.error("**********产品词+修饰词分数文件初始化完成*********");
		refreshUserSearchWord();
		logger.error("**********用户搜索词文件初始化完成*********");
		refreshTopicFile();
		logger.error("**********topic文件初始化完成*********");
		refreshProductWordFacet();
		logger.error("**********产品词-facet文件初始化完成*********");
        setRefreshTimer();
    }

	private void setRefreshTimer(){
		Timer timer = new Timer();
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				refreshSegmentMark();
				logger.error("**********搜索词分词类型字典数据刷新完成*********");
				refreshProductTags();
				logger.error("**********商品标签配置重写配置文件初始化完成*********");
				refreshProductModifyWord();
				logger.error("**********产品词+修饰词分数文件初始化完成*********");
				refreshUserSearchWord();
				logger.error("**********用户搜索词文件初始化完成*********");
				refreshTopicFile();
				logger.error("**********topic文件初始化完成*********");
				refreshProductWordFacet();
				logger.error("**********产品词-facet文件刷新完成*********");
			}
		}, CommonConsts.GUAVA_REFRESH_DELAY, CommonConsts.GUAVA_REFRESH_PERIOD);
		logger.error("**********搜索词分词类型字典数据、性别解析、重写规则、品牌词重写配置文件自动刷新任务启动*********");
	}

	private void refreshSegmentMark(){
		// 先远程下载配置文件
		FileUtil.download(CommonConsts.PRODUCT_TERMS_URL, CommonConsts.PRODUCT_DIC_PATH);
		FileUtil.download(CommonConsts.BRAND_TERMS_URL, CommonConsts.BRAND_DIC_PATH);
		FileUtil.download(CommonConsts.ATTRIBUTE_TERMS_URL, CommonConsts.ATTRIBUTE_DIC_PATH);
		FileUtil.download(CommonConsts.FEATURE_TERMS_URL, CommonConsts.FEATURE_DIC_PATH);

		querySegmentMarkCache.refreshAll();
	}

	private void refreshProductTags(){
		FileUtil.download(CommonConsts.PRODUCT_TAGS_URL, CommonConsts.PRODUCT_TAGS_PATH);
		productTagsCache.refreshValue();
	}

	private void refreshProductModifyWord() {
		FileUtil.download(CommonConsts.PRODUCT_MODIFY_SCORE_URL, CommonConsts.PRODUCT_MODIFY_SCORE_PATH);
		productModifyWordScoreCache.refreshValue();
	}

	private void refreshUserSearchWord() {
		FileUtil.download(CommonConsts.USER_SEARCH_WORD_URL, CommonConsts.USER_SEARCH_WORD_PATH);
		searchWordDataCache.refreshValue();
	}
	
	private void refreshTopicFile() {
		FileUtil.download(CommonConsts.SPECIAL_TOPIC_URL, CommonConsts.SPECIAL_TOPIC_PATH);
		specialTopicCache.refreshValue();
	}

	// 刷新产品词-facet缓存
	private void refreshProductWordFacet(){
		FileUtil.download(CommonConsts.PRODUCT_WORD_FACET_URL, CommonConsts.PRODUCT_WORD_FACET_PATH);
		productWordFacetCache.refreshValue();
	}
}
