-keepattributes *Annotation*
-keepattributes Signature

-keep class com.yandex.mobile.** { *; }
-keep class com.google.android.gms.** { *; }

-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class com.example.app.data.model.** { *; }