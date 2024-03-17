package com.biyao.search.bs.server.remote;

import com.biyao.search.bs.server.bean.ScoreWord;
import com.biyao.search.bs.server.bean.SexWord;
import com.biyao.search.bs.server.cache.guava.detail.ProductModifyWordScoreCache;
import com.biyao.search.bs.server.query.impl.RelativeQueryParser;
import com.biyao.search.bs.service.TextLinkMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.Status;
import com.biyao.search.common.model.TextLink;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service("textLinkMatch")
public class TextLinkMatchImpl implements TextLinkMatch {

    private Logger logger = LoggerFactory.getLogger(TextLinkMatchImpl.class);

    @Autowired
    private RelativeQueryParser relativeQueryParser;

    @Autowired
    private ProductModifyWordScoreCache productModifyWordScoreCache;

    @Override
    public RPCResult<List<TextLink>> match(MatchRequest matchRequest) {
        try {
            List<TextLink> textLinks = getExtensionWords(matchRequest.getQuery(), matchRequest.getExpectNum());
            return new RPCResult<List<TextLink>>(textLinks);
        } catch (Exception e) {
        	logger.error("获取扩展词时发生异常，query:{}", matchRequest.getQuery(), e);
        	return new RPCResult<>(new Status(0, "获取扩展词异常"));
        }

    }

    /**
     * 获取拓展词
     *
     * @param query
     * @param size
     * @return
     */
    private List<TextLink> getExtensionWords(String query, int size) throws Exception {
        List<TextLink> res = new ArrayList<>();
        if (StringUtils.isBlank(query) || size <= 0)
            return res;

        try {
            SexWord sexWord = relativeQueryParser.parseSexWord(query);
            query = sexWord.getNewQuery();

            List<ScoreWord> modifyWords = productModifyWordScoreCache.getModifyWordsByProductWord(query);

            relativeQueryParser.sortScoreWord(modifyWords);

            for (ScoreWord sw : modifyWords) {
                TextLink tl = new TextLink();
                tl.setShowQuery(sw.getWord());
                tl.setRealQuery((query + " " + sw.getWord() + " " + sexWord.getSexWords()).trim());

                res.add(tl);
                if (res.size() >= size)
                    break;
            }
        } catch (Exception e) {
            logger.error("获取扩展词异常 query:" + query, e);
            throw e;
        }

        return res;
    }
}
