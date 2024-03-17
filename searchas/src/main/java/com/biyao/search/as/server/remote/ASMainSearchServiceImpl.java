package com.biyao.search.as.server.remote;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.alibaba.fastjson.JSON;
import com.biyao.dclog.service.DCLogger;
import com.biyao.search.as.server.cache.redis.UuidWhiteListCache;
import com.biyao.search.as.server.enums.ActivityEnum;
import com.biyao.search.as.server.experiment.ASExperimentSpace;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeature;
import com.biyao.search.as.server.feature.threadlocal.ThreadLocalFeatureHandler;
import com.biyao.search.as.service.model.request.SearchServiceRequest;
import com.biyao.search.common.constant.SearchLimit;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.*;
import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.biyao.search.as.server.service.IRedisSearchService;
import com.biyao.search.as.service.ASMainSearchService;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.as.service.model.request.TopicProductSearchRequest;
import com.biyao.search.as.service.model.request.TopicSearchRequest;
import com.biyao.search.as.service.model.response.ASProdcutSearchResult;
import com.biyao.search.as.service.model.response.ProductSearchBlock;
import com.biyao.search.bs.service.FacetQuery;
import com.biyao.search.bs.service.PartialQueryFetch;
import com.biyao.search.bs.service.TextLinkMatch;
import com.biyao.search.bs.service.TopicMatch;
import com.biyao.search.bs.service.TopicProductMatch;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.request.TopicMatchRequest;
import com.biyao.search.bs.service.model.request.TopicProductMatchRequest;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.constant.SearchStatus;
import com.by.profiler.annotation.BProfiler;
import com.by.profiler.annotation.MonitorType;
import com.google.common.collect.Lists;

@Service("asMainService")
public class ASMainSearchServiceImpl implements ASMainSearchService {
	@Autowired
	private PartialQueryFetch partialQueryFetch;

	@Autowired
	private IRedisSearchService redisSearchService;

	@Autowired
	private TextLinkMatch textLinkMatch;

	@Autowired
	private TopicMatch topicMatch;

	@Autowired
	private TopicProductMatch topicProductMatch;

	@Autowired
	private FacetQuery facetQuery;

	@Autowired
	private ASExperimentSpace asExperimentSpace;

	@Autowired
	private ThreadLocalFeatureHandler threadLocalFeatureHandler;

	@Autowired
	private UuidWhiteListCache uuidWhiteListCache;

	private Logger logger = LoggerFactory.getLogger(getClass());
	private DCLogger dclogger = DCLogger.getLogger("searchas_normal_request");

	private static final String SPECIAL_CHAR_REGEXP = "[`~!@#$%^&*()+=|{}':;',\\[\\].<>/?~！@#￥%……&*（）——+|{}【】‘；：”“’。，、？\t\r\n]"
			+ "|[\ud83c\udc00-\ud83c\udfff]|[\ud83d\udc00-\ud83d\udfff]|[\u2600-\u27ff]";
	private static Pattern pattern = Pattern.compile(SPECIAL_CHAR_REGEXP,
			Pattern.UNICODE_CASE | Pattern.CASE_INSENSITIVE);

