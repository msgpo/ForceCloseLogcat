package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.os.Process;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;

public class MyApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "MyApplication";
    @SuppressLint("StaticFieldLeak")
    static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "onCreate: PID:" + Process.myPid());
        Thread.setDefaultUncaughtExceptionHandler(this);
        context = getApplicationContext();
        //noinspection ResultOfMethodCallIgnored
        new File(FCLogService.LOG_DIR).mkdirs();
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        final int CRASH_UI_FREEZE_DELAY = 1000;
        Log.e(TAG, "uncaughtException:" + t.getId(), e);
        FCLogInfoBridge.setFcPackageName(this.getClass().getPackage().getName());
        FCLogInfoBridge.setFcTime(String.valueOf(System.currentTimeMillis()));
        TxtFileIO.W(FCLogService.LOG_DIR + "/" + FCLogInfoBridge.getFcTime() + "_MyCrash.log",
                "EnvInfo:\n" + RuntimeEnvInfo.get() + "\nFCLog:\n"
                        + Log.getStackTraceString(e));
        SystemClock.sleep(CRASH_UI_FREEZE_DELAY);
        Process.killProcess(Process.myPid());
    }
}
