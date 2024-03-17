package com.biyao.search.bs.server.common.util;

import com.biyao.dclog.service.DCLogger;

import java.util.Map;

import com.biyao.search.bs.server.common.consts.CommonConsts;
import com.biyao.search.bs.service.model.request.MatchRequest;
import org.apache.commons.lang.StringUtils;

/**
 * @author 邹立强 (zouliqiang@idstaff.com)
 * @version V1.0
 * @Description dclog 工具类
 * @date 2019年6月25日下午4:57:15
 */
public class DclogUtil {

    private final static String SEARCH_BS_EXP = "search_bs_exp";

    private final static String SEARCH_DETAIL_LOG = "search_detail_log";

    private static DCLogger bsExpLogger = DCLogger.getLogger(SEARCH_BS_EXP);
    private static DCLogger searchDetailLog = DCLogger.getLogger(SEARCH_DETAIL_LOG);

    /**
     * @param dcLogger
     * @param map
     * @return void
     * @Description 打印日志
     * @version V1.0
     * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public static void sendDclog(DCLogger dcLogger, Map<String, Object> map) {
        StringBuffer logStr = new StringBuffer();
        for (Map.Entry<String, Object> m : map.entrySet()) {
            String key = m.getKey();
            Object obj = m.getValue();
            String value = obj == null ? "" : obj.toString();
            logStr.append(key).append("=").append(value).append("\t");
        }
        String logString = logStr.toString();
        if (StringUtils.isNotBlank(logString)) {
            logString.substring(0, logString.length() - 1);

        }
        dcLogger.printDCLog(logString);
    }

    /**
     * @param dcLogger
     * @param
     * @return void
     * @Description 打印日志
     * @version V1.0
     * @auth 邹立强 (zouliqiang@idstaff.com)
     */
    public static void sendDclog(DCLogger dcLogger, String logstr) {

        dcLogger.printDCLog(logstr);
    }

    /**
     * 搜索BS服务实验日志
     */
    public static void sendBsExp(String logBody) {
        bsExpLogger.printDCLog(logBody);
    }

    public static void sendBsExp(MatchRequest request) {
        // 构造bs实验日志
        StringBuilder logBody = new StringBuilder();
        logBody.append("lt=").append(SEARCH_BS_EXP);
        logBody.append("\tlv=1.0");
        logBody.append("\tuu=").append(request.getCommonParam().getUuid());
        logBody.append("\tu=");
        if (request.getCommonParam().getUid() != null){
            logBody.append(request.getCommonParam().getUid());
        }
        logBody.append("\tpf=").append(request.getCommonParam().getPlatform().getName());
        logBody.append("\tstid=").append(request.getCommonParam().getPlatform().getNum());
        logBody.append("\tsid=").append(request.getCommonParam().getSid());
        logBody.append("\texpid=");
        if (request.getExpIds() != null){
            logBody.append(CommonConsts.underLineJoiner.join(request.getExpIds()));
        }
        bsExpLogger.printDCLog(logBody.toString());
    }

    /**
     * search_detail_log日志
     */
    public static void printSearchDetailLog(String title,String uuid,String sid ,String logBody){

            StringBuilder sb = new StringBuilder(10240);
            sb.append("title=").append(title);
            sb.append("\tuuid=").append(uuid);
            if(sid == null){
                sb.append("\tsid=");
            }else{
                sb.append("\tsid=").append(sid);
            }
            sb.append("\tdetail=").append(logBody);
            searchDetailLog.printDCLog(sb.toString());
    }
}
