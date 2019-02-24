package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

/**
 * 其他工具类：非要在两个类中使用的共有方法会在这里
 */

class Utils {

    /**
     * 模板Toast
     *
     * @param context  语境
     * @param text     显示文本
     * @param islong   是否是长toast
     * @param isCenter 是否显示在中心位置
     */
    static void simpleToast(Context context, String text, boolean islong, boolean isCenter) {
        Toast toast = Toast.makeText(context, text, islong ? Toast.LENGTH_LONG : Toast.LENGTH_SHORT);
        if (isCenter) toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();
    }

    /**
     * 使状态栏透明
     *
     * @param activity 要渲染的活动
     */
    static void transparentStatusBar(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = activity.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
            w.setStatusBarColor(Color.TRANSPARENT);
        }
    }

    /**
     * 取应用版本名
     *
     * @param context     上下文
     * @param PackageName 包名
     * @return 版本名String
     */
    static String getAppVersionName(Context context, String PackageName) {
        String versionName = "";
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(PackageName, 0);
            versionName = packageInfo.versionName;
            if (versionName == null || versionName.length() <= 0) {
                return null;
            }
        } catch (Exception ignored) {
        }
        return versionName;
    }

    /**
     * 复制到剪贴板
     *
     * @param context 上下文
     * @param text    要复制的文本
     */
    static void copyToClipboard(Context context, String text) {
        ((ClipboardManager) Objects.requireNonNull(context.getSystemService(Context.CLIPBOARD_SERVICE)))
                .setPrimaryClip(ClipData.newPlainText(null, text));
    }

    /**
     * 如黑色对话框主题常量
     *
     * @return 整型数值
     */
    static int getDarkDialogTheme() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ?
                android.R.style.Theme_DeviceDefault_Dialog_Alert : AlertDialog.THEME_DEVICE_DEFAULT_DARK;
    }

    /**
     * https://blog.csdn.net/qq331710168/article/details/85320098
     * 伪装成系统Toast以躲避通知权限检查...
     * 调用影响全局
     */
    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("PrivateApi")
    static void defineSystemToast() {
        try {
            Method getServiceMethod = Toast.class.getDeclaredMethod("getService");
            getServiceMethod.setAccessible(true);
            final Object iNotificationManagerObj = getServiceMethod.invoke(null);
            Class iNotificationManagerCls = Class.forName("android.app.INotificationManager");
            Object iNotificationManagerProxy = Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(), new Class[]{iNotificationManagerCls}, new InvocationHandler() {
                @Override
                public Object invoke(Object o, Method method, Object[] objects) throws Throwable {

                    //强制使用系统Toast
                    if ("enqueueToast".equals(method.getName())
                            || "enqueueToastEx".equals(method.getName())) {  //华为p20 pro上为enqueueToastEx
                        objects[0] = "android";
                    }
                    return method.invoke(iNotificationManagerObj, objects);
                }
            });
            Field sServiceField = Toast.class.getDeclaredField("sService");
            sServiceField.setAccessible(true);
            sServiceField.set(null, iNotificationManagerProxy);
        } catch (Exception ignored) {
        }
    }
}
