package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.util.Log;

/**
 * FC信息桥：只包括Getter/Setter和调试日志输出的Java Bean
 */
public class FCLogInfoBridge {
    private static final String TAG = "FCLogInfoBridge";
    private static String
            logPath,
            fcPackageName,
            fcPID,
            fcTime;

    public static String getLogPath() {
        Log.v(TAG, "getLogPath: " + logPath);
        return logPath;
    }

    public static void setLogPath(String logPath) {
        Log.v(TAG, "setLogPath: " + logPath);
        FCLogInfoBridge.logPath = logPath;
    }

    public static String getFcPackageName() {
        Log.v(TAG, "getFcPackageName: " + fcPackageName);
        return fcPackageName;
    }

    public static void setFcPackageName(String fcPackageName) {
        Log.v(TAG, "setFcPackageName: " + fcPackageName);
        FCLogInfoBridge.fcPackageName = fcPackageName;
    }

    public static String getFcPID() {
        Log.v(TAG, "getFcPID: " + fcPID);
        return fcPID;
    }

    public static void setFcPID(String fcPID) {
        Log.v(TAG, "setFcPID: " + fcPID);
        FCLogInfoBridge.fcPID = fcPID;
    }

    public static String getFcTime() {
        Log.v(TAG, "getFcTime: " + fcTime);
        return fcTime;
    }

    public static void setFcTime(String fcTime) {
        Log.v(TAG, "setFcTime: " + fcTime);
        FCLogInfoBridge.fcTime = fcTime;
    }
}