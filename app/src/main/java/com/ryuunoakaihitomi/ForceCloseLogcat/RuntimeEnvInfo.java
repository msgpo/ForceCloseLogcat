package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 环境信息记录
 */

public class RuntimeEnvInfo {
    private static final String TAG = "RuntimeEnvInfo";

    public static String get(Context context) {
        String infoBody = "";
        infoBody += "crash time=" + FCLogInfoBridge.getFcTime() + "\n";
        infoBody += "model=" + Build.MODEL + "\n";
        infoBody += "android version=" + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n";
        infoBody += "brand=" + Build.BRAND + "\n";
        infoBody += "manufacturer=" + Build.MANUFACTURER + "\n";
        infoBody += "board=" + Build.BOARD + "\n";
        infoBody += "hardware=" + Build.HARDWARE + "\n";
        infoBody += "device=" + Build.DEVICE + "\n";
        if (!Objects.requireNonNull(Utils.getAppVersionName(context, FCLogInfoBridge.getFcPackageName())).isEmpty()) {
            infoBody += "version name=" + Utils.getAppVersionName(context, FCLogInfoBridge.getFcPackageName()) + "\n";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            infoBody += "supported abis=" + stringArrayToString(Build.SUPPORTED_ABIS, " & ") + "\n";
        } else {
            infoBody += "cpu abi=" + Build.CPU_ABI + "\n";
            infoBody += "cpu abi2=" + Build.CPU_ABI2 + "\n";
        }
        infoBody += "display=" + Build.DISPLAY;
        //构建信息原始数据
        Log.i(TAG, getRawBuildEnvInfo());
        return infoBody;
    }

    private static String stringArrayToString(String[] in, String dot) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        for (String string : in) {
            i++;
            out.append(string).append(i == in.length ? "" : dot);
        }
        return out.toString();
    }

    private static String getRawBuildEnvInfo() {
        Map<String, Object> map = new HashMap<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            map.put("SUPPORTED_ABIS", stringArrayToString(Build.SUPPORTED_ABIS, " "));
            map.put("SUPPORTED_32_BIT_ABIS", stringArrayToString(Build.SUPPORTED_32_BIT_ABIS, " "));
            map.put("SUPPORTED_64_BIT_ABIS", stringArrayToString(Build.SUPPORTED_64_BIT_ABIS, " "));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            map.put("VERSION.BASE_OS", Build.VERSION.BASE_OS);
            map.put("VERSION.PREVIEW_SDK_INT", Build.VERSION.PREVIEW_SDK_INT);
            map.put("VERSION.SECURITY_PATCH", Build.VERSION.SECURITY_PATCH);
        }
        map.put("VERSION.CODENAME", Build.VERSION.CODENAME);
        map.put("VERSION.INCREMENTAL", Build.VERSION.INCREMENTAL);
        map.put("VERSION.RELEASE", Build.VERSION.RELEASE);
        map.put("VERSION.SDK_INT", Build.VERSION.SDK_INT);
        map.put("getRadioVersion", Build.getRadioVersion());
        map.put("BOARD", Build.BOARD);
        map.put("BOOTLOADER", Build.BOOTLOADER);
        map.put("BRAND", Build.BRAND);
        map.put("DEVICE", Build.DEVICE);
        map.put("DISPLAY", Build.DISPLAY);
        map.put("FINGERPRINT", Build.FINGERPRINT);
        map.put("HARDWARE", Build.HARDWARE);
        map.put("HOST", Build.HOST);
        map.put("ID", Build.ID);
        map.put("MANUFACTURER", Build.MANUFACTURER);
        map.put("MODEL", Build.MODEL);
        map.put("PRODUCT", Build.PRODUCT);
        map.put("TAGS", Build.TAGS);
        map.put("TYPE", Build.TYPE);
        map.put("USER", Build.USER);
        map.put("TIME", Build.TIME);
        return "android.os.Build. " + map.toString();
    }
}
