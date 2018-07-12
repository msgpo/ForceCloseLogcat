package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.Manifest;
import android.app.Activity;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Process;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ConfigMgr.getBoolean(ConfigMgr.Options.FIRST_RUN)) {
            //noinspection ConstantConditions
            if (checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, Process.myPid(), Process.myUid()) != PackageManager.PERMISSION_GRANTED
                    || ((AppOpsManager) getSystemService(APP_OPS_SERVICE)).checkOpNoThrow(AppOpsManager.OPSTR_WRITE_EXTERNAL_STORAGE, Process.myUid(), getPackageName()) != AppOpsManager.MODE_ALLOWED)
                Utils.simpleToast(getApplicationContext(), getString(R.string.need_wes_perm_notice), false, true);
            startActivity(new Intent(MyApplication.getContext(), ConfigUI.class));
            startService(new Intent(this, FCLogService.class));
        } else {
            Utils.simpleToast(this, getString(R.string.welcome), true, true);
            startActivity(new Intent(MyApplication.getContext(), Help.class));
        }
        finish();
    }
}
