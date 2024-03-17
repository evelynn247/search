package com.biyao.search.as.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.as.service.ASDesignerSearchService;
import com.biyao.search.as.service.model.request.ASSearchRequest;
import com.biyao.search.as.service.model.response.ASSearchResponse;
import com.biyao.search.bs.service.BSDesignerSearchService;
import com.biyao.search.bs.service.model.request.BSSearchRequest;
import com.biyao.search.bs.service.model.response.BSSearchResponse;
import com.biyao.search.common.constant.SearchStatus;
import com.biyao.search.common.model.ASDesigner;
import com.biyao.search.common.model.RPCResult;

/**
 * @description: 搜索dubbo服务实现
 * @author: luozhuo
 * @version: V1.0.0
 */
@Service("asDesignerSearchService")
public class DesignerSearchDubboServiceImpl implements ASDesignerSearchService{
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	private BSDesignerSearchService bsSearchService;
	
	
	public RPCResult<ASSearchResponse<ASDesigner>> search(ASSearchRequest asSearchRequest) {
		long start = System.currentTimeMillis();
		
		BSSearchRequest bsRequest = new BSSearchRequest();
		bsRequest.setSid(asSearchRequest.getSid());
		bsRequest.setQuery(asSearchRequest.getQuery());
		bsRequest.setUuid(asSearchRequest.getUuid());
		bsRequest.setExpectNum(asSearchRequest.getPageSize());
		
		RPCResult<BSSearchResponse<ASDesigner>> bsRpcResult = null;
		try {
			bsRpcResult = bsSearchService.search(bsRequest);
		} catch (Exception e) {
			logger.error("[严重异常][dubbo异常]调用(BSDesignerProductSearchService#search)发生异常, uuid:{}, query:{}", asSearchRequest.getUuid(), asSearchRequest.getQuery(), e);
			return new RPCResult<ASSearchResponse<ASDesigner>>(SearchStatus.AS.UNKNOWN);
		}
		
		List<ASDesigner> products = bsRpcResult.getData().getResult();
		
		ASSearchResponse<ASDesigner> asSearchResponse = new ASSearchResponse<ASDesigner>();
		asSearchResponse.setResult(products);
		asSearchResponse.setHitTotal(products.size());
		asSearchResponse.setEsTookTime(bsRpcResult.getData().getEsTookTime());
		long end = System.currentTimeMillis();
		asSearchResponse.setBsTookTime((int) (end - start));
		
		return new RPCResult<ASSearchResponse<ASDesigner>>(asSearchResponse);
	}
	
}
