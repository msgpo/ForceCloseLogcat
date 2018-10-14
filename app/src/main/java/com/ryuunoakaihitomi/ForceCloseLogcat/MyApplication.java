package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Process;
import android.os.StrictMode;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;

public class MyApplication extends Application implements Thread.UncaughtExceptionHandler {
    private static final String TAG = "MyApplication";
    @SuppressLint("StaticFieldLeak")
    private static Context context;

    public static Context getContext() {
        return context;
    }

    @Override
    public void onCreate() {
        //严格模式
        StrictMode.ThreadPolicy threadPolicy = new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build();
        if (isDebuggable())
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder(threadPolicy)
                    .penaltyDialog()
                    .penaltyFlashScreen()
                    .build());
        StrictMode.setThreadPolicy(threadPolicy);
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        super.onCreate();
        Log.i(TAG, "onCreate: PID:" + Process.myPid() + " LANG=" + getLanguage());
        Thread.setDefaultUncaughtExceptionHandler(this);
        context = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                //noinspection ResultOfMethodCallIgnored
                new File(FCLogService.LOG_DIR).mkdirs();
            }
        }).start();
    }

    //Chash监听
    @Override
    public void uncaughtException(Thread t, Throwable e) {
        final int CRASH_UI_FREEZE_DELAY = 1000;
        Log.e(TAG, "uncaughtException:" + t.getId(), e);
        FCLogInfoBridge.setFcPackageName(this.getClass().getPackage().getName());
        FCLogInfoBridge.setFcTime(String.valueOf(System.currentTimeMillis()));
        TxtFileIO.W(FCLogService.LOG_DIR + "/" + FCLogInfoBridge.getFcTime() + "_MyCrash.log",
                "EnvInfo:\n" + RuntimeEnvInfo.get(context) + "\nFCLog:\n"
                        + Log.getStackTraceString(e) + "\nFinalLog:\n" + FCLogInfoBridge.log);
        if (isDebuggable())
            SystemClock.sleep(CRASH_UI_FREEZE_DELAY);
        Process.killProcess(Process.myPid());
    }

    /**
     * 判断本应用是否为debug包
     *
     * @return boolean
     */
    boolean isDebuggable() {
        return (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
    }

    String getLanguage() {
        Configuration configuration = getApplicationContext().getResources().getConfiguration();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            return configuration.getLocales().get(0).getLanguage();
        else
            //noinspection deprecation
            return configuration.locale.getLanguage();
    }
}
