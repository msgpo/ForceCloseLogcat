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
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
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
    private static final int NOTICE_ID = 1989;
    private static final String TAG = "FCLogService";
    private static final TrueTimingLogger timingLogger = new TrueTimingLogger("", "");
    //分钟接收器：检查权限
    private final BroadcastReceiver tickReceiver = new BroadcastReceiver() {
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

    @SuppressWarnings({"ConstantConditions", "NonAsciiCharacters"})
    static void 震える(Context context) {
        Vibrator vibrator = (Vibrator) context.getSystemService(VIBRATOR_SERVICE);
        if (vibrator.hasVibrator()) {
            final int DIT = 30, DAH = DIT * 3;
            //..-. -.-.
            vibrator.vibrate(new long[]{0, DIT/*1*/, DIT, DIT/*2*/, DIT, DAH/*3*/, DIT, DIT/*4*/, DAH/* */, DAH/*5*/, DIT, DIT/*6*/, DIT, DAH/*7*/, DIT, DIT/*8*/}, -1);
        } else
            Log.e(TAG, "震える: Vibrator not available!");
    }

    /**
     * 检查是否获得root权限
     *
     * @return 如已获得root权限，返回为真，反之为假
     */
    private static synchronized boolean isRoot() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec("su");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (process == null)
            return false;
        try (DataOutputStream os = new DataOutputStream(process.getOutputStream())) {
            os.writeBytes("exit\n");
            os.flush();
            int exitValue = process.waitFor();
            return exitValue == 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                process.destroyForcibly();
            else
                process.destroy();
        }
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(tickReceiver);
        LogOperaBcReceiver.unreg(this);
        isAlive = false;
        stopForeground(true);
        super.onDestroy();
    }

    /**
     * 执行shell
     *
     * @param command 所执行的命令
     * @param isRoot  是否需要root
     * @return 标准输入流
     */
    private static synchronized String cmd(String command, boolean isRoot) {
        StringBuilder ret = new StringBuilder();
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
            try {
                System.out.println("cmd: \"" + command + "\" exitValue=" + p.waitFor());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            BufferedInputStream bufferedInputStream = new BufferedInputStream(p.getInputStream());
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                ret.append(line).append("\n");
            }
            p.getErrorStream().close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret.toString();
    }

    //检查日志读取权限
    private boolean checkLogPerm() {
        int intRet;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            intRet = checkSelfPermission(Manifest.permission.READ_LOGS);
        else
            intRet = checkPermission(Manifest.permission.READ_LOGS, android.os.Process.myPid(), android.os.Process.myUid());
        return intRet == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        final long rootStartTime = System.currentTimeMillis();
        isRoot = isRoot();
        final long interval = System.currentTimeMillis() - rootStartTime;
        if (interval > 1000) {
            Log.w(TAG, "onCreate: Require root permission timeout(" + interval + "ms,>1s).Are you did it for the first time?");
            onCreate();
            return;
        } else
            Log.v(TAG, "Root Interval:" + interval + "ms");
        boolean isReadLogPermissionGranted = checkLogPerm();
        isAlive = true;
        cleanLog();
        //权限检查与试图开启
        Log.i(TAG, "onCreate: READ_LOGS perm granted:" + isReadLogPermissionGranted + " isRoot:" + isRoot);
        if (!isReadLogPermissionGranted)
            if (!isRoot)
                onPermissionDenied();
            else
                cmd("pm grant " + getPackageName() + " android.permission.READ_LOGS", true);
        startForeground(NOTICE_ID, NoticeBar.serviceStart(this));
        LogOperaBcReceiver.reg(this);
        Thread thread = new Thread(this);
        //增加优先级方法1
        thread.setPriority(Thread.MAX_PRIORITY);
        thread.start();
        registerReceiver(tickReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
        if (isAlive)
            Utils.simpleToast(this, getString(R.string.service_running), false, false);
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

    private void onPermissionDenied() {
        isAlive = false;
        Log.e(TAG, "onCreate: I do not have permission to read the system log.");
        Utils.simpleToast(this, getString(R.string.no_read_log_perm), true, true);
        stopSelf();
    }

    @Override
    public void onTrimMemory(int level) {
        Log.i(TAG, "onTrimMemory: level:" + level);
        timingLogger.reset(TAG, "onTrimMemory");
        System.gc();
        System.runFinalization();
        timingLogger.dumpToLog();
        timingLogger.reset();
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
                        final LogObjMethods headerJudge = LogObject.initLogObjDynProxyInstance(new LogObject(line));
                        boolean isJavaFCCrash = (J_SIGNAL[0].equals(headerJudge.getTag())
                                && J_SIGNAL[1].equals(headerJudge.getLevel())
                                && headerJudge.getRaw().contains(J_SIGNAL[2]));
                        if (isJavaFCCrash ||
                                (N_SIGNAL[0].equals(headerJudge.getTag())
                                        //Android 4.4发现是I等级
                                        && (N_SIGNAL[1].equals(headerJudge.getLevel()) || "I".equals(headerJudge.getLevel()))
                                        && N_SIGNAL[2].equals(headerJudge.getRaw()))
                                ||
                                (ANR_SIGNAL[0].equals(headerJudge.getTag())
                                        && ANR_SIGNAL[1].equals(headerJudge.getLevel())
                                        && headerJudge.getRaw().contains(ANR_SIGNAL[2]))) {
                            timingLogger.reset(TAG, "Crash caught");
                            final long start = System.currentTimeMillis();
                            Log.d(TAG, "run: CrashLogPrinter: PID:" + headerJudge.getPID() + " TID:" + headerJudge.getTID());
                            //给低性能设备的crash日志输出进行礼让（测试）
                            Thread.yield();
                            Log.d(TAG, "run: yield()");
                            timingLogger.addSplit("yield");
                            if (headerJudge.getRaw().contains(J_SYS_SIGNAL)) {
                                //对应的标签为"Android 系统"
                                FCLogInfoBridge.setFcPackageName("android");
                                FCLogInfoBridge.setFcPID(String.valueOf(Build.VERSION.SDK_INT));
                            } else {
                                if (isJavaFCCrash) {
                                    //如果用xposed，JVM FC crash不再需要操心
                                    if (ConfigUI.isXposedActive() && ConfigMgr.getBoolean(ConfigMgr.Options.XPOSED)) {
                                        Log.d(TAG, "run: Exit for XposedHookPlugin.");
                                        timingLogger.dumpToLog();
                                        timingLogger.reset();
                                        continue;
                                    }
                                }
                            }
                            timingLogger.addSplit("Android & Xposed judgement");
                            while (Arrays.asList(new String[]{J_SIGNAL[0], N_SIGNAL[0], ANR_SIGNAL[0]}).contains(
                                    new LogObject(line = bufferedReader.readLine()).getTag())) {
                                if (line.contains(J_PROC_SIGNAL[0])) {
                                    LogObjMethods logObj = LogObject.initLogObjDynProxyInstance(new LogObject(line));
                                    FCLogInfoBridge.setFcPackageName(logObj.getRaw().substring(J_PROC_SIGNAL[0].length(), logObj.getRaw().indexOf(J_PROC_SIGNAL[1])));
                                    FCLogInfoBridge.setFcPID(line.subSequence(line.indexOf(J_PROC_SIGNAL[1]) + J_PROC_SIGNAL[1].length(), line.length()).toString());
                                } else if (line.contains(N_PROC_SIGNAL[0]) && line.contains(N_PROC_SIGNAL[1])) {
                                    String pkgNameTmp = line.subSequence(line.indexOf(N_PROC_SIGNAL[0]) + N_PROC_SIGNAL[0].length(), line.indexOf(N_PROC_SIGNAL[1])).toString();
                                    //含有/会新建下几级目录
                                    //如"/data/user/0/com.ludashi.benchmark/lib/libldsdaemon_2.so"
                                    //pkgNameTmp = pkgNameTmp.split("/")[pkgNameTmp.split("/").length - 1];
                                    pkgNameTmp = pkgNameTmp.substring(pkgNameTmp.lastIndexOf("/") + 1, pkgNameTmp.length());
                                    FCLogInfoBridge.setFcPackageName(pkgNameTmp);
                                    FCLogInfoBridge.setFcPID(line.subSequence(line.indexOf(N_PROC_SIGNAL[2]) + N_PROC_SIGNAL[2].length(), line.indexOf(N_PROC_SIGNAL[3])).toString());
                                } else if (line.contains(ANR_PROC_SIGNAL[1])) {
                                    FCLogInfoBridge.setFcPID(line.substring(line.indexOf(ANR_PROC_SIGNAL[1]) + ANR_PROC_SIGNAL[1].length()));
                                    FCLogInfoBridge.setFcPackageName(headerJudge.getRaw().substring(headerJudge.getRaw().indexOf(ANR_PROC_SIGNAL[0]) + ANR_PROC_SIGNAL[0].length()).split(" +")[0]);
                                }
                            }
                            timingLogger.addSplit("get crash package info");
                            //裁剪包名：a.b.c:xxx
                            String pkgNameCutTmp = FCLogInfoBridge.getFcPackageName();
                            if (pkgNameCutTmp.contains(":"))
                                FCLogInfoBridge.setFcPackageName(pkgNameCutTmp.split(":")[0]);
                            if (!this.getPackageName().equals(FCLogInfoBridge.getFcPackageName())) {
                                Log.v(TAG, "run: new Thread");
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        timingLogger.addSplit("new thread");
                                        boolean
                                                isAppInWhiteList = false,
                                                isAppInCustomizeWhiteListTextFilter = false,
                                                isWhiteListAvailable = ConfigMgr.getBoolean(ConfigMgr.Options.WHITE_LIST_SWITCH),
                                                isQuietModeEnable = ConfigMgr.getBoolean(ConfigMgr.Options.QUIET_MODE);
                                        try {
                                            isAppInWhiteList = new JSONObject(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST)).optBoolean(FCLogInfoBridge.getFcPackageName());
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (!TextUtils.isEmpty(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST_TEXT)))
                                            isAppInCustomizeWhiteListTextFilter = FCLogInfoBridge.getFcPackageName().contains(ConfigMgr.getString(ConfigMgr.Options.WHITE_LIST_TEXT));
                                        Log.i(TAG, "run: isAppInWhiteList:" + isAppInWhiteList
                                                + " isWhiteListAvailable:" + isWhiteListAvailable
                                                + " isQuietModeEnable:" + isQuietModeEnable
                                                + " isAppInCustomizeWhiteListTextFilter:" + isAppInCustomizeWhiteListTextFilter);
                                        timingLogger.addSplit("read config");
                                        if (!isWhiteListAvailable || !isAppInWhiteList) {
                                            if (isWhiteListAvailable && isAppInCustomizeWhiteListTextFilter)
                                                return;
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
                                            timingLogger.addSplit("adjust logcat parameter");
                                            TxtFileIO.W(path, cmd(LOG_FILTER_OUTPUT_CMD, Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                                                    .replaceAll(LOG_BUFFER_DIVIDER + ".*\n", "").replace(N_SIGNAL[2] + System.getProperty("line.separator"), ""));
                                            timingLogger.addSplit("write log");
                                            long logLength = new File(path).length();
                                            Log.d(TAG, "run: logLength:" + logLength);
                                            FCLogInfoBridge.setLogPath(path);
                                            if (!isQuietModeEnable)
                                                NoticeBar.onFCFounded(FCLogService.this);
                                            else
                                                震える(FCLogService.this);
                                            timingLogger.addSplit("send signal");
                                        }
                                        Log.i(TAG, "run: A Workflow in " + (System.currentTimeMillis() - start) + "ms");
                                        //不清除日志在短时间发生多次崩溃时将会重复输出，但极不方便调试
                                        cleanLog();
                                        timingLogger.addSplit("clean log cache");
                                        timingLogger.dumpToLog();
                                        timingLogger.reset();
                                    }
                                }).start();
                            }
                        }
                    }
                }
            }
            Log.d(TAG, "run: jumped out of the loop.");
            if (dataOutputStream != null)
                dataOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert process != null;
        process.destroy();
        stopSelf();
    }

    //清除日志
    private void cleanLog() {
        Log.d(TAG, "cleanLog: start");
        String CLEAN_LOG_CMD = "logcat -c";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            CLEAN_LOG_CMD = "logcat -b all -c";
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
            cmd(CLEAN_LOG_CMD, true);
        else if (checkLogPerm())
            cmd(CLEAN_LOG_CMD, false);
        else if (isRoot)
            cmd(CLEAN_LOG_CMD, true);
        Log.d(TAG, "cleanLog: end");
    }
}
