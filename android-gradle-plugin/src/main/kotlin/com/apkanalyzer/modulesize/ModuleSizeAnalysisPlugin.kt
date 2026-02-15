package com.apkanalyzer.modulesize

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * Gradle plugin that registers the `analyzeModuleSizes` task on every
 * `com.android.application` module in the project.
 *
 * Apply this plugin to the **root** `build.gradle(.kts)`:
 *
 * ```kotlin
 * plugins {
 *     id("com.apkanalyzer.module-size-analysis") version "0.1.0"
 * }
 *
 * moduleSizeAnalysis {
 *     variant = "standardRelease"
 *     packageDepth = 4
 *     functionalUnitMapping = mapOf(
 *         "hotels"  to listOf("com.mmt:mmt-hotels*"),
 *         "flights" to listOf("com.mmt:mmt-flights*"),
 *     )
 * }
 * ```
 *
 * Then run:
 * ```
 * ./gradlew :app:analyzeModuleSizes
 * ```
 */
class ModuleSizeAnalysisPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // This plugin should be applied to the root project
        val rootProject = project.rootProject

        // Create extension on root project (idempotent — only once)
        if (rootProject.extensions.findByName("moduleSizeAnalysis") == null) {
            rootProject.extensions.create("moduleSizeAnalysis", ModuleSizeAnalysisExtension::class.java)
        }

        // Wire task on every android application module
        rootProject.allprojects { proj ->
            proj.afterEvaluate {
                if (!proj.plugins.hasPlugin("com.android.application")) return@afterEvaluate

                proj.tasks.register("analyzeModuleSizes", AnalyzeModuleSizesTask::class.java)
            }
        }
    }
}
