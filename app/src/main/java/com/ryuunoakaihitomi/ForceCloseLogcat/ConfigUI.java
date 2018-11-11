package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.AdapterView;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * 活动：设置和白名单编辑器界面
 */

public class ConfigUI extends Activity {
    private AlertDialog.Builder alertDialogBuilder;
    private static final String TAG = "ConfigUI";
    private AlertDialog dialog;

    @SuppressWarnings("SameReturnValue")
    static boolean isXposedActive() {
        return false;
    }

    //防止窗体泄露
    @Override
    protected void onDestroy() {
        super.onDestroy();
        dialog.dismiss();
        dialog = null;
    }

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
        alertDialogBuilder = new AlertDialog.Builder(this, Utils.getDarkDialogTheme())
                .setTitle(R.string.settings)
                .setMultiChoiceItems(new String[]
                        {getString(R.string.do_not_auto_run), getString(R.string.quiet_mode), getString(R.string.one_noti), getString(R.string.white_list), getString(R.string.xposed_switch)}, new boolean[]{
                        ConfigMgr.getBoolean(ConfigMgr.Options.NO_AUTO_RUN),
                        ConfigMgr.getBoolean(ConfigMgr.Options.QUIET_MODE),
                        ConfigMgr.getBoolean(ConfigMgr.Options.ONE_NOTICE),
                        ConfigMgr.getBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH),
                        ConfigMgr.getBoolean(ConfigMgr.Options.XPOSED)
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
                            case 4:
                                if (isXposedActive())
                                    ConfigMgr.setBoolean(ConfigMgr.Options.XPOSED, isChecked);
                                else {
                                    ConfigMgr.setBoolean(ConfigMgr.Options.XPOSED, false);
                                    if (isChecked)
                                        Utils.simpleToast(ConfigUI.this, getString(R.string.xposed_offline), false, true);
                                }
                                break;
                        }
                    }
                })
                .setNeutralButton(R.string.white_list_editor, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final ProgressDialog loadingDialog = new ProgressDialog(ConfigUI.this, Utils.getDarkDialogTheme());
                        loadingDialog.setTitle(getString(R.string.processing_please_wait));
                        loadingDialog.setMessage(getString(R.string.executing_step_n));
                        loadingDialog.setCancelable(false);
                        loadingDialog.show();
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final JSONObject appListSaver = new JSONObject();
                                final PackageManager manager = getPackageManager();
                                TrueTimingLogger logger = new TrueTimingLogger(TAG, "app list");
                                List<PackageInfo> infos = manager.getInstalledPackages(0);
                                logger.addSplit("get list info");
                                //删除自身选项
                                for (PackageInfo myInfo : infos)
                                    if (myInfo.packageName.equals(getPackageName())) {
                                        infos.remove(myInfo);
                                        break;
                                    }
                                logger.addSplit("delete me");
                                //根据当前区域按照标签排列
                                Collections.sort(infos, new Comparator<PackageInfo>() {
                                    @Override
                                    public int compare(PackageInfo o1, PackageInfo o2) {
                                        return Collator.getInstance(Locale.getDefault())
                                                .compare(o1.applicationInfo.loadLabel(manager).toString(), o2.applicationInfo.loadLabel(manager).toString());
                                    }
                                });
                                logger.addSplit("sort");
                                int appCount = infos.size();
                                Log.i(TAG, "run: appCount:" + appCount + "+1");
                                final String[] appName = new String[appCount], appList = new String[appCount], appDetails = new String[appCount];
                                boolean[] cfgShow = new boolean[appCount];
                                for (int i = 0; i < appCount; i++) {
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
                                    appDetails[i] = (appName[i].equals(appList[i] = info.packageName) ? "" : appList[i] + "\n") + (isSysApp ? getString(R.string.sys_app) : getString(R.string.usr_app));
                                    cfgShow[i] = appListSaver.optBoolean(appList[i]);
                                }
                                logger.addSplit("get config");
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
                                                finish();
                                            }
                                        })
                                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                            @Override
                                            public void onCancel(DialogInterface dialog) {
                                                //'0' means "put on end of execution queue"
                                                new Handler().postDelayed(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        setSecureFlag(false);
                                                        recreate();
                                                    }
                                                }, 0);
                                            }
                                        });
                                logger.dumpToLog();
                                logger.reset();
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        final AlertDialog mainDialogCreate = alertDialogBuilder.create();
                                        mainDialogCreate.setOnShowListener(new DialogInterface.OnShowListener() {
                                            @Override
                                            public void onShow(DialogInterface dialog) {
                                                //长按显示细节
                                                mainDialogCreate.getListView().setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                                                    @Override
                                                    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                                                        Utils.simpleToast(ConfigUI.this, appDetails[position], false, false);
                                                        return true;
                                                    }
                                                });
                                            }
                                        });
                                        loadingDialog.dismiss();
                                        setSecureFlag(true);
                                        mainDialogCreate.show();
                                    }
                                });
                            }
                        }).start();
                    }
                })
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ConfigMgr.saveAll();
                        Utils.simpleToast(ConfigUI.this, getString(R.string.saved), false, false);
                        finish();
                    }
                })
                .setNegativeButton(R.string.help_btn, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(ConfigUI.this, Help.class));
                        finish();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                });
        //调试：My Crash
        dialog = alertDialogBuilder.create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                throw new Error("MyCrash Test");
            }
        });
        //删除日志目录
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                TrueTimingLogger logger = new TrueTimingLogger(TAG, "delete dir");
                TxtFileIO.D(FCLogService.LOG_DIR);
                logger.addSplit("must invoke addSplit()");
                logger.dumpToLog();
                Utils.simpleToast(ConfigUI.this, getString(R.string.deleted), false, false);
                return true;
            }
        });
    }

    void setSecureFlag(boolean isSecure) {
        int flag = WindowManager.LayoutParams.FLAG_SECURE;
        if (isSecure)
            getWindow().setFlags(flag, flag);
        else
            getWindow().clearFlags(flag);
    }
}
