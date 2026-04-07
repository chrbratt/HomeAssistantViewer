# ── Crash analysis ────────────────────────────────────────────────────────────
# Preserve source file names and line numbers so stack traces in Play Console
# and Firebase Crashlytics are human-readable even after obfuscation.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlin ────────────────────────────────────────────────────────────────────
# Keep Kotlin metadata so reflection-based libraries work correctly.
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class kotlin.Metadata { *; }

# ── Moshi — JSON models ───────────────────────────────────────────────────────
# Keep all data classes that are serialized/deserialized by Moshi so that
# field names and constructors survive minification.
-keep class se.inix.homeassistantviewer.data.model.** { *; }

# Moshi generated adapters (KSP codegen) — names end with JsonAdapter.
-keep class **JsonAdapter { *; }
-keep class **JsonAdapter$* { *; }

# Moshi core — keep @Json and related annotations.
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }

# ── Retrofit ──────────────────────────────────────────────────────────────────
-keepattributes Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ── OkHttp ────────────────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────────────────────────────
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ── Enums ─────────────────────────────────────────────────────────────────────
# R8 can inline enums; keep valueOf()/values() so they survive.
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── DataStore / Preferences ───────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── Security Crypto ───────────────────────────────────────────────────────────
-keep class androidx.security.crypto.** { *; }
