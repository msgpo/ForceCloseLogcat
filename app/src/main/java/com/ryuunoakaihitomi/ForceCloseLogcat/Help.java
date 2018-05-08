package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static android.app.AlertDialog.THEME_DEVICE_DEFAULT_DARK;

/**
 * 活动：帮助
 */

public class Help extends Activity {
    private final String DONATE_LINK = "http://ryuunoakaihitomi.info/donate/";
    private final String DOWNLOAD_LINK = "https://github.com/ryuunoakaihitomi/ForceCloseLogcat/releases";

    /**
     * 将输入流转为字符串
     *
     * @param in 待转换的输入流
     * @return 转换后的字符串
     */
    private static String inputStream2String(InputStream in) {
        String str = "", encode = "utf-8";
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, encode));
            StringBuilder sb = new StringBuilder();
            while ((str = reader.readLine()) != null)
                sb.append(str).append("\n");
            return sb.toString();
        } catch (Exception ignored) {
        }
        return str;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.transparentStatusBar(this);
        Builder builder =
                new Builder(Help.this, THEME_DEVICE_DEFAULT_DARK)
                        .setTitle(R.string.help)
                        .setMessage(inputStream2String(getResources().openRawResource(R.raw.help_body)))
                        .setPositiveButton(R.string.donate, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openURL(DONATE_LINK);
                            }
                        })
                        .setNegativeButton(R.string.dl_link, new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                openURL(DOWNLOAD_LINK);
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                startActivity(new Intent(Help.this, ConfigUI.class));
                                finish();
                            }
                        });
        if (!ConfigMgr.getBoolean(ConfigMgr.Options.FIRST_RUN)) {
            ConfigMgr.setBoolean(ConfigMgr.Options.FIRST_RUN, true);
            ConfigMgr.saveAll();
            builder.setNeutralButton(R.string.start_service, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    startService(new Intent(Help.this, FCLogService.class));
                    finish();
                }
            });
        } else
            builder.setNeutralButton(R.string.copy_adb_cmd, new OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Utils.copyToClipboard("adb shell pm grant com.ryuunoakaihitomi.ForceCloseLogcat android.permission.READ_LOGS");
                    finish();
                }
            });
        builder.show();
        Utils.simpleToast(this, String.format(getString(R.string.help_toast), Utils.getAppVersionName(getPackageName()), getString(R.string.help_update_time)), false, false);
    }

    void openURL(String link) {
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
        finish();
    }
}