package com.biyao.search.as.server.feature.threadlocal;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.biyao.search.as.server.cache.redis.UuidWhiteListCache;
import com.biyao.search.as.server.common.consts.CommonConsts;
import com.biyao.search.as.server.common.util.DcLogUtil;
import com.biyao.search.as.server.feature.model.ContextFeature;
import com.biyao.search.as.server.feature.model.UserFeature;
import com.biyao.search.as.service.model.request.SearchRequest;
import com.biyao.search.common.enums.PlatformEnum;
import com.biyao.search.common.model.CommonRequestParam;
import com.biyao.uc.service.UcServerService;
import com.google.common.collect.Lists;
import com.uc.domain.bean.User;
import com.uc.domain.params.UserRequest;
import com.uc.domain.result.ApiResult;
import com.uc.domain.result.ResultCodeMsg;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Map;

/**
 * @author: xiafang
 * @date: 2019/11/19
 * @modify zhaiweixi 20191121 修改ThreadLocal对象使用方法
 */
@Slf4j
@Service
public class ThreadLocalFeatureHandler {
    /**
     * 搜索站点
     */
    private static final String SEARCH = "searchas";
    /**
     * 上下文特征key,以c_开头
     */
    private static final String C_STID = "c_stid";

    /**
     * 用户特征key，以u_开头
     */
    private static final String U_ID = "u_id";
    private static final String U_SEX = "u_sex";
    private static final String U_C3_PREFER = "u_c3prefer";
    private static final String U_OS = "u_os";
    private static final String U_FS = "u_fs";
    private static final String U_SEASON = "u_season";

    /**
     * 用户特征原始数据字段，来自uc
     */
    private static final String SEX = "sex";
    private static final String SEASON = "season";
    private static final String LEVEL_3_HOBBY = "level3Hobby";
    private static final String BUY_SUPPLIERS = "buySuppliers";
    private static final String COLLECT_SUPPLIERS = "collectSuppliers";

    @Autowired
    UcServerService ucServerService;

    @Resource
    UuidWhiteListCache uuidWhiteListCache;

    /**
     * 初始化ThreadLocal对象
     *
     * @param request
     */
    public void initThreadLocalContext(SearchRequest request) {
        this.buildContextFeature(request);
        this.buildUserFeature(request);
        try {
            String ufLogBody = "sid=" + ThreadLocalFeature.SID.get() + "\tdetail=" + JSON.toJSONString(ThreadLocalFeature.USER_FEATURE.get());
            DcLogUtil.printRankDetail(CommonConsts.LOG_TYPE_USER_FEATURE, ufLogBody);
            String contextLogBody = "sid=" + ThreadLocalFeature.SID.get() + "\tdetail=" + JSON.toJSONString(ThreadLocalFeature.CONTEXT_FEATURE.get());
            DcLogUtil.printRankDetail(CommonConsts.LOG_TYPE_CONTEXT_FEATURE, contextLogBody);
        } catch (Exception e) {
            log.error("[一般异常][dclog异常]发送dcLog失败，日志类型:", CommonConsts.LOG_TYPE_USER_FEATURE, CommonConsts.LOG_TYPE_CONTEXT_FEATURE, e);
        }
    }

    /**
     * 上下文特征初始化
     *
     * @param request
     */
    private void buildContextFeature(SearchRequest request) {
        if (request == null) {
            return;
        }
        ContextFeature contextFeature = new ContextFeature();
        CommonRequestParam commonParam = request.getCommonParam();
        String sid = commonParam.getSid();
        ThreadLocalFeature.SID.set(sid);
        String uuid = commonParam.getUuid();
        ThreadLocalFeature.IS_WHITE_LIST_UUID.set(uuidWhiteListCache.isWhiteListUuid(uuid));
        if (commonParam.getPlatform() == null) {
            contextFeature.getFeatures().put(C_STID, PlatformEnum.M.getNum().toString());
        } else {
            String platform = commonParam.getPlatform().getNum().toString();
            contextFeature.getFeatures().put(C_STID, platform);
        }
        ThreadLocalFeature.CONTEXT_FEATURE.set(contextFeature);
    }


