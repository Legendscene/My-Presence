# ── R8 / ProGuard Production Rules ──────────────────────────────────

# Remove all debug logging in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}

# Remove kotlinx.coroutines debug info
-assumenosideeffects class kotlinx.coroutines.DebugKt {
    public static *** getCOROUTINE_ID(...);
}

# Remove kotlinx.serialization verbose logging
-dontwarn kotlinx.serialization.AnnotationsKt

# ── Obfuscation ─────────────────────────────────────────────────────
-repackageclasses 'mypresence'
-allowaccessmodification
-useuniqueclassmembernames
-keepattributes Exceptions, InnerClasses, Signature, Deprecated,
    SourceFile, LineNumberTable, *Annotation*, EnclosingMethod

# Keep source file names and line numbers for crash reports
-keepattributes SourceFile, LineNumberTable

# ── Ktor ────────────────────────────────────────────────────────────
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { volatile <fields>; }
-keep class io.ktor.client.engine.okhttp.OkHttp { *; }
-dontwarn io.ktor.**

# ── OkHttp ──────────────────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }

# ── Kotlinx Serialization ───────────────────────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kyrx.mypresence.**$$serializer { *; }
-keepclassmembers class com.kyrx.mypresence.** { *** Companion; }
-keepclasseswithmembers class com.kyrx.mypresence.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Room ────────────────────────────────────────────────────────────
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ── Hilt / Dagger ───────────────────────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-dontwarn dagger.hilt.**

# ── DataStore ───────────────────────────────────────────────────────
-keep class androidx.datastore.preferences.** { *; }
-dontwarn androidx.datastore.**

# ── Coil ────────────────────────────────────────────────────────────
-dontwarn coil.**
-keep class coil.** { *; }

# ── Kotlin Coroutines ──────────────────────────────────────────────
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { volatile <fields>; }

# ── App-specific models (serialized) ────────────────────────────────
-keep class com.kyrx.mypresence.domain.model.** { *; }
-keep class com.kyrx.mypresence.data.remote.** { *; }
-keep class com.kyrx.mypresence.data.local.** { *; }

# ── Keep ViewModels (reflection used by Hilt) ──────────────────────
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}

# ── Firebase Crashlytics ───────────────────────────────────────────
-keepattributes SourceFile, LineNumberTable
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# ── Firebase Performance Monitoring ────────────────────────────────
-keep class com.google.firebase.perf.** { *; }
-dontwarn com.google.firebase.perf.**
-keep class com.google.firebase.analytics.** { *; }
-dontwarn com.google.firebase.analytics.**

# ── Keep Compose (prevent stripping of @Composable functions) ──────
-dontwarn androidx.compose.**
