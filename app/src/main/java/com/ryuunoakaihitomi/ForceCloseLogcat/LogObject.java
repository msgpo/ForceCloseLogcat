package com.ryuunoakaihitomi.ForceCloseLogcat;

public class LogObject {
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

    public String getDate() {
        return logSplit[0];
    }

    public String getTime() {
        return logSplit[1];
    }

    public String getPID() {
        return logSplit[2];
    }

    public String getTID() {
        return logSplit[3];
    }

    public String getLevel() {
        return logSplit[4];
    }

    public String getTag() {
        String tagRaw = logSplit[5];
        int separatorPos = tagRaw.length() - LONG_TAG_SEPARATOR.length();
        return LONG_TAG_SEPARATOR.equals(tagRaw.substring(separatorPos))
                ? tagRaw.substring(0, separatorPos)
                : tagRaw;
    }

    public String getRaw() {
        return srcLog.substring(srcLog.indexOf(RAW_SEPARATOR) + RAW_SEPARATOR.length());
    }
}
