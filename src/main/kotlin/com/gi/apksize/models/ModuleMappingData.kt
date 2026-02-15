package com.gi.apksize.models

/**
 * Container for parsed module mapping files produced by the module-size-analysis Gradle plugin.
 * Used to attribute APK/AAB sizes to functional units (LOBs).
 */
data class ModuleMappingData(
    val metadata: ModuleMetadata,
    /** File path -> list of module indices that contribute the file */
    val resourceMapping: Map<String, List<Int>>,
    /** Package name -> list of [moduleIndex, classCount] pairs */
    val packageMapping: Map<String, List<List<Int>>>,
)

/**
 * Deserialization target for module-metadata.json.
 */
data class ModuleMetadata(
    /** Indexed array of module identifiers (0-based). All other files reference by index. */
    val modules: List<String>,
    /** Functional unit name -> list of module indices belonging to that unit */
    val functionalUnits: Map<String, List<Int>>,
    /** Build variant used for analysis */
    val variant: String? = null,
    /** Dynamic feature module names */
    val dynamicFeatures: List<String>? = null,
)
