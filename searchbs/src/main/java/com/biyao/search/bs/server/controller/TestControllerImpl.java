package com.biyao.search.bs.server.controller;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import com.biyao.search.bs.server.cache.memory.ReletedWordCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service("testControllerImpl")
@Path("/")
@Produces({ContentType.APPLICATION_JSON_UTF_8})
public class TestControllerImpl implements TestController {

    @Autowired
    private ReletedWordCache reletedWordCache;

    @Override
    @GET
    @Path("testonline")
    public Map<String, String> testonline() {

        Map<String, String> result = new HashMap<>();
        result.put("state", "yes");
        return result;
    }

    @Override
    @GET
    @Path("getRelatedWordsByProductWord")
    public List<String> getRelatedWordsByProductWord(@QueryParam("productWord") String productWord) {
        List<String> relatedWordList = reletedWordCache.getReletedList(productWord);
        return relatedWordList;
    }


    @Override
    @GET
    @Path("refreshRelatedWordCache")
    public Map<String, String> refreshRelatedWordCache() {
        reletedWordCache.refresh();
        Map<String, String> result = new HashMap<>();
        result.put("state", "缓存更新成功");
        return result;
    }
}
