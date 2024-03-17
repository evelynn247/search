package com.biyao.search.bs.server.rpc;

import com.biyao.uc.service.UcServerService;
import com.uc.domain.bean.User;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import com.uc.domain.result.ResultCodeMsg;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author zj
 * @version 1.0
 * @date 2019/12/24 21:56
 * @description
 */
@Service
public class UcRpcService {

    private Logger log = LoggerFactory.getLogger(getClass());

    @Autowired
    UcServerService ucServerService;

    public User geUserData(String uuid, String uid, List<String> fields, String caller){
        UserRequest userRequest = new UserRequest();
        userRequest.setUuid(uuid);
        userRequest.setUid(uid);
        userRequest.setCaller(caller);
        userRequest.setFields(fields);
        User result = null;
        ApiResult<User> queryResult = null;
        try{
            queryResult = ucServerService.query(userRequest);
        }catch (Exception e){
            log.error("查询uc出现异常，uuid {}, uid {}, fields {}, caller {}, e{}", uuid, uid, fields, caller, e);
        }

        if(queryResult != null && queryResult.getCode().equals(ResultCodeMsg.SUCCESS_CODE)){
            result = queryResult.getData();
        }else{
            log.error("查询uc出现错误，uuid {}, uid {}, fields {}, caller {}", uuid, uid, fields, caller);
        }
        return result;
    }
}
