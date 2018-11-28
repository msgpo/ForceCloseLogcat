package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 封装类：对logcat -v threadtime输出的日志进行的解析
 * 日志示例："04-30 04:37:25.233  1486  1486 I chatty  : uid=1000 system_server expire 1 line"
 */

//使用动态代理捕获异常

interface LogObjMethods {
    String getDate();

    String getTime();

    String getPID();

    String getTID();

    String getLevel();

    String getTag();

    String getRaw();
}

class ExceptionCatcher implements InvocationHandler {

    private static final String TAG = "ExceptionCatcher";

    private Object instance;

    ExceptionCatcher(Object instance) {
        this.instance = instance;
    }

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        try {
            return method.invoke(instance, objects);
        } catch (IllegalAccessException | InvocationTargetException e) {
            if (e instanceof InvocationTargetException) {
                Throwable rootException = e.getCause();
                Log.e(TAG, "Exception caught...", rootException);
                if (rootException instanceof ArrayIndexOutOfBoundsException) {
                    return templateBuilder(method, objects);
                } else {
                    Log.e(TAG, "other cause... ", rootException);
                    throw rootException;
                }
            } else {
                Log.e(TAG, "invoke: ", e);
                throw e;
            }
        }
    }

    //标准模板构建
    private Object templateBuilder(Method method, Object[] objects) {
        //标准格式日志对象
        final String LOG_LINE_TEMPLATE = "04-30 04:37:25.233  1486  1486 I chatty  : uid=1000 system_server expire 1 line";
        try {
            return method.invoke(new LogObject(LOG_LINE_TEMPLATE), objects);
        } catch (IllegalAccessException | InvocationTargetException e) {
            //不存在
            Log.e(TAG, "templateBuilder: ???", e);
        }
        return null;
    }
}

@SuppressWarnings("CanBeFinal")
class LogObject implements LogObjMethods {

    @SuppressWarnings("FieldCanBeLocal")
    private final String
            SPLIT_REGEX = " +",
            RAW_SEPARATOR = ": ",
            LONG_TAG_SEPARATOR = ":";
    private String srcLog;
    private String[] logSplit;

    LogObject(String threadTimeLog) {
        srcLog = threadTimeLog;
        logSplit = srcLog.split(SPLIT_REGEX);
    }

    @Override
    public String getDate() {
        return logSplit[0];
    }

    /**
     * @return 时分秒
     */
    @Override
    public String getTime() {
        return logSplit[1];
    }

    public String getPID() {
        return logSplit[2];
    }

    @Override
    public String getTID() {
        return logSplit[3];
    }

    @Override
    public String getLevel() {
        return logSplit[4];
    }

    @Override
    public String getTag() {
        String tagRaw = logSplit[5];
        int separatorPos = tagRaw.length() - LONG_TAG_SEPARATOR.length();
        return LONG_TAG_SEPARATOR.equals(tagRaw.substring(separatorPos))
                ? tagRaw.substring(0, separatorPos)
                : tagRaw;
    }


    /**
     * @return 取得日志正文
     */
    @Override
    public String getRaw() {
        return srcLog.substring(srcLog.indexOf(RAW_SEPARATOR) + RAW_SEPARATOR.length());
    }
}
