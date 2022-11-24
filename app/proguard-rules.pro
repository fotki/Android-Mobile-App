# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in /Users/apple/Library/Android/sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

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
# Keep our interfaces so they can be used by other ProGuard rules.
# See http://sourceforge.net/p/proguard/bugs/466/
-keep,allowobfuscation @interface com.facebook.common.internal.DoNotStrip

# Do not strip any method/class that is annotated with @DoNotStrip
-keep @com.facebook.common.internal.DoNotStrip class *
-keepclassmembers class * {
    @com.facebook.common.internal.DoNotStrip *;
}

# Keep native methods
-keepclassmembers class * {
    native <methods>;
}

-dontwarn okio.**
-dontwarn com.android.volley.toolbox.**
-dontwarn com.squareup.okhttp.**
-dontwarn javax.annotation.**
-dontwarn com.facebook.infer.**
-dontwarn com.roughike.bottombar.**
-keep class org.apache.http.** { *; }
-keepclassmembers class org.apache.http.** {*;}
-dontwarn org.apache.**

-keep class android.net.http.** { *; }
-keepclassmembers class android.net.http.** {*;}
-dontwarn android.net.**
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-printmapping outputfile.txt
-keep public class * extends android.app.Service

-keep class com.simplemobiletools.** { *; }
-dontwarn android.graphics.Canvas
-dontwarn com.simplemobiletools.**
-dontwarn org.apache.**
