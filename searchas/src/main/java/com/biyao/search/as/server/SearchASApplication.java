package com.biyao.search.as.server;

import com.alibaba.fastjson.JSON;
import com.biyao.search.as.service.ASMainSearchService;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.as.service.model.request.SearchServiceRequest;
import com.biyao.search.as.service.model.response.ASProdcutSearchResult;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.common.model.RPCResult;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SearchASApplication {

    static public Logger logger = LoggerFactory.getLogger(SearchASApplication.class);

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/search-as.xml"});
        context.start();
        test3(context);
        System.in.read(); // 按任意键退出
    }


    public static void test(ClassPathXmlApplicationContext context) {

        ASMainSearchService asMainSearchService = (ASMainSearchService) context.getBean("asMainService");
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery("test");
        searchRequest.setExpectNum(500);
        CommonRequestParam commonRequestParam = new CommonRequestParam();
        commonRequestParam.setUuid("mccc");
        commonRequestParam.setUid(111445);
        commonRequestParam.setSid("123456789");
        commonRequestParam.setPlatform(PlatformEnum.IOS);
        searchRequest.setCommonParam(commonRequestParam);
        //searchRequest.setSupplierId(130022L);
        //searchRequest.setAliasType(6);
        searchRequest.setHyChannelId("100001");

        RPCResult<ASProdcutSearchResult> result = asMainSearchService.productSearch(searchRequest);
        System.out.println("test");
    }

    public static void test1(ClassPathXmlApplicationContext context) {

        ASMainSearchService asMainSearchService = (ASMainSearchService) context.getBean("asMainService");
        SearchServiceRequest searchServiceRequest = new SearchServiceRequest();
        searchServiceRequest.setQuery("分销活动");
        searchServiceRequest.setSid("123456789");
        searchServiceRequest.setUuid("92012081812472b17d80bb2f9b3090000000");
        searchServiceRequest.setUid(111445);
        searchServiceRequest.setHyChannelId("100100");
        searchServiceRequest.setCaller("pro2c");
        searchServiceRequest.setSiteId("7");
        searchServiceRequest.setIsGetPartial(1);
        searchServiceRequest.setSceneId(3); 
//        searchServiceRequest.setHyTopicId("3");
        //searchServiceRequest.setProductPool("1");

        RPCResult<ASProdcutSearchResult> result = asMainSearchService.searchService(searchServiceRequest);
        System.out.println(JSON.toJSONString(result));
        System.out.println("test");
    }

    public static void test2(ClassPathXmlApplicationContext context) {

        ASMainSearchService asMainSearchService = (ASMainSearchService) context.getBean("asMainService");
        SearchServiceRequest searchServiceRequest = new SearchServiceRequest();
        searchServiceRequest.setQuery("测试");
        searchServiceRequest.setSid("123456789");
        searchServiceRequest.setUuid("92012081812472b17d80bb2f9b3090000000");
        searchServiceRequest.setUid(111445);
        searchServiceRequest.setSiteId("4");
        searchServiceRequest.setIsGetPartial(1);
        //searchServiceRequest.setProductPool("1");
        searchServiceRequest.setCaller("searchas");

        RPCResult<ASProdcutSearchResult> result = asMainSearchService.searchService(searchServiceRequest);
        System.out.println("test");
    }
    
    /**
     * 鸿源V3.3 分销小程序和购买小程序 搜索功能支持
     * @param context
     */
    public static void test3(ClassPathXmlApplicationContext context) {

        ASMainSearchService asMainSearchService = (ASMainSearchService) context.getBean("asMainService");
        SearchServiceRequest searchServiceRequest = new SearchServiceRequest();
        searchServiceRequest.setQuery("分销标签");
        searchServiceRequest.setSid("123456789");
        searchServiceRequest.setUuid("92012081812472b17d80bb2f9b3090000000");
        searchServiceRequest.setUid(111445);
        searchServiceRequest.setHyChannelId("210049");
        searchServiceRequest.setCaller("pro2c");
        searchServiceRequest.setSiteId("4");
        searchServiceRequest.setIsGetPartial(1);
        //场景值：3分销小程序  4购买小程序
        searchServiceRequest.setSceneId(3); 
//        searchServiceRequest.setHyTopicId("3");
        //searchServiceRequest.setProductPool("1");
        System.out.println(ToStringBuilder.reflectionToString(searchServiceRequest,ToStringStyle.MULTI_LINE_STYLE));
        RPCResult<ASProdcutSearchResult> result = asMainSearchService.searchService(searchServiceRequest);
        System.out.println(JSON.toJSONString(result));
        System.out.println("test");
    }
}
