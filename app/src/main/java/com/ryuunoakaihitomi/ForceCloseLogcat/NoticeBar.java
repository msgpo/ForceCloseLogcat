package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;

import java.util.Objects;

/**
 * 通知管理
 */

public class NoticeBar {
    private static final String TAG = "NoticeBar";
    private static int id = Integer.MIN_VALUE;

    private static Intent operationBaseIntent(Context context, String whichAction, String envInfo, int id) {
        return new Intent(whichAction)
                .setPackage(context.getPackageName())
                .putExtra(LogViewer.EXTAG_PATH, FCLogInfoBridge.getLogPath())
                .putExtra(LogViewer.EXTAG_ENVINFO, envInfo)
                .putExtra(LogViewer.EXTAG_NOTICE_ID, id);
    }

    public static Notification serviceStart(Context c) {
        String fsChannelName = c.getString(R.string.foreground_srv_channel_name);
        String fsChannelId = "FClog.serviceStart";
        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(fsChannelId, fsChannelName, NotificationManager.IMPORTANCE_LOW);
            //noinspection ConstantConditions
            c.getSystemService(NotificationManager.class).createNotificationChannel(notificationChannel);
            builder = new Notification.Builder(c, fsChannelId);
            builder.setBadgeIconType(Notification.BADGE_ICON_SMALL);
        } else
            builder = new Notification.Builder(c);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            builder.setTicker(c.getString(R.string.service_name) + c.getString(R.string.running));
        builder
                .setContentTitle(c.getString(R.string.service_name))
                .setContentText(c.getString(R.string.running))
                .setContentIntent(PendingIntent.getActivity(c, 0, new Intent(c, ConfigUI.class), 0))
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher);
        Intent killService = new Intent(c, FCLogService.class).setAction(FCLogService.KILL_SIGNAL).setPackage(c.getPackageName());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
            //Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS  开发者选项 intent action
            builder.addAction(new Notification.Action(0, c.getString(R.string.developer_option), PendingIntent.getActivity(c, 0, new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), 0)));
            builder.addAction(new Notification.Action(0, c.getString(R.string.kill_service), PendingIntent.getService(c, 0, killService, 0)));
        } else {
            builder.addAction(0, c.getString(R.string.developer_option), PendingIntent.getActivity(c, 0, new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS), 0));
            builder.addAction(0, c.getString(R.string.kill_service), PendingIntent.getService(c, 0, killService, 0));
        }
        return builder.build();
    }

    public static void onFCFounded(Context c) {
        String crChannelName = c.getString(R.string.crash_report_channel_name);
        String crChannelId = "FClog.onFCFounded";
        Notification.BigTextStyle bigTextStyle = new Notification.BigTextStyle();
        boolean isLogEmpty = TextUtils.isEmpty(TxtFileIO.R(FCLogInfoBridge.getLogPath()));
        if (isLogEmpty)
            bigTextStyle.bigText(c.getText(R.string.null_log_body));
        else
            bigTextStyle.bigText(TxtFileIO.R(FCLogInfoBridge.getLogPath()));
        NotificationManager notificationManager = (NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE);
        assert notificationManager != null;
        int nid;
        if (ConfigMgr.getBoolean(ConfigMgr.Options.ONE_NOTICE)) {
            nid = Integer.MAX_VALUE;
            //Android N会自动折叠通知
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
                //cancelAll()并不会清除前台服务通知
                notificationManager.cancelAll();
        } else
            nid = ++id;
        //防止多次取环境信息
        String envInfo = RuntimeEnvInfo.get(c);
        PendingIntent pendingIntent = PendingIntent.getActivity(c, nid, new Intent(c, LogViewer.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS | Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .setData(Uri.parse("custom://" + System.currentTimeMillis()))
                .putExtra(LogViewer.EXTAG_PATH, FCLogInfoBridge.getLogPath())
                .putExtra(LogViewer.EXTAG_ENVINFO, envInfo)
                .putExtra(LogViewer.EXTAG_NOTICE_ID, nid), PendingIntent.FLAG_UPDATE_CURRENT);
        String appName = getProgramNameByPackageName(c, FCLogInfoBridge.getFcPackageName());
        Notification.Builder builder = new Notification.Builder(c);
        if (appName == null)
            appName = FCLogInfoBridge.getFcPackageName();
        else {
            try {
                Drawable icon = c.getPackageManager().getApplicationIcon(FCLogInfoBridge.getFcPackageName());
                if (icon instanceof BitmapDrawable)
                    builder.setLargeIcon(((BitmapDrawable) icon).getBitmap());
                else {
                    //AdaptiveIconDrawable?
                    Bitmap bitmap = Bitmap.createBitmap(icon.getIntrinsicWidth(), icon.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    icon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                    icon.draw(canvas);
                    builder.setLargeIcon(bitmap);
                }
            } catch (PackageManager.NameNotFoundException e) {
                //不可能转到这个逻辑
                Log.e(TAG, "onFCFounded: IMPOSSIBLE", e);
            }
        }
        builder
                .setWhen(0)
                .setContentTitle(c.getString(R.string.fc_found))
                .setSubText(String.format(c.getString(R.string.fcnoti_subtext), appName, FCLogInfoBridge.getFcPID()))
                .setContentText(FCLogInfoBridge.getFcTime())
                .setContentIntent(pendingIntent)
                .setStyle(bigTextStyle)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX);
        Intent copy = operationBaseIntent(c, LogOperaBcReceiver.EXACT_COPY, envInfo, nid),
                delete = operationBaseIntent(c, LogOperaBcReceiver.EXACT_DELETE, envInfo, nid),
                share = operationBaseIntent(c, LogOperaBcReceiver.EXACT_SHARE, envInfo, nid),
                slide = operationBaseIntent(c, LogOperaBcReceiver.EXACT_SLIDE, envInfo, nid);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            builder.setColor(Color.RED);
        if (!isLogEmpty)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH)
                builder.addAction(new Notification.Action(0, c.getString(R.string.copy), PendingIntent.getBroadcast(c, nid, copy, PendingIntent.FLAG_UPDATE_CURRENT)))
                        .addAction(new Notification.Action(0, c.getString(R.string.delete), PendingIntent.getBroadcast(c, nid, delete, PendingIntent.FLAG_UPDATE_CURRENT)))
                        .addAction(new Notification.Action(0, c.getString(R.string.share), PendingIntent.getBroadcast(c, nid, share, PendingIntent.FLAG_UPDATE_CURRENT)));
            else
                builder.addAction(0, c.getString(R.string.delete), PendingIntent.getBroadcast(c, nid, delete, PendingIntent.FLAG_UPDATE_CURRENT))
                        .addAction(0, c.getString(R.string.copy), PendingIntent.getBroadcast(c, nid, copy, PendingIntent.FLAG_UPDATE_CURRENT))
                        .addAction(0, c.getString(R.string.share), PendingIntent.getBroadcast(c, nid, share, PendingIntent.FLAG_UPDATE_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(crChannelId, crChannelName, NotificationManager.IMPORTANCE_DEFAULT);
            Objects.requireNonNull(c.getSystemService(NotificationManager.class)).createNotificationChannel(notificationChannel);
            builder.setChannelId(crChannelId);
            builder.setBadgeIconType(Notification.BADGE_ICON_NONE);
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            builder.setTicker(c.getString(R.string.fc_found) + " -> " + appName);
        builder.setDeleteIntent(PendingIntent.getBroadcast(c, 0, slide, PendingIntent.FLAG_UPDATE_CURRENT));
        Notification notification = builder.build();
        notificationManager.notify(nid, notification);
        Log.d(TAG, "onFCFounded: end id:" + id + " nid:" + nid);
    }

    private static String getProgramNameByPackageName(Context context, String packageName) {
        PackageManager pm = context.getPackageManager();
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
