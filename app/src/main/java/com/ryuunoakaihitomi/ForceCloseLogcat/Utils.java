package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.IOException;

public class Utils {
    /**
     * 检查是否获得root权限
     *
     * @return 如已获得root权限，返回为真，反之为假
     */
    public static synchronized boolean isRoot() {
        Process process = null;
        DataOutputStream os = null;
        try {
            process = Runtime.getRuntime().exec("su");
            os = new DataOutputStream(process.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            return false;
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                assert process != null;
                process.destroy();
            } catch (Exception ignored) {
            }
        }
    }

    static synchronized void cmd(String command, boolean isRoot) {
        try {
            Process p;
            if (isRoot) {
                p = Runtime.getRuntime().exec("su");
            } else {
                p = Runtime.getRuntime().exec("sh");
            }
            DataOutputStream d = new DataOutputStream(p.getOutputStream());
            d.writeBytes(command + "\n");
            d.writeBytes("exit\n");
            d.flush();
            /*OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream(), "UTF-8");
            osw.write(command + "\n");
            osw.write("exit\n");
            osw.flush();*/
            p.getErrorStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 基本模板Toast
     *
     * @param context  语境
     * @param text     显示文本
     * @param islong   是否是长toast
     * @param isCenter 是否显示在中心位置
     */
    public static void simpleToast(Context context, String text, boolean islong, boolean isCenter) {
        Toast toast = Toast.makeText(context, text, islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        if (isCenter) toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * 使状态栏透明
     *
     * @param activity 要渲染的活动
     */
    public static void transparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            activity.getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    static String getAppVersionName(String PackageName) {
        String versionName = "";
        try {
            PackageManager packageManager = MyApplication.getContext().getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(PackageName, 0);
            versionName = packageInfo.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return null;
            }
        } catch (Exception ignored) {
        }
        return versionName;
    }
}
