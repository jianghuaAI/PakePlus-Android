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

# 修复拍照闪退：保留 FileProvider 与 MainActivity 内部类（相机回调通过反射/内部类调用）
-keep class androidx.core.content.FileProvider { *; }
-keep class com.app.pakeplus.MainActivity { *; }
-keep class com.app.pakeplus.MainActivity$* { *; }
-keep class com.app.pakeplus.MainActivity$MyChromeClient { *; }