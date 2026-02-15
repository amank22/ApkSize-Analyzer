-dontobfuscate
-allowaccessmodification
-overloadaggressively
-printusage <user.dir>/build/r8/usages.txt
-printmapping <user.dir>/build/r8/mapping.txt
-keepattributes Exceptions,InnerClasses,Signature,Deprecated,SourceFile,LineNumberTable,*Annotation*,EnclosingMethod

-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}
-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
}

# --- Suppress warnings for classes not present in the lite JAR ---
-dontwarn org.apache.sshd.**
-dontwarn javax.mail.Authenticator
-dontwarn org.jetbrains.kotlin.**
-dontwarn **.dev.**
-dontwarn javax.**
-dontwarn org.jaxen.**
-dontwarn com.sun.**
-dontwarn com.intellij.**
-dontwarn com.android.sdklib.**
-dontwarn com.android.dvlib.**
-dontwarn org.apache.avalon.**
-dontwarn org.apache.log.**
-dontwarn org.apache.log4j.**
-dontwarn com.google.archivepatcher.**

# Bundletool and its ecosystem are not in the lite JAR — suppress all references.
# Our code guards these paths at runtime via isBundletoolEmbedded() check.
-dontwarn com.android.tools.build.bundletool.**
-dontwarn shadow.bundletool.**
-dontwarn com.android.bundle.**
-dontwarn com.android.aapt.**
-dontwarn com.google.protobuf.**
-dontwarn com.google.auto.**
-dontwarn org.jose4j.**
-dontwarn org.checkerframework.**
-dontwarn dagger.**
-dontwarn org.slf4j.**

# --- Keep rules ---

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class MainKt {
	public static void main(java.lang.String[]);
}

-keep public class org.apache.commons.logging.** { *; }

-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Keep Gson-deserialized data classes fully intact (constructors, defaults, copy)
-keep class com.gi.apksize.models.** { *; }

# Keep Gson TypeToken generic signatures (needed for reflection-based deserialization)
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken

#-whyareyoukeeping class kotlin.reflect.jvm.internal.impl.load.java.**
