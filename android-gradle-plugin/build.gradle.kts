plugins {
    `java-gradle-plugin`
    kotlin("jvm")  // version inherited from root project (2.3.10)
    id("com.gradle.plugin-publish") version "1.3.0"
}

group = "io.github.amank22"
version = "0.4.0"

// Repositories are centrally managed in settings.gradle

dependencies {
    // AGP types (resolved by consumer project at runtime)
    compileOnly("com.android.tools.build:gradle:8.2.0")

    // Gradle API is provided by java-gradle-plugin automatically
    implementation("com.google.code.gson:gson:2.10.1")
}

gradlePlugin {
    website.set("https://github.com/amank22/ApkSize-Analyzer/tree/main/android-gradle-plugin")
    vcsUrl.set("https://github.com/amank22/ApkSize-Analyzer")

    plugins {
        create("moduleSizeAnalysis") {
            id = "io.github.amank22.module-size-analysis"
            displayName = "Module Size Analysis"
            description = "Scans all Android Gradle modules and their AAR/JAR dependencies at build time, " +
                "producing JSON mapping files (module metadata, resource mapping, package mapping) " +
                "that power per-LOB (Line-of-Business) APK size attribution in the ApkSize Analyzer CLI. " +
                "Supports dynamic feature modules, functional unit grouping, and fine-grained overrides."
            tags.set(listOf("android", "apk-size", "size-analysis", "module-analysis", "aar",
                "lob-attribution", "dependency-analysis", "build-reporting"))
            implementationClass = "com.apkanalyzer.modulesize.ModuleSizeAnalysisPlugin"
        }
    }
}

kotlin {
    jvmToolchain(17)
}
