# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
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
-keepnames class * extends android.os.Parcelable
-keepnames class * extends java.io.Serializable
-keep class * extends com.kylecorry.trail_sense.shared.ProguardIgnore { *; }
-keep class * extends androidx.fragment.app.Fragment{}
-keep class * extends com.kylecorry.andromeda.views.subscaleview.decoder.ImageDecoder
-keep class * extends com.kylecorry.andromeda.views.subscaleview.decoder.ImageRegionDecoder
-keep class com.kylecorry.andromeda.bitmaps.Range2d { *; }
-keep class com.kylecorry.andromeda.bitmaps.Toolkit { *; }

-dontwarn com.caverock.androidsvg.SVG
-dontwarn com.caverock.androidsvg.SVGParseException
-dontwarn pl.droidsonroids.gif.GifDrawable