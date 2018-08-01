package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Calendar;

/**
 * 服务：核心，主要功能实现
 */

public class FCLogService extends Service implements Runnable {
    @SuppressWarnings("ConstantConditions")
    static final String LOG_DIR = MyApplication.getContext().getExternalFilesDir("FClog").getPath();
    static final String KILL_SIGNAL = "killAction";
    static final int NOTICE_ID = 1989;
    private static final String TAG = "FCLogService";
    //分钟接收器：检查权限
    BroadcastReceiver tickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            isTickReceived = true;
            if (!checkLogPerm())
                onPermissionDenied();
        }
    };
    private boolean isRoot, isAlive, isTickReceived;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        isRoot = Utils.isRoot();
        isAlive = true;
        cleanLog();
        //权限检查与试图开启
        Log.i(TAG, "onCreate: READ_LOGS perm granted:" + checkLogPerm() + " isRoot:" + isRoot);
        if (!isRoot && !checkLogPerm()) {
            onPermissionDenied();
        }
        if (isRoot && !checkLogPerm()) {
            Utils.cmd("pm grant com.ryuunoakaihitomi.ForceCloseLogcat android.permission.READ_LOGS", true);
        }
        startForeground(NOTICE_ID, NoticeBar.serviceStart());
        LogOperaBcReceiver.reg();
        Thread thread = new Thread(this);
        //增加优先级方法1
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        if (isAlive)
            Utils.simpleToast(this, getString(R.string.service_running), false, false);
    }

    @Override
    public void run() {
        //增加优先级方法2
        //priority	int: A Linux priority level, from -20 for highest scheduling priority to 19 for lowest scheduling priority.
        android.os.Process.setThreadPriority(-20);
        final String GET_LOG_CMD = "logcat -v threadtime" + "\n";
        //头部识别
        final String LOG_BUFFER_DIVIDER = "--------- beginning of ";
        //Native崩溃头部识别
        final String[] N_SIGNAL = {"DEBUG", "F", "*** *** *** *** *** *** *** *** *** *** *** *** *** *** *** ***"};
        final String[] J_SIGNAL = {"AndroidRuntime", "E", "FATAL EXCEPTION"};
        //日志示例："ANR in com.android.development (com.android.development/.BadBehaviorActivity)"
        final String[] ANR_SIGNAL = {"ActivityManager", "E", "ANR in "};
        //日志示例："PID: 9763"
        final String[] ANR_PROC_SIGNAL = {ANR_SIGNAL[2], "PID: "};
        //日志示例："    Process: com.android.development, PID: 9752"
        final String[] J_PROC_SIGNAL = {"Process: ", ", PID: "};
        //日志示例："    pid: 9645, tid: 9645, name: oid.development  >>> com.android.development <<<"
        final String[] N_PROC_SIGNAL = {">>> ", " <<<", ": pid: ", ", tid:"};
        /*
        Lollipop会跳过native崩溃日志输出
        A/libc: Fatal signal 5 (SIGTRAP), code 1 in tid 5055 (.crashgenerator)
        I/libc: Suppressing debuggerd output because prctl(PR_GET_DUMPABLE)==0

        A/libc: Fatal signal 4 (SIGILL), code 1, fault addr 0xb3849740 in tid 11183 (.crashgenerator)
        */
        final String J_SYS_SIGNAL = "*** " + J_SIGNAL[2] + " IN SYSTEM PROCESS";
        Process process = null;
        DataOutputStream dataOutputStream = null;
        //从pm的返回值可以看出在22中可以pm grant授权，但实测21也可以。
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                process = Runtime.getRuntime().exec("su");
                dataOutputStream = new DataOutputStream(process.getOutputStream());
                dataOutputStream.writeBytes(GET_LOG_CMD);
                dataOutputStream.flush();
            } else
                process = Runtime.getRuntime().exec(GET_LOG_CMD);
            String line;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (isAlive) {
                while ((line = bufferedReader.readLine()) != null) {
                    if (isTickReceived) {
                        Log.d(TAG, "run: An Alive Signal. line=\"" + line + "\"");
                        isTickReceived = false;
                        //礼让暗示
                        Thread.yield();
                    }
                    if (!line.contains(LOG_BUFFER_DIVIDER)) {
                        FCLogInfoBridge.log = line;
                        final LogObject headerJudge = new LogObject(line);
                        if ((J_SIGNAL[0].equals(headerJudge.getTag())
                                && J_SIGNAL[1].equals(headerJudge.getLevel())
                                && headerJudge.getRaw().contains(J_SIGNAL[2]))
                                ||
                                (N_SIGNAL[0].equals(headerJudge.getTag())
                                        //Android 4.4发现是I等级
                                        && (N_SIGNAL[1].equals(headerJudge.getLevel()) || "I".equals(headerJudge.getLevel()))
                                        && N_SIGNAL[2].equals(headerJudge.getRaw()))
                                ||
                                (ANR_SIGNAL[0].equals(headerJudge.getTag())
                                        && ANR_SIGNAL[1].equals(headerJudge.getLevel())
                                        && headerJudge.getRaw().contains(ANR_SIGNAL[2]))) {
                            Log.d(TAG, "run: CrashLogPrinter: PID:" + headerJudge.getPID() + " TID:" + headerJudge.getTID());
                            //给低性能设备的crash日志输出进行礼让（测试）
                            Thread.yield();
                            Log.d(TAG, "run: yield()");
                            final long start = System.currentTimeMillis();
                            if (headerJudge.getRaw().contains(J_SYS_SIGNAL)) {
                                //对应的标签为"Android 系统"
                                FCLogInfoBridge.setFcPackageName("android");
                                FCLogInfoBridge.setFcPID("i");
                            }
                            while (Arrays.asList(new String[]{J_SIGNAL[0], N_SIGNAL[0], ANR_SIGNAL[0]}).contains(
                                    new LogObject(line = bufferedReader.readLine()).getTag())) {
                                if (line.contains(J_PROC_SIGNAL[0])) {
                                    String pkgNameTmp;
                                    pkgNameTmp = new LogObject(line).getRaw().substring(J_PROC_SIGNAL[0].length(), new LogObject(line).getRaw().indexOf(J_PROC_SIGNAL[1]));
                                    if (pkgNameTmp.contains(":"))
                                        pkgNameTmp = pkgNameTmp.split(":")[0];
                                    FCLogInfoBridge.setFcPackageName(pkgNameTmp);
                                    FCLogInfoBridge.setFcPID(line.subSequence(line.indexOf(J_PROC_SIGNAL[1]) + J_PROC_SIGNAL[1].length(), line.length()).toString());
                                } else if (line.contains(N_PROC_SIGNAL[0]) && line.contains(N_PROC_SIGNAL[1])) {
                                    FCLogInfoBridge.setFcPackageName(line.subSequence(line.indexOf(N_PROC_SIGNAL[0]) + N_PROC_SIGNAL[0].length(), line.indexOf(N_PROC_SIGNAL[1])).toString());
                                    FCLogInfoBridge.setFcPID(line.subSequence(line.indexOf(N_PROC_SIGNAL[2]) + N_PROC_SIGNAL[2].length(), line.indexOf(N_PROC_SIGNAL[3])).toString());
                                } else {
                                    if (ANR_PROC_SIGNAL[0].equals(new LogObject(line).getTag()))
                                        if (line.contains(ANR_PROC_SIGNAL[1]))
                                            FCLogInfoBridge.setFcPID(line.substring(line.indexOf(ANR_PROC_SIGNAL[1]) + ANR_PROC_SIGNAL[1].length()));
                                        else
                                            FCLogInfoBridge.setFcPackageName(new LogObject(line).getRaw().substring(new LogObject(line).getRaw().indexOf(ANR_PROC_SIGNAL[0]) + ANR_PROC_SIGNAL[0].length()).split(" +")[0]);
                                }
                            }
                            if (!this.getClass().getPackage().getName().equals(FCLogInfoBridge.getFcPackageName())) {
                                Log.v(TAG, "run: new Thread");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        boolean
                                                isAppInWhiteList = false,
                                                isWhiteListAvailable = ConfigMgr.getBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH),
                                                isQuietModeEnable = ConfigMgr.getBoolean(ConfigMgr.Options.QUIET_MODE);
                                        try {
                                            isAppInWhiteList = new JSONObject(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST)).optBoolean(FCLogInfoBridge.getFcPackageName());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        Log.i(TAG, "run: isAppInWhiteList:" + isAppInWhiteList + " isWhiteListAvailable:" + isWhiteListAvailable + " isQuietModeEnable:" + isQuietModeEnable);
                                        if (!isWhiteListAvailable || !isAppInWhiteList) {
                                            String time = Calendar.getInstance().get(Calendar.YEAR)
                                                    + "-" + headerJudge.getDate()
                                                    + " " + headerJudge.getTime();
                                            FCLogInfoBridge.setFcTime(time);
                                            String path = LOG_DIR + "/" + time + "_" + FCLogInfoBridge.getFcPackageName() + ".log";
                                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT)
                                                path = path.replace(':', '.');
                                            //选择日志过滤条件
                                            String logFilter;
                                            if ((J_SIGNAL[0].equals(headerJudge.getTag())))
                                                logFilter = "AndroidRuntime:E";
                                            else if ((N_SIGNAL[0].equals(headerJudge.getTag())))
                                                //未确定DEBUG的Level变成F是在什么API Level
                                                logFilter = "DEBUG:F,DEBUG:I";
                                            else
                                                logFilter = "ActivityManager:E";
                                            Log.i(TAG, "run: set logcat filter:" + logFilter);
                                            final String LOG_FILTER_OUTPUT_CMD = "logcat -v raw -d -s " + logFilter;
                                            TxtFileIO.W(path, Utils.cmd(LOG_FILTER_OUTPUT_CMD, Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || Build.VERSION.SDK_INT > Build.VERSION_CODES.O));
                                            long logLength = new File(path).length();
                                            Log.d(TAG, "run: logLength:" + logLength);
                                            FCLogInfoBridge.setLogPath(path);
                                            if (!isQuietModeEnable)
                                                NoticeBar.onFCFounded();
                                            else
                                                震える();
                                        }
                                        Log.i(TAG, "run: A Workflow in " + (System.currentTimeMillis() - start) + "ms");
                                        //不清除日志在短时间发生多次崩溃时将会重复输出，但极不方便调试
                                        cleanLog();
                                    }
                                }).start();
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "run: jumped out the loop.");
            if (dataOutputStream != null)
                dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert process != null;
        process.destroy();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(tickReceiver);
        LogOperaBcReceiver.unreg();
        isAlive = false;
        stopForeground(true);
        super.onDestroy();
    }

    //清除日志
    void cleanLog() {
        final String CLEAN_LOG_CMD = "logcat -c";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP || Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            Utils.cmd(CLEAN_LOG_CMD, true);
        else if (checkLogPerm())
            Utils.cmd(CLEAN_LOG_CMD, false);
        else if (isRoot)
            Utils.cmd(CLEAN_LOG_CMD, true);
    }

    //检查日志读取权限
    boolean checkLogPerm() {
        int intRet;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            intRet = checkSelfPermission(Manifest.permission.READ_LOGS);
        else
            intRet = checkPermission(Manifest.permission.READ_LOGS, android.os.Process.myPid(), android.os.Process.myUid());
        return intRet == PackageManager.PERMISSION_GRANTED;
    }

    @SuppressWarnings({"ConstantConditions", "NonAsciiCharacters"})
    void 震える() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            final int DIT = 30, DAH = DIT * 3;
            //..-. -.-.
            vibrator.vibrate(new long[]{0, DIT/*1*/, DIT, DIT/*2*/, DIT, DAH/*3*/, DIT, DIT/*4*/, DAH/* */, DAH/*5*/, DIT, DIT/*6*/, DIT, DAH/*7*/, DIT, DIT/*8*/}, -1);
        } else
            Log.e(TAG, "震える: Vibrator not available!(真っ沈黙モード)");
    }

    @SuppressWarnings("ConstantConditions")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && KILL_SIGNAL.equals(intent.getAction())) {
            ((NotificationManager) getSystemService(NOTIFICATION_SERVICE)).cancelAll();
            stopSelf();
            new Thread() {
                @Override
                public void run() {
                    System.exit(0);
                }
            }.start();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    void onPermissionDenied() {
        isAlive = false;
        Log.e(TAG, "onCreate: I do not have permission to read the system log.");
        Utils.simpleToast(this, getString(R.string.no_read_log_perm), true, true);
        stopSelf();
    }
}
