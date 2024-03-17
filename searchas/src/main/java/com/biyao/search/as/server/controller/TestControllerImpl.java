package com.biyao.search.as.server.controller;

import com.alibaba.dubbo.rpc.protocol.rest.support.ContentType;
import org.springframework.stereotype.Service;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import java.util.HashMap;
import java.util.Map;

@Service("testControllerImpl")
@Path("/")
@Produces({ContentType.APPLICATION_JSON_UTF_8})
public class TestControllerImpl  implements TestController{

    @Override
    @GET
    @Path("testonline")
    public Map<String, String> testonline() {

        Map<String,String> result = new HashMap<>();
        result.put("state","yes");
        return result;
    }
}
