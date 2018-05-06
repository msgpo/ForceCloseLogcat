package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 广播接收器：开机启动
 */

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        boolean isNoAutoRun = ConfigMgr.getBoolean(ConfigMgr.Options.NO_AUTO_RUN);
        Log.d(TAG, "onReceive: isNoAutoRun:" + isNoAutoRun);
        if (intent != null && Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && !isNoAutoRun)
            context.startService(new Intent(context, FCLogService.class));
    }
}
