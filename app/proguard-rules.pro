# Keep Room entities/DAO annotations, Kotlin serialization, OkHttp
-keepattributes *Annotation*
-keep class com.ainote.manager.data.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# androidx.security.crypto (Tink) references compile-time-only annotations that
# aren't present at runtime. R8 fails on "Missing classes" for these unless told
# to ignore them — this is a known, harmless warning, not a real missing dependency.
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
