# RentEase release shrinker rules.
# Applies only to the release build type (isMinifyEnabled = true); debug builds
# and unit tests are unaffected.

# kotlinx.serialization: keep generated serializers and @Serializable DTOs.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.rentease.app.**$$serializer { *; }
-keepclassmembers class com.rentease.app.** { *** Companion; }
-keepclasseswithmembers class com.rentease.app.** { kotlinx.serialization.KSerializer serializer(...); }

# Retrofit: keep generic signatures + annotations used to build service proxies.
-keepattributes Signature, Exceptions
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep interface com.rentease.app.data.remote.api.** { *; }

# Room entities / DAOs are referenced by generated code.
-keep class com.rentease.app.data.local.** { *; }
