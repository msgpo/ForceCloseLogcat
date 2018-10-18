package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Objects;

import static com.ryuunoakaihitomi.ForceCloseLogcat.FCLogService.LOG_DIR;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_ENVINFO;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_NOTICE_ID;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_PATH;

/**
 * 广播接收器：日志操作
 */

@SuppressWarnings("WeakerAccess")
public class LogOperaBcReceiver extends BroadcastReceiver {
    private static final String TAG = "LogOperaBcReceiver";
    static String logPath, envInfo;
    private static final String header = LogOperaBcReceiver.class.getPackage().getName() + ".";
    final static String
            EXACT_COPY = header + "copy",
            EXACT_SHARE = header + "share",
            EXACT_DELETE = header + "delete",
            EXACT_SLIDE = header + "slide";
    private static final String[] actions = {EXACT_COPY, EXACT_DELETE, EXACT_SHARE, EXACT_SLIDE};
    private static final BroadcastReceiver[] broadcastReceivers = {
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    collapseStatusBar(context);
                    logPath = intent.getStringExtra(EXTAG_PATH);
                    envInfo = intent.getStringExtra(EXTAG_ENVINFO);
                    Utils.simpleToast(context, context.getString(R.string.copied_info), false, false);
                    Utils.copyToClipboard(context, packageLog());
                }
            },
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    logPath = intent.getStringExtra(EXTAG_PATH);
                    TxtFileIO.D(logPath);
                    ((NotificationManager) Objects.requireNonNull(context.getSystemService(Context.NOTIFICATION_SERVICE))).cancel(intent.getIntExtra(EXTAG_NOTICE_ID, -1));
                }
            },
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    collapseStatusBar(context);
                    logPath = intent.getStringExtra(EXTAG_PATH);
                    envInfo = intent.getStringExtra(EXTAG_ENVINFO);
                    Intent intentSend = new Intent(Intent.ACTION_SEND);
                    intentSend.setType("text/plain");
                    intentSend.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.application_fc_log));
                    intentSend.putExtra(Intent.EXTRA_TEXT, packageLog());
                    intentSend.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(Intent.createChooser(intentSend, context.getString(R.string.send_log_to_developer))
                            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                }
            },
            new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    logPath = intent.getStringExtra(EXTAG_PATH);
                    Log.w(TAG, "onReceive: DeleteIntent:(EXACT_SLIDE) logPath" + logPath);
                }
            },
    };

    static void reg(Context context) {
        for (int i = 0; i < broadcastReceivers.length; i++) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(actions[i]);
            context.registerReceiver(broadcastReceivers[i], filter);
        }
    }

    @SuppressWarnings("SpellCheckingInspection")
    static void unreg(Context context) {
        for (BroadcastReceiver broadcastReceiver : broadcastReceivers) {
            context.unregisterReceiver(broadcastReceiver);
        }
    }

    static String packageLog() {
        return "#######RuntimeEnvironmentInformation#######\n" +
                envInfo +
                "\n#######ForceCloseCrashLog#######\n" +
                TxtFileIO.R(logPath);
    }

    /**
     * 折叠状态栏
     *
     * @param context 带有android.permission.EXPAND_STATUS_BAR权限的上下文
     */
    static void collapseStatusBar(Context context) {
        //https://android.googlesource.com/platform/prebuilts/runtime/+/master/appcompat/hiddenapi-light-greylist.txt
        @SuppressWarnings("SpellCheckingInspection") @SuppressLint("WrongConstant") Object statusBarService = context.getSystemService("statusbar");
        try {
            @SuppressLint("PrivateApi") Class<?> statusBarMgrClz = Class.forName("android.app.StatusBarManager");
            Method collapsePanels = statusBarMgrClz.getMethod("collapsePanels");
            collapsePanels.invoke(statusBarService);
        } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    //Xposed模块捕捉的crash记录
    @SuppressLint("SimpleDateFormat")
    @Override
    public void onReceive(Context context, Intent intent) {
        //筛选
        if (!Objects.equals(intent.getAction(), XposedHookPlugin.INTENT_ACTION)) {
            Log.w(TAG, "onReceive: Invalid intent.");
            return;
        }
        if (!isMyServiceRunning(context, FCLogService.class)) {
            Log.w(TAG, "onReceive: FCLogService is dead.");
            return;
        }
        if (!ConfigUI.isXposedActive()) {
            Log.w(TAG, "onReceive: Xposed plugin may be unavailable.");
            return;
        }
        if (!ConfigMgr.getBoolean(ConfigMgr.Options.XPOSED)) {
            Log.w(TAG, "onReceive: The value of ConfigMgr.Options.XPOSED is false.");
            return;
        }
        String packageName = intent.getStringExtra(XposedHookPlugin.INTENT_PACKAGE_NAME);
        boolean
                isAppInWhiteList = false,
                isWhiteListAvailable = ConfigMgr.getBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH);
        try {
            isAppInWhiteList = new JSONObject(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST)).optBoolean(packageName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if (isWhiteListAvailable && isAppInWhiteList) {
            Log.w(TAG, "onReceive: " + packageName + " is on the white list,and the white list is available.");
            return;
        }
        //装载
        Log.i(TAG, "onReceive: Received the crash info reported by Xposed plug-in.");
        FCLogInfoBridge.setFcPackageName(packageName);
        FCLogInfoBridge.setFcPID(intent.getStringExtra(XposedHookPlugin.INTENT_PID));
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(intent.getLongExtra(XposedHookPlugin.INTENT_TIME, System.currentTimeMillis()));
        FCLogInfoBridge.setFcTime(time);
        String path = LOG_DIR + "/" + time + "_" + FCLogInfoBridge.getFcPackageName() + ".log";
        // minSdkVersion 19
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT)
            path = path.replace(':', '.');
        //写入
        TxtFileIO.W(path, intent.getStringExtra(XposedHookPlugin.INTENT_STACKTRACE));
        FCLogInfoBridge.setLogPath(path);
        //通知
        if (!ConfigMgr.getBoolean(ConfigMgr.Options.QUIET_MODE))
            NoticeBar.onFCFounded(context);
        else
            FCLogService.震える(context);
    }

    /**
     * 检查自身服务是否在运行
     *
     * @param context      Context
     * @param serviceClass 服务类
     * @return boolean
     */
    @SuppressWarnings("SameParameterValue")
    private boolean isMyServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert manager != null;
        //public List<ActivityManager.RunningServiceInfo> getRunningServices (int maxNum)
        // This method was deprecated in API level 26.
        // As of Build.VERSION_CODES.O, this method is no longer available to third party applications.
        // For backwards compatibility, it will still return the caller's own services.
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
            if (serviceClass.getName().equals(service.service.getClassName()))
                return true;
        return false;
    }
}
