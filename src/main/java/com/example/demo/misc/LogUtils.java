package com.example.demo.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogUtils {

    public static final String GENERAL = "GEN";
    public static final String SECURITY = "SEC";

    public static final String SUCCESS_MARKER = "OK";
    public static final String FAILURE_MARKER = "NG";

    private LogUtils() {
        // not instantiable
    }

    public static Logger getLogger(Class<?> cls, String tag) {
        return LoggerFactory.getLogger(tag + "> " + cls.getSimpleName());
    }

    public static Logger getLogger(Class<?> cls) {
        return getLogger(cls, GENERAL);
    }

    public static String logSuccess(String msg) {
        return SUCCESS_MARKER + " " + msg;
    }

    public static String logFailure(String msg) {
        return FAILURE_MARKER + " " + msg;
    }

    public static String logSuccessFailure(boolean condition, String msg) {
        return condition ? logSuccess(msg) : logFailure(msg);
    }
}
