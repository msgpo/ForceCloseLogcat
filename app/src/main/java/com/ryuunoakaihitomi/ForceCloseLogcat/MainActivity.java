package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (ConfigMgr.getBoolean(ConfigMgr.Options.FIRST_RUN)) {
            startActivity(new Intent(MyApplication.getContext(), ConfigUI.class));
            startService(new Intent(this, FCLogService.class));
        } else {
            Utils.simpleToast(this, getString(R.string.welcome), true, true);
            startActivity(new Intent(MyApplication.getContext(), Help.class));
        }
        finish();
    }
}
