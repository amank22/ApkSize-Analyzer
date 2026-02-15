plugins {
    `java-gradle-plugin`
    kotlin("jvm")  // version inherited from root project (2.3.10)
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "com.apkanalyzer"
version = "0.1.0"

// Repositories are centrally managed in settings.gradle

dependencies {
    // AGP types (resolved by consumer project at runtime)
    compileOnly("com.android.tools.build:gradle:8.2.0")

    // Gradle API is provided by java-gradle-plugin automatically
    implementation("com.google.code.gson:gson:2.10.1")
}

gradlePlugin {
    website.set("https://github.com/anthropics/ApkSize-Analyzer")
    vcsUrl.set("https://github.com/anthropics/ApkSize-Analyzer")

    plugins {
        create("moduleSizeAnalysis") {
            id = "com.apkanalyzer.module-size-analysis"
            displayName = "Module Size Analysis"
            description = "Analyzes Android module and AAR dependencies to extract resource counts, " +
                "package/class info, native library details, and produces file-level resource mapping " +
                "for APK size attribution across functional units."
            tags.set(listOf("android", "apk", "size-analysis", "module-analysis", "aar"))
            implementationClass = "com.apkanalyzer.modulesize.ModuleSizeAnalysisPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}
