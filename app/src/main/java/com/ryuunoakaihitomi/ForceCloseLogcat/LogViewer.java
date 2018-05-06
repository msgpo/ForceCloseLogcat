package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

import java.lang.reflect.Field;

public class LogViewer extends Activity {
    static final String
            EXTAG_PATH = "path",
            EXTAG_ENVINFO = "envinfo",
            EXTAG_NOTICE_ID = "noti_id";
    private static final String TAG = "LogViewer";
    String path, envInfo;
    AlertDialog dialog;

    @SuppressWarnings("JavaReflectionMemberAccess")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Dialog);
        path = getIntent().getStringExtra(EXTAG_PATH);
        envInfo = getIntent().getStringExtra(EXTAG_ENVINFO);
        String logBody = TxtFileIO.R(path);
        final String separator = "  /////// ";
        String message = separator + getString(R.string.log_path) + "\n" +
                path + "\n" +
                separator + getString(R.string.env_info) + "\n" +
                envInfo + "\n" +
                separator + getString(R.string.log_body) + "\n" +
                logBody;
        dialog = new AlertDialog.Builder(LogViewer.this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(R.string.log_reader)
                .setMessage(message)
                .setPositiveButton(R.string.share, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(new Intent(LogOperaBcReceiver.EXACT_SHARE)
                                .putExtra(LogViewer.EXTAG_PATH, path)
                                .putExtra(LogViewer.EXTAG_ENVINFO, envInfo));
                        finish();
                    }
                })
                .setNegativeButton(R.string.copy, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(new Intent(LogOperaBcReceiver.EXACT_COPY)
                                .putExtra(LogViewer.EXTAG_PATH, path)
                                .putExtra(LogViewer.EXTAG_ENVINFO, envInfo));
                        finish();
                    }
                })
                .setNeutralButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(new Intent(LogOperaBcReceiver.EXACT_DELETE)
                                .putExtra(LogViewer.EXTAG_PATH, path));
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
                .create();
        dialog.show();
        try {
            Field mAlert = AlertDialog.class.getDeclaredField("mAlert");
            mAlert.setAccessible(true);
            Object mAlertController = mAlert.get(dialog);
            Field mMessageView = mAlertController.getClass().getDeclaredField("mMessageView");
            mMessageView.setAccessible(true);
            TextView textView = (TextView) mMessageView.get(mAlertController);
            textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            textView.setTextColor(Color.GREEN);
        } catch (IllegalAccessException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        boolean isScreenOn;
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        assert powerManager != null;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            isScreenOn = powerManager.isInteractive();
        } else
            isScreenOn = powerManager.isScreenOn();
        Log.i(TAG, "onPause: isScreenOn:" + isScreenOn);
        if (isScreenOn)
            dialog.dismiss();
    }
}
