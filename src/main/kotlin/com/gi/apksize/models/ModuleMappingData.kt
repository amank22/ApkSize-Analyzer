package com.gi.apksize.models

/**
 * Container for parsed module mapping files produced by the module-size-analysis Gradle plugin.
 * Used to attribute APK/AAB sizes to functional units (LOBs).
 */
data class ModuleMappingData(
    val metadata: ModuleMetadata = ModuleMetadata(),
    /** File path -> list of module indices that contribute the file */
    val resourceMapping: Map<String, List<Int>> = emptyMap(),
    /** Package name -> list of [moduleIndex, classCount] pairs */
    val packageMapping: Map<String, List<List<Int>>> = emptyMap(),
)

/**
 * Deserialization target for module-metadata.json.
 * All fields have defaults so the Kotlin compiler emits a no-arg constructor,
 * which Gson needs when running inside a GraalVM native image.
 */
data class ModuleMetadata(
    /** Indexed array of module identifiers (0-based). All other files reference by index. */
    val modules: List<String> = emptyList(),
    /** Functional unit name -> list of module indices belonging to that unit */
    val functionalUnits: Map<String, List<Int>> = emptyMap(),
    /** Build variant used for analysis */
    val variant: String? = null,
    /** Dynamic feature module names */
    val dynamicFeatures: List<String>? = null,
)
