# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ================================
# Critical Android Components - MUST BE KEPT
# ================================

# Keep Application class - CRITICAL! Without this, app crashes on startup
-keep class com.galaxyjoy.hexviewer.MyApplication { *; }
-keep public class * extends android.app.Application

# Keep all Activity, Service, BroadcastReceiver, ContentProvider
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends android.app.Fragment

# Keep all View constructors (needed for layout inflation)
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ================================
# APK Size Optimization Rules
# ================================

# Remove logging in release builds - SECURITY: Prevents sensitive data leakage
# This removes all Log statements including sensitive GAIDs, file paths, etc.
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
}

# Remove custom logging that may contain sensitive information
# MyApplication.addLog stores logs in memory buffer - remove in release
-assumenosideeffects class com.galaxyjoy.hexviewer.MyApplication {
    public static *** addLog(...);
}

# Note: We keep Log.e() for crash reporting
# To remove all logging including errors, uncomment below:
# -assumenosideeffects class android.util.Log {
#     public static *** e(...);
# }

# Aggressive optimization (but not too aggressive)
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Keep model classes (preserve logic)
-keep class com.galaxyjoy.hexviewer.models.** { *; }

# Keep AdMob classes
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.mediation.** { *; }

# Keep Lottie
-keep class com.airbnb.lottie.** { *; }

# Keep Preferences (AndroidX)
-keep class androidx.preference.** { *; }
-keep class * extends androidx.preference.PreferenceFragmentCompat
-keepclassmembers class * extends androidx.preference.PreferenceFragmentCompat {
    public <init>(...);
}

# Keep Kotlin metadata (for reflection)
-keep class kotlin.Metadata { *; }
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# Keep app-specific classes that might use reflection
-keep class com.galaxyjoy.hexviewer.ui.** { *; }
-keep class com.galaxyjoy.hexviewer.util.** { *; }
-keep class com.galaxyjoy.hexviewer.constants.** { *; }

# Keep classes referenced in AndroidManifest
-keep class com.galaxyjoy.hexviewer.ui.act.** { *; }

# Keep R classes
-keepclassmembers class **.R$* {
    public static <fields>;
}

# Remove unused resources
-dontwarn org.apache.commons.collections4.**
-dontwarn androidx.emoji.**

# Optimize enums
-optimizations !code/allocation/variable

# Keep debug attributes for better crash reports
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod