package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * 配置管理器：SharedPreferences管理
 */

class ConfigMgr {
    private static final String CFG_FILENAME = "config";
    private static final String TAG = "ConfigMgr";
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;

    static {
        sharedPreferences = MyApplication.getContext().getSharedPreferences(CFG_FILENAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    //boolean值IO

    static void setBoolean(String key, boolean val) {
        Log.d(TAG, "setBoolean: key:" + key + " val:" + val);
        editor.putBoolean(key, val);
    }

    static boolean getBoolean(String key) {
        boolean ret = sharedPreferences.getBoolean(key, false);
        Log.d(TAG, "getBoolean: key:" + key + " ret:" + ret);
        return ret;
    }

    //字符串类型保存的是很长一串json，因此不打日志

    @SuppressWarnings("SameParameterValue")
    static void setString(String key, String val) {
        editor.putString(key, val);
    }

    @SuppressWarnings("SameParameterValue")
    static String getString(String key) {
        return sharedPreferences.getString(key, "");
    }

    static void saveAll() {
        Log.v(TAG, "saveAll: apply");
        editor.apply();
    }

    //选项
    class Options {
        static final String
                FIRST_RUN = "1strun",
                NO_AUTO_RUN = "!atorun",
                QUIET_MODE = "quiet",
                ONE_NOTICE = "1noti",
                WHITE_LIST = "whitelist",
                WHITE_LIST_SWITCH = "wlswi",
                XPOSED = "xposed";
    }
}
