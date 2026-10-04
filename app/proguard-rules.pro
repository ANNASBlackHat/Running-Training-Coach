# ProGuard rules for Running Companion

-keepattributes *Annotation*
-dontwarn java.lang.invoke.**

# Keep Kotlinx Serialization companions and serializers
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Preserve Room entities and domain models from obfuscation
-keep class com.runningcompanion.app.domain.model.** { *; }
-keep class com.runningcompanion.app.data.db.entity.** { *; }
-keep class androidx.room.** { *; }

# OSMDroid
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**
