package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

/**
 * 活动：设置和白名单编辑器界面
 */

public class ConfigUI extends Activity {
    AlertDialog.Builder alertDialogBuilder;
    private static final String TAG = "ConfigUI";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.transparentStatusBar(this);
        //初始化白名单列表，防止JSON异常
        if (ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST).isEmpty()) {
            ConfigMgr.setString(ConfigMgr.Options.WHITE_LIST, new JSONObject().toString());
            ConfigMgr.saveAll();
        }
        //一级管理：设置
        alertDialogBuilder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle(R.string.settings)
                .setMultiChoiceItems(new String[]
                        {getString(R.string.do_not_auto_run), getString(R.string.quiet_mode), getString(R.string.one_noti), getString(R.string.white_list)}, new boolean[]{
                        ConfigMgr.getBoolean(ConfigMgr.Options.NO_AUTO_RUN),
                        ConfigMgr.getBoolean(ConfigMgr.Options.QUIET_MODE),
                        ConfigMgr.getBoolean(ConfigMgr.Options.ONE_NOTICE),
                        ConfigMgr.getBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH),
                }, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        switch (which) {
                            case 0:
                                ConfigMgr.setBoolean(ConfigMgr.Options.NO_AUTO_RUN, isChecked);
                                break;
                            case 1:
                                ConfigMgr.setBoolean(ConfigMgr.Options.QUIET_MODE, isChecked);
                                break;
                            case 2:
                                ConfigMgr.setBoolean(ConfigMgr.Options.ONE_NOTICE, isChecked);
                                break;
                            case 3:
                                ConfigMgr.setBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH, isChecked);
                                break;
                        }
                    }
                })
                .setNeutralButton(R.string.white_list_editor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //二级管理：白名单编辑器
                        Utils.simpleToast(ConfigUI.this, getString(R.string.processing_please_wait), false, true);
                        /*  https://github.com/aosp-mirror/platform_frameworks_base/blob/master/services/core/java/com/android/server/notification/NotificationManagerService.java
                        static final int SHORT_DELAY = 2000; // 2 seconds
                        土司持续时间
                        */
                        //不再设1000,因为枚举速度跟不上会导致土司过早退出
                        final int TOAST_SHOW_DELAY = 300;
                        new Timer().schedule(new TimerTask() {
                            @Override
                            public void run() {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        final JSONObject appListSaver = new JSONObject();
                                        final PackageManager manager = getPackageManager();
                                        List<PackageInfo> infos = manager.getInstalledPackages(0);
                                        //根据当前区域按照标签排列。注意：这个的实现让我卡了一下。关键词：Collections.sort，Collator
                                        Collections.sort(infos, new Comparator<PackageInfo>() {
                                            @Override
                                            public int compare(PackageInfo o1, PackageInfo o2) {
                                                return Collator.getInstance(Locale.getDefault())
                                                        .compare(o1.applicationInfo.loadLabel(manager).toString(), o2.applicationInfo.loadLabel(manager).toString());
                                            }
                                        });
                                        int appCount = infos.size();
                                        Log.i(TAG, "run: appCount:" + appCount);
                                        final String[] appName = new String[appCount], appList = new String[appCount], appDetails = new String[appCount];
                                        boolean[] cfgShow = new boolean[appCount];
                                        for (int i = 0; i < infos.size(); i++) {
                                            PackageInfo info = infos.get(i);
                                            boolean isSysApp = (info.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
                                            try {
                                                //初始化配置列表
                                                appListSaver.put(info.packageName, new JSONObject(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST)).optBoolean(info.packageName));
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                            //应用标签
                                            appName[i] = info.applicationInfo.loadLabel(manager).toString();
                                            //包名类别
                                            appDetails[i] = info.packageName + "\n" + (isSysApp ? getString(R.string.sys_app) : getString(R.string.usr_app));
                                            appList[i] = info.packageName;
                                            cfgShow[i] = appListSaver.optBoolean(info.packageName);
                                        }
                                        alertDialogBuilder.setMultiChoiceItems(appName, cfgShow, new DialogInterface.OnMultiChoiceClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                                                try {
                                                    appListSaver.put(appList[which], isChecked);
                                                } catch (JSONException e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        })
                                                .setNeutralButton(null, null)
                                                .setNegativeButton(null, null)
                                                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        ConfigMgr.setString(ConfigMgr.Options.WHITE_LIST, appListSaver.toString());
                                                        ConfigMgr.saveAll();
                                                        Utils.simpleToast(ConfigUI.this, getString(R.string.saved), false, false);
                                                        exitAndPreventWindowLeaked(dialog);
                                                    }
                                                })
                                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                                    @Override
                                                    public void onCancel(DialogInterface dialog) {
                                                        recreate();
                                                    }
                                                });
                                        final AlertDialog mainDialogCreate = alertDialogBuilder.create();
                                        mainDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                                            @Override
                                            public void onShow(DialogInterface dialog) {
                                                ListView listView = mainDialogCreate.getListView();
                                                //长按显示细节
                                                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                                    @Override
                                                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                                        Utils.simpleToast(ConfigUI.this, appDetails[position], false, false);
                                                        return false;
                                                    }
                                                });
                                            }
                                        });
                                        mainDialogCreate.show();
                                    }
                                });
                            }
                        }, TOAST_SHOW_DELAY);
                    }
                })
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConfigMgr.saveAll();
                        Utils.simpleToast(ConfigUI.this, getString(R.string.saved), false, false);
                        exitAndPreventWindowLeaked(dialog);
                    }
                })
                .setNegativeButton(R.string.help, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitAndPreventWindowLeaked(dialog);
                        startActivity(new Intent(ConfigUI.this, Help.class));
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        exitAndPreventWindowLeaked(dialog);
                    }
                });
        //调试：My Crash
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                throw new Error("MyCrash Test");
            }
        });
    }

    //防止窗体泄露
    void exitAndPreventWindowLeaked(DialogInterface dialog) {
        dialog.dismiss();
        finish();
    }
}
