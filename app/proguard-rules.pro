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

# JSON 序列化/Intent 传递的消息体，保留字段名（也保证新旧版本互通）
-keep class com.fgsqw.lanshare.pojo.** { *; }
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 图片预览用反射访问 PhotoViewAttacher.mBaseMatrix/resetMatrix
-keep class com.github.chrisbanes.photoview.PhotoViewAttacher { *; }

# 保留行号便于定位崩溃
-keepattributes SourceFile,LineNumberTable
