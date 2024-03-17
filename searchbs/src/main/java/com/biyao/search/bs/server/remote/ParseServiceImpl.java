package com.biyao.search.bs.server.remote;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.alibaba.fastjson.JSON;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.bs.server.cache.memory.DictionaryCache;
import com.biyao.search.bs.server.cache.memory.QueryProductCache;
import com.biyao.search.bs.server.cache.memory.RedisCache;
import com.biyao.search.bs.server.common.config.ESClientConfig;
import com.biyao.search.bs.server.common.consts.ElasticSearchConsts;
import com.biyao.search.bs.server.common.util.DclogUtil;
import com.biyao.search.bs.service.ParseService;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.common.enums.QueryAnalyzerEnum;
import com.biyao.search.common.enums.QueryTermTypeEnum;
import com.biyao.search.common.model.ParseResponse;
import com.biyao.search.common.model.QueryTerm;
import com.biyao.search.common.model.RPCResult;
import org.apache.commons.lang.StringUtils;
import org.elasticsearch.action.admin.indices.analyze.AnalyzeResponse;
import org.elasticsearch.client.transport.TransportClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.*;

/**
 * @author zj
 * @version 1.0
 * @date 2019/11/1 11:19
 * @description
 */
@Service("parseService")
@Path("/")
@Produces({ContentType.APPLICATION_JSON_UTF_8})
public class ParseServiceImpl implements ParseService {

    @Autowired
    private DictionaryCache dictionaryCache;

    @Autowired
    private QueryProductCache queryProductCache;

    @Autowired
    private RedisCache redisCache;

    /**
     * query词解析dclog
     */
    private DCLogger queryParseDclogger = DCLogger.getLogger("searchbs_query_parse");

    @Override
    @GET
    @Path("queryParse")
    public RPCResult<ParseResponse> parse(MatchRequest matchRequest) {

        ParseResponse result = new ParseResponse();
        //query为空直接返回
        if (StringUtils.isBlank(matchRequest.getQuery())) {
            return new RPCResult<>(result);
        }

        result.setQuery(matchRequest.getQuery());
        //分词
        List<String> termList = new ArrayList<>();
        TransportClient client = ESClientConfig.getESClient();
        AnalyzeResponse analyzeResponse = client.admin().indices().prepareAnalyze(matchRequest.getQuery())
                .setIndex(ElasticSearchConsts.BY_MALL_ALIAS).setAnalyzer(QueryAnalyzerEnum.IK_SMART.getCode()).get();
        for (AnalyzeResponse.AnalyzeToken token : analyzeResponse.getTokens()) {
            termList.add(token.getTerm());
        }

        //词典匹配
        termList.forEach(item -> {
            result.getAllTermList().add(new QueryTerm(item, 0d));
            Map<String, Double> termTypeList = dictionaryCache.getWordMap(item);
            for (Map.Entry<String, Double> termType : termTypeList.entrySet()) {
//                if (QueryTermTypeEnum.PRODUCT.getCode().equals(termType.getKey())) {
//                    result.getProductTerm().add(new QueryTerm(item, termType.getValue()));
//                }
                if (QueryTermTypeEnum.ATTRIBUTE.getCode().equals(termType.getKey())) {
                    result.getAttributeTerm().add(new QueryTerm(item, termType.getValue()));
                }
                if (QueryTermTypeEnum.FUNCTION.getCode().equals(termType.getKey())) {
                    result.getFunctionTerm().add(new QueryTerm(item, termType.getValue()));
                }
                if (QueryTermTypeEnum.OTHER.getCode().equals(termType.getKey())) {
                    result.getOtherTerm().add(new QueryTerm(item, termType.getValue()));
                }
                if (QueryTermTypeEnum.SEX.getCode().equals(termType.getKey())) {
                    result.getSexTerm().add(new QueryTerm(item, termType.getValue()));
                }
                if (QueryTermTypeEnum.SEASON.getCode().equals(termType.getKey())) {
                    result.getSeasonTerm().add(new QueryTerm(item, termType.getValue()));
                }
                if (QueryTermTypeEnum.BRAND.getCode().equals(termType.getKey())) {
                    result.getBrandTerm().add(new QueryTerm(item, termType.getValue()));
                }
            }
        });

        //query产品词识别
        String productWord = queryProductCache.getQueryProduct(matchRequest.getQuery());
        if(StringUtils.isNotBlank(productWord)){
            result.getProductTerm().add(new QueryTerm(productWord, 0d));
        }

        //打印dclog
        StringBuilder sb = new StringBuilder(10240);
        sb.append("lt=query_parse");
        sb.append("\tlv=1.0");
        sb.append("\tuu=");
        sb.append(matchRequest.getCommonParam().getUuid());
        sb.append("\tu=");
        sb.append(matchRequest.getCommonParam().getUid() == null ? "" : matchRequest.getCommonParam().getUid());
        sb.append("\tsid=");
        sb.append(matchRequest.getCommonParam().getSid() == null ? "" : matchRequest.getCommonParam().getSid());
        sb.append("\tpf=");
        sb.append(matchRequest.getCommonParam().getPlatform() == null ? "" : matchRequest.getCommonParam().getPlatform().getName());
        sb.append("\tquery=");
        sb.append(matchRequest.getQuery());
        sb.append("\tparse_result=");
        sb.append(JSON.toJSONString(result));
        sb.append("\tst=");
        sb.append(System.currentTimeMillis());
        DclogUtil.sendDclog(queryParseDclogger, sb.toString());
        return new RPCResult<>(result);
    }
}
