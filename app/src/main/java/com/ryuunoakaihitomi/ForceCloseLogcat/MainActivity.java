package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity extends Activity {
    private final int REQUEST_PERMISSION_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ConfigMgr.getBoolean(ConfigMgr.Options.FIRST_RUN)) {
            //noinspection ConstantConditions
            if (checkPermission(WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                    || ((AppOpsManager) getSystemService(APP_OPS_SERVICE)).checkOpNoThrow(AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED)
                Utils.simpleToast(getApplicationContext(), getString(R.string.need_wes_perm_notice), false, true);
            startActivity(new Intent(this, ConfigUI.class));
            startService(new Intent(this, FCLogService.class));
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                if (checkSelfPermission(WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                else
                    welcome();
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
                welcome();
            } else {
                if (shouldShowRequestPermissionRationale(WRITE_EXTERNAL_STORAGE)) {
                    Log.e("MainActivity", "onRequestPermissionsResult: !=PackageManager.PERMISSION_GRANTED");
                    //重复请求权限
                    requestPermissions(new String[]{WRITE_EXTERNAL_STORAGE}, REQUEST_PERMISSION_CODE);
                } else {
                    Log.e(getClass().getSimpleName(), "onRequestPermissionsResult: !shouldShowRequestPermissionRationale");
                    welcome();
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
}
