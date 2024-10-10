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

-keepclassmembers class * extends java.lang.Enum {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class MainKt {
	public static void main(java.lang.String[]);
}

-keep public class org.apache.commons.logging.** { *; }
#-keep public class org.apache.pdfbox.** { *; }
#-keep public class com.openhtmltopdf.** { *; }

#-keep class org.celtric.kotlin.html.** { public final Object invoke(); }
#-keep class com.gi.apksize.ui.** { public final Object invoke(); }


-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

#-whyareyoukeeping class kotlin.reflect.jvm.internal.impl.load.java.**