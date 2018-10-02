#   http://developer.android.com/guide/developing/tools/proguard.html
-keep public class com.ryuunoakaihitomi.ForceCloseLogcat.XposedHookPlugin
-keep public class com.ryuunoakaihitomi.ForceCloseLogcat.ConfigUI {
    static boolean isXposedActive();
}
-renamesourcefileattribute f
-keepattributes f,LineNumberTable