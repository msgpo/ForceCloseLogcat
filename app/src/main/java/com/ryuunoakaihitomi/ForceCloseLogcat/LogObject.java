package com.ryuunoakaihitomi.ForceCloseLogcat;

/**
 * 封装类：对logcat -v threadtime输出的日志进行的解析
 * 日志示例："04-30 04:37:25.233  1486  1486 I chatty  : uid=1000 system_server expire 1 line"
 */


@SuppressWarnings("CanBeFinal")
class LogObject {
    @SuppressWarnings("FieldCanBeLocal")
    private final String
            SPLIT_REGEX = " +",
            RAW_SEPARATOR = ": ",
            LONG_TAG_SEPARATOR = ":";
    private String srcLog;
    private String[] logSplit;

    LogObject(String threadTimeLog) {
        srcLog = threadTimeLog;
        logSplit = srcLog.split(SPLIT_REGEX);
    }

    String getDate() {
        return logSplit[0];
    }

    /**
     * @return 时分秒
     */
    String getTime() {
        return logSplit[1];
    }

    String getPID() {
        return logSplit[2];
    }

    String getTID() {
        return logSplit[3];
    }

    String getLevel() {
        return logSplit[4];
    }

    public String getTag() {
        String tagRaw = logSplit[5];
        int separatorPos = tagRaw.length() - LONG_TAG_SEPARATOR.length();
        return LONG_TAG_SEPARATOR.equals(tagRaw.substring(separatorPos))
                ? tagRaw.substring(0, separatorPos)
                : tagRaw;
    }

    /**
     * @return 取得日志正文
     */
    String getRaw() {
        return srcLog.substring(srcLog.indexOf(RAW_SEPARATOR) + RAW_SEPARATOR.length());
    }
}
