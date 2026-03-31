# Ktor
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.cashpilot.android.model.**$$serializer { *; }
-keepclassmembers class com.cashpilot.android.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.cashpilot.android.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