    /**
     * 根据uc的基础数据转换为排序模型的用户特征UserFeature
     *
     * @param request
     */
    private void buildUserFeature(SearchRequest request) {
        User userInfo = getUserInfo(request);
        UserFeature userFeature = new UserFeature();
        if (userInfo == null) {
            ThreadLocalFeature.USER_FEATURE.set(userFeature);
            return;
        }
        try {
            userFeature.setUuid(userInfo.getUuid());
            String uid = userInfo.getUid();
            if (StringUtils.isNotEmpty(uid)) {
                userFeature.setUid(Integer.parseInt(userInfo.getUid()));
                userFeature.getFeatures().put(U_ID, uid);
            }
            Integer sex = userInfo.getSex();
            if (sex != null) {
                userFeature.getFeatures().put(U_SEX, sex.toString());
            }
            Map<String, BigDecimal> level3Hobby = userInfo.getLevel3Hobby();
            if (level3Hobby != null && level3Hobby.size() > 0) {
                StringBuilder level3HobbyStrBuilder = new StringBuilder();
                for (Map.Entry<String, BigDecimal> temp : level3Hobby.entrySet()) {
                    level3HobbyStrBuilder.append(temp.getKey()).append(":").append(temp.getValue()).append(",");
                }
                String level3HobbyStr = level3HobbyStrBuilder.substring(0, level3HobbyStrBuilder.length() - 1);
                userFeature.getFeatures().put(U_C3_PREFER, level3HobbyStr);
            }
            String buySuppliers = userInfo.getBuySuppliers();
            if (StringUtils.isNotEmpty(buySuppliers)) {
                userFeature.getFeatures().put(U_OS, buySuppliers);
            }
            String collectSuppliers = userInfo.getCollectSuppliers();
            if (StringUtils.isNotEmpty(collectSuppliers)) {
                userFeature.getFeatures().put(U_FS, collectSuppliers);
            }
            String season = userInfo.getSeason();
            if (StringUtils.isNotEmpty(season)) {
                userFeature.getFeatures().put(U_SEASON, season);
            }
        } catch (Exception e) {
            log.error("[严重异常]构造用户特征失败，参数：{}，异常信息:", JSONObject.toJSONString(request), e);
        }
        ThreadLocalFeature.USER_FEATURE.set(userFeature);
    }

    /**
     * 从UC(用户中心)获取用户基本信息
     *
     * @param request productsearch接口的请求入参
     * @return 返回uc获取的基本用户信息
     */
    private User getUserInfo(SearchRequest request) {
        CommonRequestParam commonParam = request.getCommonParam();
        Integer uid = commonParam.getUid();
        String uuid = commonParam.getUuid();
        if (uuid != null) {
            UserRequest userRequest = new UserRequest();
            if (uid != null) {
                userRequest.setUid(uid.toString());
            }
            userRequest.setUuid(uuid);
            userRequest.setCaller(SEARCH);
            userRequest.setFields(Lists.newArrayList(SEX, SEASON, LEVEL_3_HOBBY, BUY_SUPPLIERS, COLLECT_SUPPLIERS));
            ApiResult<User> ucResult = null;
            try {
                ucResult = ucServerService.query(userRequest);
                if (ucResult == null || !ResultCodeMsg.SUCCESS_CODE.equals(ucResult.getCode())) {
                    log.error("[严重异常][dubbo异常]uc服务（接口UcServerService#query(param)）调用失败，param:{},结果：{}", JSONObject.toJSONString(userRequest), JSONObject.toJSONString(ucResult));
                    return null;
                }
                return ucResult.getData();
            } catch (Exception e) {
                log.error("[严重异常][dubbo异常]uc服务获取用户信息（接口UcServerService#query(param)）失败,param:{},异常信息：", JSONObject.toJSONString(userRequest), e);
            }
        }
        return null;
    }
}
