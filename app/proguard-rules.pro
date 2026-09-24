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

# Google Play Services Auth (Google Sign-In / Drive access) ships its own consumer
# proguard rules, but keep the request/response model classes just in case R8 strips
# something it shouldn't across the reflection-based bits of the auth flow.
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.common.api.** { *; }
-dontwarn com.google.android.gms.**
