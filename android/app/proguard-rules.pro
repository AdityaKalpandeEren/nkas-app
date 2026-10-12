# kotlinx.serialization: keep generated serializers of the dashboard DTOs
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.kalpande.algopulse.**$$serializer { *; }
-keepclassmembers class com.kalpande.algopulse.** { *** Companion; }
-keepclasseswithmembers class com.kalpande.algopulse.** { kotlinx.serialization.KSerializer serializer(...); }

# WorkManager workers / Glance receivers are instantiated by reflection
-keep class com.kalpande.algopulse.work.** extends androidx.work.ListenableWorker { <init>(...); }
-keep class com.kalpande.algopulse.widget.** { *; }

# OkHttp ships its own rules; silence optional platform warnings
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
