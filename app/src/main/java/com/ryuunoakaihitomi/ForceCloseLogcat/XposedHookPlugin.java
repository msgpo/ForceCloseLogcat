package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.app.ActivityManager;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

//https://github.com/wasdennnoch/Scoop

public class XposedHookPlugin implements IXposedHookLoadPackage {

    static final String INTENT_ACTION = "tk.wasdennnoch.scoop.EXCEPTION";
    static final String INTENT_PACKAGE_NAME = "pkg";
    static final String INTENT_TIME = "time";
    static final String INTENT_STACKTRACE = "stacktrace";
    static final String INTENT_PID = "pid";
    private static final String TAG = "FC scoop";
    private static String mPkg;
    private Application mApplication;
    private boolean mSent;
    private final XC_MethodHook uncaughtExceptionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {
            if (mSent) {
                Log.d(TAG, "uncaughtExceptionHook (" + mPkg + "): Broadcast already sent");
                return;
            }
            Log.d(TAG, "uncaughtExceptionHook (" + mPkg + "): Sending broadcast");
            Throwable t = (Throwable) param.args[1];
            String description = t.toString();
            Intent intent = new Intent(INTENT_ACTION)
                    .setClassName(XposedHookPlugin.class.getPackage().getName(), LogOperaBcReceiver.class.getName())
                    .putExtra(INTENT_PACKAGE_NAME, mApplication.getPackageName())
                    .putExtra(INTENT_TIME, System.currentTimeMillis())
                    .putExtra(INTENT_STACKTRACE, Log.getStackTraceString(t))
                    .putExtra(INTENT_PID, String.valueOf(getPIDFromContext(mApplication)));
            XposedBridge.log(TAG + " StackTrace Description: " + description);
            // Just send everything here because it costs no performance (well, technically
            // it does, but the process is about to die anyways, so I don't care).
            // Also I have no idea how to detect custom subclasses efficiently.
            //mApplication.sendBroadcast(intent);
            mSent = true; // Doesn't need to be reset as process dies soon
            // A ThreadDeath Error gets thrown to force stop a worker thread.
            // It is a "normal occurrence" and does not make apps crash.
            if (!description.startsWith(ThreadDeath.class.getName()))
                mApplication.sendBroadcast(intent);
        }
    };
    private final XC_MethodHook setUncaughtExceptionHandlerHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) {

            if (param.args[0] != null)
                hookUncaughtException(param.args[0].getClass());
        }
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) {
        mPkg = lpparam.packageName;
        if (mPkg.equals("android"))
            return;
        XposedHelpers.findAndHookConstructor(Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) {
                mApplication = (Application) param.thisObject;
                mSent = false;
            }
        });

        if (mPkg.equals(getClass().getPackage().getName())) {
            // don't use xxx.class here,it won't take effect
            XposedHelpers.findAndHookMethod("com.ryuunoakaihitomi.ForceCloseLogcat.ConfigUI", lpparam.classLoader, "isXposedActive", XC_MethodReplacement.returnConstant(true));
            return;
        }
        XposedHelpers.findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(ThreadGroup.class, "uncaughtException", Thread.class, Throwable.class, uncaughtExceptionHook);
        // Gets initialized in between native application creation, handleLoadPackage gets called after native creation
        hookUncaughtException(Thread.getDefaultUncaughtExceptionHandler().getClass());
    }

    private void hookUncaughtException(Class<?> clazz) {
        int i = 0;
        do { // Search through superclasses
            try {
                XposedHelpers.findAndHookMethod(clazz, "uncaughtException", Thread.class, Throwable.class, uncaughtExceptionHook);
                Log.d(TAG, "hookUncaughtException (" + mPkg + "): Hooked class " + clazz.getName() + " after " + i + " loops");
                return;
            } catch (Throwable ignore) {
            }
            i++;
        } while ((clazz = clazz.getSuperclass()) != null);
        Log.d(TAG, "hookUncaughtException (" + mPkg + "): No class found to hook!");
    }

    private int getPIDFromContext(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        assert activityManager != null;
        for (ActivityManager.RunningAppProcessInfo info : activityManager.getRunningAppProcesses())
            if (info.processName.equals(context.getPackageName()))
                return info.pid;
        return -1;
    }
}