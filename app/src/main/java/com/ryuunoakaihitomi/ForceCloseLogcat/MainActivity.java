package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Process;
import android.provider.Settings;
import android.util.Log;

import java.util.Objects;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity {
    private final int REQUEST_PERMISSION_CODE = 1;
    private final int REQUEST_IGNORE_BATTERY_CODE = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Utils.transparentStatusBar(this);
        if (ConfigMgr.getBoolean(ConfigMgr.Options.FIRST_RUN)) {
            //java.lang.IllegalArgumentException: Unknown operation string: android:write_external_storage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //noinspection ConstantConditions
                if (checkPermission(WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                        || ((AppOpsManager) getSystemService(APP_OPS_SERVICE)).checkOpNoThrow(AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED)
                    Utils.simpleToast(this, getString(R.string.need_wes_perm_notice), false, true);
                //Android 8.1 Toast overlap
                if (!Objects.requireNonNull(getSystemService(PowerManager.class)).isIgnoringBatteryOptimizations(getPackageName()))
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1)
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                Utils.simpleToast(MainActivity.this, getString(R.string.battery_opti_warn), false, true);
                            }
                        }, 2000);
                    else
                        Utils.simpleToast(MainActivity.this, getString(R.string.battery_opti_warn), false, true);
            }
            startActivity(new Intent(this, ConfigUI.class));
            startService(new Intent(this, FCLogService.class));
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                else
                    requestIgnoreBatteryOptimizations();
            else
                welcome();
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(MainActivity.class.getSimpleName(), "onRequestPermissionsResult: granted");
                requestIgnoreBatteryOptimizations();
            } else {
                if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                    Log.e("MainActivity", "onRequestPermissionsResult: !=PackageManager.PERMISSION_GRANTED");
                    //重复请求权限
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                } else {
                    Log.e(getClass().getSimpleName(), "onRequestPermissionsResult: !shouldShowRequestPermissionRationale");
                    requestIgnoreBatteryOptimizations();
                }
            }
        }
    }

    //欢迎，弹出帮助
    private void welcome() {
        Utils.simpleToast(this, getString(R.string.welcome), true, true);
        startActivity(new Intent(this, Help.class));
        finish();
    }

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("ConstantConditions")
    @SuppressLint("BatteryLife")
    private void requestIgnoreBatteryOptimizations() {
        if (!getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName()))
            try {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, REQUEST_IGNORE_BATTERY_CODE);
            } catch (ActivityNotFoundException e) {
                //听说某些第三方阉割电池优化
                e.printStackTrace();
                welcome();
            }
        else
            welcome();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IGNORE_BATTERY_CODE) {
            if (resultCode == RESULT_OK)
                Log.v(toString(), "succeeded");
            else
                Log.e(toString(), "onActivityResult: requestIgnoreBatteryOptimizations failed");
            welcome();
        }
    }
}
