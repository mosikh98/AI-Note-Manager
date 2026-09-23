# Keep Room entities/DAO annotations, Kotlin serialization, OkHttp
-keepattributes *Annotation*
-keep class com.ainote.manager.data.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-dontwarn okhttp3.**
-dontwarn okio.**
