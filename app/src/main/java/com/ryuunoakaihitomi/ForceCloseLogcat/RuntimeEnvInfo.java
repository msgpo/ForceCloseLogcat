package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.os.Build;

import java.util.Objects;

/**
 * 环境信息记录
 */

public class RuntimeEnvInfo {
    public static String get() {
        String infoBody = "";
        infoBody += "crash time=" + FCLogInfoBridge.getFcTime() + "\n";
        infoBody += "model=" + Build.MODEL + "\n";
        infoBody += "android version=" + Build.VERSION.RELEASE + "(" + Build.VERSION.SDK_INT + ")\n";
        infoBody += "brand=" + Build.BRAND + "\n";
        infoBody += "manufacturer=" + Build.MANUFACTURER + "\n";
        infoBody += "board=" + Build.BOARD + "\n";
        infoBody += "hardware=" + Build.HARDWARE + "\n";
        infoBody += "device=" + Build.DEVICE + "\n";
        if (!Objects.requireNonNull(Utils.getAppVersionName(FCLogInfoBridge.getFcPackageName())).isEmpty()) {
            infoBody += "version name=" + Utils.getAppVersionName(FCLogInfoBridge.getFcPackageName()) + "\n";
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            infoBody += "supported abis=" + stringArrayToString(Build.SUPPORTED_ABIS, " & ") + "\n";
        } else {
            infoBody += "cpu abi=" + Build.CPU_ABI + "\n";
            infoBody += "cpu abi2=" + Build.CPU_ABI2 + "\n";
        }
        infoBody += "display=" + Build.DISPLAY;
        return infoBody;
    }

    private static String stringArrayToString(String[] in, @SuppressWarnings("SameParameterValue") String dot) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < in.length; i++) {
            if (i == in.length - 1) {
                out.append(in[i]);
            } else {
                out.append(in[i]).append(dot);
            }
        }
        return out.toString();
    }
}
