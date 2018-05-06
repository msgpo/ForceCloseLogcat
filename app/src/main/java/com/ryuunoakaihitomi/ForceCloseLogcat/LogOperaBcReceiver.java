package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.util.Objects;

import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_ENVINFO;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_NOTICE_ID;
import static com.ryuunoakaihitomi.ForceCloseLogcat.LogViewer.EXTAG_PATH;

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
                    logPath = intent.getStringExtra(EXTAG_PATH);
                    envInfo = intent.getStringExtra(EXTAG_ENVINFO);
                    ((ClipboardManager) Objects.requireNonNull(MyApplication.getContext().getSystemService(Context.CLIPBOARD_SERVICE)))
                            .setPrimaryClip(ClipData.newPlainText(null, packageLog()));
                    Utils.simpleToast(MyApplication.getContext(), MyApplication.getContext().getString(R.string.copied_info), false, false);
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

    static void reg() {
        for (int i = 0; i < broadcastReceivers.length; i++) {
            IntentFilter filter = new IntentFilter();
            filter.addAction(actions[i]);
            MyApplication.getContext().registerReceiver(broadcastReceivers[i], filter);
        }
    }

    static void unreg() {
        for (BroadcastReceiver broadcastReceiver : broadcastReceivers) {
            MyApplication.getContext().unregisterReceiver(broadcastReceiver);
        }
    }

    static String packageLog() {
        return "#######RuntimeEnvironmentInformation#######\n" +
                envInfo +
                "\n#######ForceCloseCrashLog#######\n" +
                TxtFileIO.R(logPath);
    }
}
