package com.biyao.search.as.server.feature.threadlocal;

import com.biyao.search.as.server.feature.model.ContextFeature;
import com.biyao.search.as.server.feature.model.UserFeature;

/**
 * @author: xiafang
 * @date: 2019/11/18
 */
public class ThreadLocalFeature implements AutoCloseable {
    //public final static ThreadLocal<String> SID = new ThreadLocal<String>();
    public final static ThreadLocal<String> SID = ThreadLocal.withInitial(()-> "");
    //public final static ThreadLocal<UserFeature> USER_FEATURE = new ThreadLocal<UserFeature>();
    public final static ThreadLocal<UserFeature> USER_FEATURE = ThreadLocal.withInitial(()-> new UserFeature());
    //public final static ThreadLocal<ContextFeature> CONTEXT_FEATURE = new ThreadLocal<ContextFeature>();
    public final static ThreadLocal<ContextFeature> CONTEXT_FEATURE = ThreadLocal.withInitial(()-> new ContextFeature());
    //public final static ThreadLocal<Boolean> IS_WHITE_LIST_UUID = new ThreadLocal<Boolean>();
    public final static ThreadLocal<Boolean> IS_WHITE_LIST_UUID = ThreadLocal.withInitial(()-> false);

    @Override
    public void close() {
        SID.remove();
        USER_FEATURE.remove();
        CONTEXT_FEATURE.remove();
        IS_WHITE_LIST_UUID.remove();
    }

    public static void manualClose() {
        SID.remove();
        USER_FEATURE.remove();
        CONTEXT_FEATURE.remove();
        IS_WHITE_LIST_UUID.remove();
    }
}
