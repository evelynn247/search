package com.biyao.search.as.server;

import com.alibaba.fastjson.JSON;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.as.server.feature.manager.LambdaMARTRankerManager;
import com.biyao.search.as.service.ASSearchService;
import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.as.service.model.response.ASHiResponse;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.bs.service.BSSearchService;
import com.biyao.search.bs.service.model.request.BSSearchRequest;
import com.biyao.search.bs.service.model.response.BSSearchResponse;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.RPCResult;
import com.biyao.search.common.model.SearchItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @description: 搜索dubbo服务实现
 * @author: luozhuo
 * @version: V1.0.0
 *
 * 2018-05-15 注释掉扩展词实验、分词日志
 */
@Service("asSearchService")
public class SearchDubboServiceImpl implements ASSearchService{
	private Logger logger = LoggerFactory.getLogger(getClass());
	private DCLogger dclogger = DCLogger.getLogger("searchas_normal_request");

	@Autowired
	private BSSearchService bsSearchService;

	@Autowired
    private LambdaMARTRankerManager lambdaMARTRankerManager;

	@Override
	public RPCResult<ASSearchResponse<ASProduct>> search(ASSearchRequest asSearchRequest) {
		long start = System.currentTimeMillis();
		
		dclogger.printDCLog(JSON.toJSONString(asSearchRequest));
		
		BSSearchRequest bsRequest = new BSSearchRequest();
		bsRequest.setSid(asSearchRequest.getSid());
		bsRequest.setQuery(asSearchRequest.getQuery());
		bsRequest.setUuid(asSearchRequest.getUuid());
		bsRequest.setExpectNum(asSearchRequest.getPageSize());
		bsRequest.setPlatform(PlatformEnum.getByName(asSearchRequest.getPlatform().getName()));
		
		RPCResult<BSSearchResponse<ASProduct>> bsRpcResult = null;
		try {
			bsRpcResult = bsSearchService.match(bsRequest);
		} catch (Exception e) {
			logger.error("[严重异常][dubbo异常]调用(接口BSSearchService#match)发生异常, uuid:{}, query:{}", asSearchRequest.getUuid(), asSearchRequest.getQuery(), e);
			return new RPCResult<ASSearchResponse<ASProduct>>(SearchStatus.AS.UNKNOWN);
		}
        List<ASProduct> products = bsRpcResult.getData().getResult();

		try {
			//根据待排元素格式和新的排序模型重载一个排序方法
			products = lambdaMARTRankerManager.sort4PC(products, asSearchRequest.getQuery());
		} catch (Exception e) {
			logger.error("[未知异常]PC端排序异常,异常堆栈：", e);
		}
        ASSearchResponse<ASProduct> asSearchResponse = new ASSearchResponse<ASProduct>();
		asSearchResponse.setResult(products);
		asSearchResponse.setHitTotal(products.size());
		asSearchResponse.setEsTookTime(bsRpcResult.getData().getEsTookTime());
		long end = System.currentTimeMillis();
		asSearchResponse.setBsTookTime((int) (end - start));
		
		return new RPCResult<ASSearchResponse<ASProduct>>(asSearchResponse);
	}



	/**
	 * AS Hi 接口
	 */
	public RPCResult<ASHiResponse> hi(){
	    ASHiResponse asHiResponse = new ASHiResponse();
	    return new RPCResult<ASHiResponse>(asHiResponse);
	}
}