	/**
	 * 根据搜索词搜索商品 包含完全匹配和部分匹配
	 *
	 * @param request
	 * @return
	 * @author: luozhuo
	 * @see com.biyao.search.as.service.ASMainSearchService#productSearch(com.biyao.search.as.service.model.request.SearchRequest)
	 * @date: 2018年7月26日 下午12:00:14
	 */
	@Override
	@BProfiler(key = "com.biyao.search.as.server.remote.productSearch",
    monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
            FUNCTION_ERROR})
	public RPCResult<ASProdcutSearchResult> productSearch(SearchRequest request) {
		// 实验分流
		asExperimentSpace.divert(request);
		dclogger.printDCLog(JSON.toJSONString(request));
		threadLocalFeatureHandler.initThreadLocalContext(request);

		ASProdcutSearchResult result = new ASProdcutSearchResult();

		List<ProductSearchBlock> blocks = searchByQueryAndGenerateBlock(request.getQuery(), 500, request,request.getAliasType());
		if(blocks!= null){
			result.getProductBlocks().addAll(blocks);
		}

		// 手动移出ThreadLocal对象
        ThreadLocalFeature.manualClose();

		// 拼接sem参数
		SetSemStr(result,request.getExpIds());
		return new RPCResult<>(result);
	}

	/**
	 * sem 参数拼接实验号
	 * @param result
	 * @param expIds
	 */
	private void SetSemStr(ASProdcutSearchResult result, List<Integer> expIds) {
		if(expIds.size() == 0){
			return;
		}
		StringBuilder expStr = new StringBuilder();
		for (int id :expIds){
			expStr.append(",").append(id);
		}
		for (ProductSearchBlock productSearchBlock:result.getProductBlocks()) {
			for (SearchItem searchItem:productSearchBlock.getItems()) {
				searchItem.setSemStr(searchItem.getSemStr()+expStr.toString());
			}
		}
	}


	/**
	 * 根据搜索词获取query的facet
	 *
	 * @param finalFacetFromBs
	 * @param request
	 * @return
	 * @author: luozhuo
	 * @date: 2018年7月26日 下午12:09:42
	 */
	private List<FacetItem> newGenerateFacetFunction(List<FacetItem> finalFacetFromBs, SearchRequest request) {
		List<FacetItem> result = new ArrayList<>();
		if (finalFacetFromBs.size() > 0) { // 价格facet始终在第一位
			result.add(finalFacetFromBs.get(0));
		}

		RPCResult<List<FacetItem>> facetRpc = null;
		try {
			facetRpc = facetQuery.match(request.getQuery());
		} catch (Exception e) {
			logger.error("根据搜索词获取facet时调用接口异常", e);
			return result;
		}
		if (facetRpc == null || !SearchStatus.OK.equals(facetRpc.getStatus())) {
			logger.error("根据搜索词获取facet时接口返回失败");
			return result;
		}

		result.addAll(facetRpc.getData());

		return result;
	}

	/**
	 * 根据bs返回的所有商品的facet进行合并
	 *
	 * @param allFacet
	 * @return
	 * @author: luozhuo
	 * @date: 2018年7月26日 下午12:10:59
	 */
	private List<FacetItem> generateFinalFacet(List<FacetItem> allFacet) {
		List<FacetItem> result = new ArrayList<>();

		Integer minPrice = 0, maxPrice = 0;
		boolean minPriceInit = false;
		// key -> 筛选条件 value -> 筛选条件所有可选值
		Map<String, Set<String>> allFacetMap = new LinkedHashMap<>();
		for (FacetItem item : allFacet) {
			// 处理价格区间
			if (item.getKey().equals("价格区间")) {
				for (String value : item.getValues()) {
					for (String price : value.split(" - ")) {
						if (!minPriceInit) {
							minPrice = Integer.valueOf(price);
							minPriceInit = true;
						}

						if (minPrice > Integer.valueOf(price)) { // 当前最小价格比商品价格大
							minPrice = Integer.valueOf(price);
						} else if (maxPrice < Integer.valueOf(price)) { // 当前最大价格比商品价格小
							maxPrice = Integer.valueOf(price);
						}
					}

				}
				continue;
			}

			if (!allFacetMap.containsKey(item.getKey())) {
				allFacetMap.put(item.getKey(), new HashSet<>());
			}

			allFacetMap.get(item.getKey()).addAll(item.getValues());
		}

		// 处理价格facet
		if (maxPrice != minPrice) {
			FacetItem priceItem = new FacetItem();
			int priceDiff = (int) (maxPrice - minPrice);
			int levelDiff = priceDiff % 3 == 0 ? priceDiff / 3 : priceDiff / 3 + 1;

			String level1 = String.valueOf(minPrice.intValue() + levelDiff * 0);
			String level2 = String.valueOf(minPrice.intValue() + levelDiff * 1);
			String level3 = String.valueOf(minPrice.intValue() + levelDiff * 2);
			String level4 = String.valueOf(minPrice.intValue() + levelDiff * 3);
			List<String> values = Lists.newArrayList(level1 + " - " + level2, level2 + " - " + level3,
					level3 + " - " + level4);

			priceItem.setKey("价格区间");
			priceItem.setValues(values);
			result.add(priceItem);
		}

		for (String key : allFacetMap.keySet()) {
			FacetItem item = new FacetItem();
			item.setKey(key);
			item.setValues(new ArrayList<>(allFacetMap.get(key)));

			if (item.getValues().size() < 2) {
				continue;
			}

			result.add(item);
		}

		return result;
	}

	/**
	 * 根据搜索词得到匹配搜索结果，当发生异常时返回null
	 *
	 * @param query
	 * @param expectNum
     * @param request
	 * @return
	 */
	private List<ProductSearchBlock> searchByQueryAndGenerateBlock(String query, Integer expectNum,
			SearchRequest request,Integer aliasType) {

		List<ProductSearchBlock> blocks = new ArrayList<>();
		if (redisSearchService.isTagQuery(query, request) && (aliasType != null || ActivityEnum.NO_ACTIVITY.getCode().equals(aliasType))) {
			//tag搜索
			ProductMatchResult matchResult = redisSearchService.tagSearchByExactQuery(query, expectNum, request);
			if (matchResult == null) {
				return null;
			}
			ProductSearchBlock block = new ProductSearchBlock();
			block.setQuery(matchResult.getQuery());
			block.setItems(matchResult.getItems());
			block.setFacets(matchResult.getFacets());

			blocks.add(block);

		}else{
			//普通搜索
			List<ProductMatchResult> matchResults = redisSearchService.searchByExactQuery(query, expectNum, request,aliasType);
			if (matchResults == null) {
				return null;
			}
			for (ProductMatchResult matchResult :matchResults) {
				ProductSearchBlock block = new ProductSearchBlock();
				block.setQuery(matchResult.getQuery());
				block.setItems(matchResult.getItems());
				block.setFacets(matchResult.getFacets());

				blocks.add(block);
			}
		}

		return blocks;
	}

	/**
	 * 根据搜索词获取文字按钮列表 （目前应该没人调用，词语结果不理想，前端的文字按钮改成直接从热词获取 20180726）
	 *
	 * @param request
	 * @return
	 * @author: Administrator
	 * @see com.biyao.search.as.service.ASMainSearchService#textLinkSearch(com.biyao.search.as.service.model.request.SearchRequest)
	 * @date: 2018年7月26日 下午3:13:46
	 */
	@Override
	public RPCResult<List<TextLink>> textLinkSearch(SearchRequest request) {
		MatchRequest matchRequest = new MatchRequest();
		matchRequest.setCommonParam(request.getCommonParam());
		matchRequest.setQuery(request.getQuery());
		matchRequest.setExpectNum(request.getExpectNum());

		RPCResult<List<TextLink>> rpcResult = textLinkMatch.match(matchRequest);
		if (!SearchStatus.OK.equals(rpcResult.getStatus())) {
			return new RPCResult<>(SearchStatus.AS.UNKNOWN);
		}

		return new RPCResult<List<TextLink>>(rpcResult.getData());
	}

	/**
	 * 获取主题列表
	 *
	 * @param request
	 * @return
	 * @author: Administrator
	 * @see com.biyao.search.as.service.ASMainSearchService#topicSearch(com.biyao.search.as.service.model.request.TopicSearchRequest)
	 * @date: 2018年7月26日 下午3:15:19
	 */
	@Override
	public RPCResult<List<TopicItem>> topicSearch(TopicSearchRequest request) {
		TopicMatchRequest topicMatchRequest = new TopicMatchRequest();
		topicMatchRequest.setCommonParam(request.getCommonParam());
		topicMatchRequest.setExpectNum(request.getExpectNum());
		topicMatchRequest.setTopicType(request.getTopicType());
		topicMatchRequest.setQuery(request.getQuery());

		RPCResult<List<TopicItem>> rpcResult = topicMatch.match(topicMatchRequest);
		if (rpcResult == null || !SearchStatus.OK.equals(rpcResult.getStatus())) {
			return new RPCResult<>(SearchStatus.AS.UNKNOWN);
		}

		return new RPCResult<List<TopicItem>>(rpcResult.getData());
	}

	/**
	 * 获取主题下的商品列表
	 *
	 * @param request
	 * @return
	 * @author: Administrator
	 * @see com.biyao.search.as.service.ASMainSearchService#topicProductSearch(com.biyao.search.as.service.model.request.TopicProductSearchRequest)
	 * @date: 2018年7月26日 下午3:15:38
	 */
	@Override
	public RPCResult<ASProdcutSearchResult> topicProductSearch(TopicProductSearchRequest request) {
		TopicProductMatchRequest matchRequest = new TopicProductMatchRequest();
		matchRequest.setCommonParam(request.getCommonParam());
		matchRequest.setExpectNum(request.getExpectNum());
		matchRequest.setQuery(request.getQuery());
		matchRequest.setTopicId(request.getTopicId());

		RPCResult<ProductMatchResult> rpcResult = topicProductMatch.match(matchRequest);
		if (rpcResult == null || !SearchStatus.OK.equals(rpcResult.getStatus())) {
			return new RPCResult<>(SearchStatus.AS.UNKNOWN);
		}

		ASProdcutSearchResult result = new ASProdcutSearchResult();

		ProductSearchBlock block = new ProductSearchBlock();
		block.setQuery(rpcResult.getData().getQuery());
		block.setItems(rpcResult.getData().getItems());

		result.setQuery(rpcResult.getData().getQuery());
		result.getProductBlocks().add(block);

		return new RPCResult<ASProdcutSearchResult>(result);
	}

	/**
	 * 判断是否是白名单
	 *
	 * @param query
	 * @return
	 * @author: Administrator
	 * @see com.biyao.search.as.service.ASMainSearchService#isTagQuery(java.lang.String)
	 * @date: 2018年7月26日 下午3:15:50
	 */
	@Override
	public RPCResult<Boolean> isTagQuery(String query) {
		RPCResult<Boolean> rpcResult = null;
		try {
			rpcResult = partialQueryFetch.isTagQuery(query);
		} catch (Exception e) {
			logger.error("判断是否是白名单词语时bs发生异常，query：{}", query);
			e.printStackTrace();
		}

		if (rpcResult == null || !SearchStatus.OK.equals(rpcResult.getStatus())) {
			logger.error("判断是否是白名单词语时bs返回结果有误，query：{}", query);
			return new RPCResult<Boolean>(false);
		}

		return new RPCResult<Boolean>(rpcResult.getData());
	}

	/**
	 * 根据搜索词搜索商品，完全匹配
	 */
	@BProfiler(key = "com.biyao.search.as.server.remote.productCompletelySearch",
		    monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
		            FUNCTION_ERROR})
	@Override
	public RPCResult<ASProdcutSearchResult> productCompletelySearch(SearchRequest request) {
		// 初始化ThreadLocal对象
		threadLocalFeatureHandler.initThreadLocalContext(request);
		ASProdcutSearchResult result = new ASProdcutSearchResult();
		request.setQuery(request.getQuery());

		/* 先进行完全匹配 */
		List<ProductSearchBlock> blocks = searchByQueryAndGenerateBlock(request.getQuery(), 500, request,1);
		if(blocks!= null){
			result.getProductBlocks().addAll(blocks);
		}

		//返回前手动释放ThreadLocal对象
		ThreadLocalFeature.manualClose();
		return new RPCResult<ASProdcutSearchResult>(result);

	}

	/**
	 *对外暴露接口，不支持searchui调用
	 * @param request
	 * @return
	 */
	@BProfiler(key = "com.biyao.search.as.server.remote.searchService",
		    monitorType = {MonitorType.TP, MonitorType.HEARTBEAT, MonitorType.
		            FUNCTION_ERROR})
	public RPCResult<ASProdcutSearchResult> searchService(SearchServiceRequest request) {
		//参数校验&转换为SearchRequest
		if (!checkParameter(request)) {
			Status status = new Status(1, "参数校验不通过，请检查必传参数");
			return new RPCResult<>(status);
		}
		SearchRequest searchRequest = convertToSearchRequest(request);
		//调用productSearch接口，productSearch接口实现逻辑修改
		//根据场景Id,控制是否进入tag搜索
		//是否进行部分匹配召回字段赋值
		RPCResult<ASProdcutSearchResult> result = productSearch(searchRequest);
		//召回结果返回
		return result;
	}

	/**
	 * 请求参数转换，复用productSearch接口
	 *
	 * @return
	 */
	public SearchRequest convertToSearchRequest(SearchServiceRequest request) {
		SearchRequest searchRequest = new SearchRequest();
		String query=request.getQuery();
		// 请求参数过滤特殊符号
		if (query == null) {
			query = "";
		}
		query = pattern.matcher(query).replaceAll("").trim();
		if (query.length() > SearchLimit.MAX_QUERY_LENGTH) {
			query = query.substring(0, SearchLimit.MAX_QUERY_LENGTH);
		}
		searchRequest.setQuery(query);
		CommonRequestParam commonRequestParam = new CommonRequestParam();
		commonRequestParam.setSid(request.getSid());
		commonRequestParam.setUid(request.getUid());
		commonRequestParam.setUuid(request.getUuid());
		commonRequestParam.setPlatform(PlatformEnum.getPlatformByNum(Integer.valueOf(request.getSiteId())));
		searchRequest.setCommonParam(commonRequestParam);
		searchRequest.setSiteId(request.getSiteId());
		//是否进行部分匹配,商品池,场景Id
		searchRequest.setIsGetPartial(request.getIsGetPartial());
		//searchRequest.setProductPool(request.getProductPool());
		searchRequest.setSceneId(request.getSceneId());
		//aliasType，控制是否进入tag搜索,null表示不进入tag搜索，searchService接口请求默认不进入tag搜索，如有特殊需求，根据sceneId转换为aliasType控制（默认不支持tag搜索）
		searchRequest.setAliasType(null);
		searchRequest.setHyChannelId(request.getHyChannelId());
		searchRequest.setHyActivityId(request.getHyActivityId());
		searchRequest.setHyTopicId(request.getHyTopicId());
		return searchRequest;
	}


	protected boolean checkParameter(SearchServiceRequest request) {
		if(request==null){
			return false;
		}
		if (Strings.isNullOrEmpty(request.getCaller())) {
			return false;
		}
		if (Strings.isNullOrEmpty(request.getSiteId()) || Strings.isNullOrEmpty(request.getUuid())) {
			return false;
		}
		if (Strings.isNullOrEmpty(request.getSid())) {
			return false;
		}
		return true;
	}
}
