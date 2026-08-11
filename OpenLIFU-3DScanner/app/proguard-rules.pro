# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve line numbers for Crashlytics stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep all Retrofit/Gson DTO classes — field names must match JSON keys
-keep class health.openwater.openlifu3dscanner.network.dto.** { *; }

# Keep Gson type adapters registered by class reference
-keep class health.openwater.openlifu3dscanner.network.adapter.** { *; }