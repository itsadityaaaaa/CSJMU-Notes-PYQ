# Keep Domain and Data Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.data.local.** { *; }
-keep class com.example.data.remote.** { *; }

# Room Database rules
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase & Google Play Services
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

# Kotlin Coroutines & Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**

# OkHttp & Gson
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes EnclosingMethod

# Keep R class and Android resources
-keepclassmembers class **.R$* {
    public static <fields>;
}
