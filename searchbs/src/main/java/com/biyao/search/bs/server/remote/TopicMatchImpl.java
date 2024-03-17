package com.biyao.search.bs.server.remote;

import com.biyao.search.bs.server.bean.SpecialTopic;
import com.biyao.search.bs.server.cache.guava.detail.SpecialTopicCache;
import com.biyao.search.bs.service.TopicMatch;
import com.biyao.search.bs.service.model.request.TopicMatchRequest;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.Status;
import com.biyao.search.common.model.TopicItem;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("topicMatch")
public class TopicMatchImpl implements TopicMatch {

	private Logger logger = LoggerFactory.getLogger(TopicMatchImpl.class);

	@Autowired
	private SpecialTopicCache specialTopicCache;

	@BProfiler(key = "com.biyao.search.bs.server.remote.match", monitorType = { MonitorType.TP, MonitorType.HEARTBEAT,
			MonitorType.FUNCTION_ERROR })
	@Override
	public RPCResult<List<TopicItem>> match(TopicMatchRequest topicMatchRequest) {
		try {
			int expectNum = null == topicMatchRequest.getExpectNum() ? 0 : topicMatchRequest.getExpectNum();
			List<SpecialTopic> topicList = specialTopicCache.getTopicList(topicMatchRequest.getTopicType(), expectNum);
			return new RPCResult<>(parseList(topicList));
		} catch (Exception e) {
			logger.error("获取专题时发生异常，query:{}", topicMatchRequest.getQuery(), e);
			return new RPCResult<>(new Status(0, "获取专题异常"));
		}
	}

	private List<TopicItem> parseList(List<SpecialTopic> topicList) throws Exception {
		List<TopicItem> res = new ArrayList<>();
		for (SpecialTopic st : topicList) {
			TopicItem ti = new TopicItem();
			ti.setTopicId(st.getId());
			ti.setImageUrl(st.getUrl());
			ti.setType(st.getType());
			ti.setShowQuery(st.getTopic());
			res.add(ti);
		}
		return res;
	}

	@BProfiler(key = "com.biyao.search.bs.server.remote.getTopicItem", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public RPCResult<TopicItem> getTopicItem(Integer topicId) {
		try {
			SpecialTopic specialTopic = specialTopicCache.getDataByTopicId(topicId);
			TopicItem topicItem = new TopicItem();
			topicItem.setTopicId(specialTopic.getId());
			topicItem.setImageUrl(specialTopic.getUrl());
			topicItem.setType(specialTopic.getType());
			topicItem.setShowQuery(specialTopic.getTopic());

			topicItem.setSubTitle(specialTopic.getSubTitle());
			topicItem.setProductImgUrlList(specialTopic.getProductImageUrlList());
			topicItem.setSummary(specialTopic.getSummary());

			return new RPCResult<>(topicItem);
		} catch (Exception e) {
			logger.error("获取专题时发生异常，topicId:{}", topicId, e);
			return new RPCResult<>(new Status(0, "获取专题异常"));
		}
	}

	@BProfiler(key = "com.biyao.search.bs.server.remote.getTopicItemMap", monitorType = { MonitorType.TP,
			MonitorType.HEARTBEAT, MonitorType.FUNCTION_ERROR })
	@Override
	public RPCResult<Map<Integer, TopicItem>> getTopicItemMap() {
		try {
			Map<Integer, SpecialTopic> specialTopicMap = specialTopicCache.getSpecialTopicMap();
			Map<Integer, TopicItem> TopicItemMap = new HashMap<>();
			for (Integer topicId : specialTopicMap.keySet()) {
				SpecialTopic specialTopic = specialTopicMap.get(topicId);
				TopicItem topicItem = new TopicItem();
				topicItem.setTopicId(specialTopic.getId());
				topicItem.setImageUrl(specialTopic.getUrl());
				topicItem.setType(specialTopic.getType());
				topicItem.setShowQuery(specialTopic.getTopic());

				topicItem.setSubTitle(specialTopic.getSubTitle());
				topicItem.setProductImgUrlList(specialTopic.getProductImageUrlList());
				topicItem.setSummary(specialTopic.getSummary());

				TopicItemMap.put(topicId, topicItem);
			}
			return new RPCResult<>(TopicItemMap);
		} catch (Exception e) {
			logger.error("获取全部专题信息时发生异常", e);
			return new RPCResult<>(new Status(0, "获取全部专题信息异常"));
		}
	}
}
