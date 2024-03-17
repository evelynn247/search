package com.biyao.search.as.server.common.util;

import com.biyao.dclog.service.DCLogger;
import com.biyao.search.as.server.common.consts.CommonConsts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author zhaiweixi@idstaff.com
 * @date 2019/11/25
 **/
public class DcLogUtil {

    private static final Logger logger = LoggerFactory.getLogger(DcLogUtil.class);

    /**
     * 搜索排序日志DcLog
     */
    private static final DCLogger SEARCHAS_LOG = DCLogger.getLogger(CommonConsts.TAG_SEARCHAS_RANK_DETAIL);

    private static final DCLogger SEARCH_DETAIL_LOG = DCLogger.getLogger(CommonConsts.SEARCH_DETAIL_LOG);


    public static void printRankDetail(String logType, String logBody){
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("lt=").append(logType);
            sb.append("\tlv=1.0\t").append(logBody);
            SEARCHAS_LOG.printDCLog(sb.toString());
        }catch (Exception e){
            logger.error("[一般异常][dclog异常]打印dcLog失败:lt=[{}], body=[{}]", logType, logBody, e);
        }
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
        SEARCH_DETAIL_LOG.printDCLog(sb.toString());
    }
}
