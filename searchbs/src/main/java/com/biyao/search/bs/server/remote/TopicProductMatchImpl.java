package com.biyao.search.bs.server.remote;

import com.biyao.search.bs.server.bean.SpecialTopic;
import com.biyao.search.bs.server.cache.guava.detail.SpecialTopicCache;
import com.biyao.search.bs.service.TopicProductMatch;
import com.biyao.search.bs.service.model.request.TopicProductMatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.SearchItem;
import com.biyao.search.common.model.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("topicProductMatch")
public class TopicProductMatchImpl implements TopicProductMatch {

    private Logger logger = LoggerFactory.getLogger(TopicProductMatchImpl.class);

    @Autowired
    private SpecialTopicCache specialTopicCache;

    @Override
    public RPCResult<ProductMatchResult> match(TopicProductMatchRequest topicProductMatchRequest) {
        try {
            ProductMatchResult res = new ProductMatchResult();
            SpecialTopic data = specialTopicCache.getDataByTopicId(topicProductMatchRequest.getTopicId());

            int expectNum = null == topicProductMatchRequest.getExpectNum() ? 0 : topicProductMatchRequest.getExpectNum();
            if (null != data) {
                for (int i = 0; i < data.getPids().size(); i++) {
                    SearchItem item = new SearchItem();
                    item.setProductId(data.getPids().get(i));
                    res.getItems().add(item);

                    if (expectNum > 0 && res.getItems().size() >= expectNum)
                        break;
                }
            }

            return new RPCResult<>(res);
        } catch (Exception e) {
            logger.error("根据topic获取专题时发生异常，query:{}", topicProductMatchRequest.getQuery(), e);
            return new RPCResult<>(new Status(0, "根据topic获取专题异常"));
        }
    }
}
