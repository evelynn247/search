package com.biyao.search.bs.server;

import com.alibaba.fastjson.JSON;
import com.biyao.search.bs.server.remote.*;
import com.biyao.search.bs.service.BSSearchService;
import com.biyao.search.bs.service.NewProductMatch;
import com.biyao.search.bs.service.model.request.BSSearchRequest;
import com.biyao.search.bs.service.model.request.MatchRequest;
import com.biyao.search.bs.service.model.response.BSSearchResponse;
import com.biyao.search.bs.service.model.response.ProductMatchResult;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.ASProduct;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.search.common.model.RPCResult;
import com.google.common.base.Charsets;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.UUID;

public class SearchBSApplication {

    static public Logger logger = LoggerFactory.getLogger(SearchBSApplication.class);

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(new String[]{"spring/search-bs.xml"});
        context.start();
        test0(context);
        System.in.read(); // 按任意键退出
    }

    private static void test0(ClassPathXmlApplicationContext context) {

        BSSearchService newProductMatch = (BSSearchService) context.getBean("bsSearchService");
        BSSearchRequest bsSearchRequest = new BSSearchRequest();
        bsSearchRequest.setQuery("面膜");
        bsSearchRequest.setSid(createBlockId());
        bsSearchRequest.setUuid("922010613191835d955dd4b61a75d0000000");
        bsSearchRequest.setExpectNum(500);
        bsSearchRequest.setPlatform(PlatformEnum.ANDROID);

        RPCResult<BSSearchResponse<ASProduct>> match = newProductMatch.match(bsSearchRequest);
        System.out.println(JSON.toJSONString(match));
        System.out.println("完成");
    }

    private static void test(ClassPathXmlApplicationContext context) {

        NewProductMatch newProductMatch = (NewProductMatch) context.getBean("newProductMatch");

        MatchRequest matchRequest = new MatchRequest();
        CommonRequestParam commonRequestParam = new CommonRequestParam();
        commonRequestParam.setPlatform(PlatformEnum.ANDROID);
        commonRequestParam.setSid(createBlockId());
        commonRequestParam.setUuid("922010613191835d955dd4b61a75d0000000");
        matchRequest.setCommonParam(commonRequestParam);
        matchRequest.setQuery("面膜");
        matchRequest.setExpectNum(500);
        //matchRequest.setAliasType(1);
        //matchRequest.setIsGetPartial(1);
        //matchRequest.setProductPool("1");
        //matchRequest.setAliasType(0);
        //matchRequest.setIsSupportCreation(2);

        RPCResult<List<ProductMatchResult>> result = newProductMatch.match(matchRequest);
        System.out.println(JSON.toJSONString(result));
        System.out.println("完成");
    }

    public static String createBlockId() {
        HashFunction hf = Hashing.md5();
        String uuidMd5 = hf.newHasher().putString(UUID.randomUUID().toString(), Charsets.UTF_8).hash().toString();

        String result = uuidMd5.substring(9, 25);
        return result;
    }
}