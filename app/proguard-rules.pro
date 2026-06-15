# StudyMate R8 / ProGuard rules (release build, minify + resource shrink on).
#
# AGP automatically keeps everything referenced from AndroidManifest.xml
# (Application, every Activity), so those need no rules here. The keeps below
# cover things that are created reflectively or sealed inside libraries.

# Keep crash-report line numbers meaningful (use the generated mapping.txt in
# Play Console to de-obfuscate). Hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Room entities ──────────────────────────────────────────────────────────
# Room's generated code is R8-safe, but keep the entity classes' members so
# column binding via reflection can never be stripped.
-keep class uws.ac.uk.studymate.data.entities.** { *; }

# ── ViewModels ─────────────────────────────────────────────────────────────
# ViewModelProvider instantiates these reflectively through their
# (Application) constructor — keep the constructors.
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# ── WorkManager workers ────────────────────────────────────────────────────
# CoroutineWorkers (AssignmentReminderWorker, ReviewReminderWorker) are created
# reflectively by WorkManager via the (Context, WorkerParameters) constructor.
-keep class * extends androidx.work.ListenableWorker {
    <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ── Tink (backs EncryptedSharedPreferences via security-crypto) ─────────────
# Tink loads its key managers reflectively; stripping them breaks the encrypted
# biometric credential store at runtime.
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**
-dontwarn javax.annotation.**
-dontwarn com.google.errorprone.annotations.**
