package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_ENVINFO;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_NOTICE_ID;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_PATH;

/**
 * 广播接收器：日志操作
 */

@SuppressWarnings("WeakerAccess")
public class LogOperaBcReceiver {
    private static final String TAG = "LogOperaBcReceiver";
    static String logPath, envInfo;
    private static final String header = LogOperaBcReceiver.class.getPackage().getName() + ".";
    static String
            EXACT_COPY = header + "copy",
            EXACT_SHARE = header + "share",
            EXACT_DELETE = header + "delete",
            EXACT_SLIDE = header + "slide";
    private static String[] actions = {EXACT_COPY, EXACT_DELETE, EXACT_SHARE, EXACT_SLIDE};
    private static BroadcastReceiver[] broadcastReceivers = {
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
}
