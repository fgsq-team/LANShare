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
#指定压缩级别
-optimizationpasses 5
# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

#-keep class com.fgsqw.lanshare.pojo.AddDevice {*;}
#-keep class com.fgsqw.lanshare.pojo.message.MessageContent {*;}
#-keep class com.fgsqw.lanshare.pojo.message.MessageFileContent {*;}
#-keep class com.fgsqw.lanshare.pojo.message.MessageFolderContent {*;}
#-keep class com.fgsqw.lanshare.pojo.message.MessageMediaContent {*;}
