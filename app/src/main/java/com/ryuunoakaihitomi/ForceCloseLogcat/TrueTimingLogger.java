package com.ryuunoakaihitomi.ForceCloseLogcat;

import android.os.SystemClock;
import android.util.Log;

import java.util.ArrayList;

/**
 * 模拟android.util.TimingLogger的封装类，但是去掉了mDisabled的相关设定
 * <p>
 * https://github.com/aosp-mirror/platform_frameworks_base/blob/master/core/java/android/util/TimingLogger.java
 */

@SuppressWarnings("WeakerAccess")
public class TrueTimingLogger {

    /**
     * Stores the time of each split.
     */
    ArrayList<Long> mSplits;
    /**
     * Stores the labels for each split.
     */
    ArrayList<String> mSplitLabels;
    /**
     * The Log tag to use for checking Log.isLoggable and for
     * logging the timings.
     */
    private String mTag;
    /**
     * A label to be included in every log.
     */
    private String mLabel;

    /**
     * Create and initialize a TimingLogger object that will log using
     * the specific tag.
     *
     * @param tag   the log tag to use while logging the timings
     * @param label a string to be displayed with each log
     */
    public TrueTimingLogger(String tag, String label) {
        reset(tag, label);
    }

    /**
     * Clear and initialize a TimingLogger object that will log using
     * the specific tag.
     *
     * @param tag   the log tag to use while logging the timings
     * @param label a string to be displayed with each log
     */
    public void reset(String tag, String label) {
        mTag = tag;
        mLabel = label;
        reset();
    }

    /**
     * Clear and initialize a TimingLogger object that will log using
     * the tag and label that was specified previously, either via
     * the constructor or a call to reset(tag, label).
     */
    public void reset() {
        if (mSplits == null) {
            mSplits = new ArrayList<>();
            mSplitLabels = new ArrayList<>();
        } else {
            mSplits.clear();
            mSplitLabels.clear();
        }
        addSplit(null);
    }

    /**
     * Add a split for the current time, labeled with splitLabel.
     *
     * @param splitLabel a label to associate with this split.
     */
    public void addSplit(String splitLabel) {
        long now = SystemClock.elapsedRealtime();
        mSplits.add(now);
        mSplitLabels.add(splitLabel);
    }

    /**
     * Dumps the timings to the log using Log.d().
     */
    public void dumpToLog() {
        Log.d(mTag, mLabel + ": begin");
        final long first = mSplits.get(0);
        long now = first;
        for (int i = 1; i < mSplits.size(); i++) {
            now = mSplits.get(i);
            final String splitLabel = mSplitLabels.get(i);
            final long prev = mSplits.get(i - 1);
            Log.d(mTag, mLabel + ":      " + (now - prev) + " ms, " + splitLabel);
        }
        Log.d(mTag, mLabel + ": end, " + (now - first) + " ms");
    }
}
