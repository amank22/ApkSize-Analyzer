package com.apkanalyzer.modulesize

// ── Resource extraction results ──────────────────────────────────────────

data class ResourceCount(
    val total: Int = 0,
    val byType: Map<String, Int> = emptyMap(),
)

data class ResourceResult(
    val declared: ResourceCount = ResourceCount(),
    val totalRTxtEntries: Int = 0,
    val transitive: Int = 0,
    val warnings: MutableList<String> = mutableListOf(),
)

// ── Class extraction results ─────────────────────────────────────────────

data class ClassScanResult(
    val packages: MutableMap<String, Int> = mutableMapOf(),
    val classCount: Int = 0,
)

data class ClassResult(
    val packages: MutableMap<String, Int> = mutableMapOf(),
    val classCount: Int = 0,
    val warnings: MutableList<String> = mutableListOf(),
)

// ── Native lib info ──────────────────────────────────────────────────────

data class NativeLibInfo(
    val abi: String,
    val name: String,
    val sizeBytes: Long,
)
