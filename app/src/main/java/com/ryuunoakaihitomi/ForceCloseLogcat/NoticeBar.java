package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.util.Objects;

/**
 * 通知管理
 */

public class NoticeBar {
    private static final String TAG = "NoticeBar";
    @SuppressLint("StaticFieldLeak")
    private static Context c = MyApplication.getContext();
    private static int id = Integer.MIN_VALUE;
    @SuppressWarnings("FieldCanBeLocal")
    private static String SSchannelId = "FClog.ncids",
            SSchannelName = "ForceCloseLogcat ServiceKeeper",
            FCchannelId = "FClog.ncidf",
            FCchannelName = "ForceCloseLogcat FCCrashReport";

    private static Intent operationBaseIntent(String whichAction) {
        return new Intent(whichAction)
                .putExtra(LogViewer.EXTAG_PATH, FCLogInfoBridge.getLogPath())
                .putExtra(LogViewer.EXTAG_ENVINFO, RuntimeEnvInfo.get())
                .putExtra(LogViewer.EXTAG_NOTICE_ID, id);
    }

    public static Notification serviceStart() {
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(SSchannelId, SSchannelName, NotificationManager.IMPORTANCE_LOW);
            //noinspection ConstantConditions
            c.getSystemService(NotificationManager.class).createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(c, SSchannelId);
        } else
            builder = new Notification.Builder(c);
        builder
                .setContentTitle(c.getString(R.string.service_name))
                .setContentText(c.getString(R.string.running))
                .setContentIntent(PendingIntent.getActivity(c, 0, new Intent(c, ConfigUI.class), 0))
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);
        Intent killService = new Intent(c, FCLogService.class).setAction(FCLogService.KILL_SIGNAL);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            builder.addAction(new Notification.Action(0, c.getString(R.string.kill_service), PendingIntent.getService(c, 0, killService, 0)));
        else
            builder.addAction(0, c.getString(R.string.kill_service), PendingIntent.getService(c, 0, killService, 0));
        return builder.build();
    }

    public static void onFCFounded() {
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        bigTextStyle.bigText(TxtFileIO.R(FCLogInfoBridge.getLogPath()));
        int nid;
        if (ConfigMgr.getBoolean(ConfigMgr.Options.ONE_NOTICE))
            nid = Integer.MAX_VALUE;
        else
            nid = ++id;
        PendingIntent pendingIntent = PendingIntent.getActivity(c, nid, new Intent(c, LogViewer.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(Uri.parse("custom://" + System.currentTimeMillis()))
                .putExtra(LogViewer.EXTAG_PATH, FCLogInfoBridge.getLogPath())
                .putExtra(LogViewer.EXTAG_ENVINFO, RuntimeEnvInfo.get())
                .putExtra(LogViewer.EXTAG_NOTICE_ID, nid), PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = new Notification.Builder(c)
                .setWhen(0)
                .setContentTitle(c.getString(R.string.fc_found))
                .setSubText(String.format(c.getString(R.string.fcnoti_subtext)
                        , getProgramNameByPackageName(FCLogInfoBridge.getFcPackageName()), FCLogInfoBridge.getFcPID()))
                .setContentText(FCLogInfoBridge.getFcTime())
                .setContentIntent(pendingIntent)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX);
        Intent copy = operationBaseIntent(LogOperaBcReceiver.EXACT_COPY),
                delete = operationBaseIntent(LogOperaBcReceiver.EXACT_DELETE),
                share = operationBaseIntent(LogOperaBcReceiver.EXACT_SHARE),
                slide = operationBaseIntent(LogOperaBcReceiver.EXACT_SLIDE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.RED);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
            builder.addAction(new Notification.Action(0, c.getString(R.string.copy), PendingIntent.getBroadcast(c, nid, copy, PendingIntent.FLAG_UPDATE_CURRENT)))
                    .addAction(new Notification.Action(0, c.getString(R.string.delete), PendingIntent.getBroadcast(c, nid, delete, PendingIntent.FLAG_UPDATE_CURRENT)))
                    .addAction(new Notification.Action(0, c.getString(R.string.share), PendingIntent.getBroadcast(c, nid, share, PendingIntent.FLAG_UPDATE_CURRENT)));
        else
            builder.addAction(0, c.getString(R.string.delete), PendingIntent.getBroadcast(c, nid, delete, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addAction(0, c.getString(R.string.copy), PendingIntent.getBroadcast(c, nid, copy, PendingIntent.FLAG_UPDATE_CURRENT))
                    .addAction(0, c.getString(R.string.share), PendingIntent.getBroadcast(c, nid, share, PendingIntent.FLAG_UPDATE_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(FCchannelId, FCchannelName, NotificationManager.IMPORTANCE_DEFAULT);
            Objects.requireNonNull(c.getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
            builder.setChannelId(FCchannelId);
        }
        builder.setDeleteIntent(PendingIntent.getBroadcast(c, 0, slide, PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        notificationManager.notify(nid, notification);
        Log.d(TAG, "onFCFounded: end id:" + id);
    }

    private static String getProgramNameByPackageName(String packageName) {
        PackageManager pm = MyApplication.getContext().getPackageManager();
        String name = null;
        try {
            name = pm.getApplicationLabel(
                    pm.getApplicationInfo(packageName,
                            PackageManager.GET_META_DATA)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return name;
    }
}
