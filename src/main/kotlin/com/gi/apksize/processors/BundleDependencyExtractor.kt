package com.gi.apksize.processors

import com.gi.apksize.models.BundleDependencyInfo
import com.gi.apksize.utils.Printer
import com.gi.apksize.utils.ProtoWireReader
import com.gi.apksize.utils.ProtoWireReader.Companion.WIRE_LENGTH_DELIMITED
import java.io.File
import java.util.zip.ZipFile

/**
 * Extracts Maven dependency info from an AAB's
 * `BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb`
 * **without** requiring protobuf-java or aapt2-proto on the classpath.
 *
 * This is used by the lite-JAR code path where the bundletool Java API is
 * unavailable. The full-JAR path uses [BundleMetadataProcessor] instead.
 */
object BundleDependencyExtractor {

    private const val DEPS_ENTRY_PATH =
        "BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb"

    /**
     * Reads `dependencies.pb` directly from the AAB ZIP and returns parsed
     * dependency coordinates, sorted by group:artifact.
     */
    fun extractFromAabZip(aabPath: String): List<BundleDependencyInfo> {
        val bytes = ZipFile(File(aabPath)).use { zip ->
            zip.getEntry(DEPS_ENTRY_PATH)?.let { entry ->
                zip.getInputStream(entry).readBytes()
            }
        }
        if (bytes == null) {
            Printer.log("No dependencies.pb found in AAB metadata")
            return emptyList()
        }
        return parseAppDependenciesProto(bytes)
    }

    // ------------------------------------------------------------------
    //  Wire-format decoders for the AppDependencies proto hierarchy
    //
    //  Schema (bundletool 1.18.x):
    //    message AppDependencies { repeated Library library = 1; }
    //    message Library          { MavenLibrary maven_library = 1; ... }
    //    message MavenLibrary     { string group_id = 1; string artifact_id = 2;
    //                               string packaging = 3; string classifier = 4;
    //                               string version = 5; }
    // ------------------------------------------------------------------

    private fun parseAppDependenciesProto(data: ByteArray): List<BundleDependencyInfo> {
        val result = mutableListOf<BundleDependencyInfo>()
        val reader = ProtoWireReader(data)
        while (reader.hasRemaining()) {
            val tag = reader.readTag()
            if (tag.wireType == WIRE_LENGTH_DELIMITED && tag.fieldNumber == 1) {
                parseLibraryProto(reader.readBytes())?.let { result.add(it) }
            } else {
                reader.skip(tag.wireType)
            }
        }
        return result
            .distinctBy { "${it.groupId}:${it.artifactId}:${it.version}" }
            .sortedBy { "${it.groupId}:${it.artifactId}" }
    }

    private fun parseLibraryProto(data: ByteArray): BundleDependencyInfo? {
        val reader = ProtoWireReader(data)
        while (reader.hasRemaining()) {
            val tag = reader.readTag()
            if (tag.wireType == WIRE_LENGTH_DELIMITED && tag.fieldNumber == 1) {
                return parseMavenLibraryProto(reader.readBytes())
            } else {
                reader.skip(tag.wireType)
            }
        }
        return null
    }

    private fun parseMavenLibraryProto(data: ByteArray): BundleDependencyInfo? {
        val reader = ProtoWireReader(data)
        var groupId: String? = null
        var artifactId: String? = null
        var version: String? = null
        while (reader.hasRemaining()) {
            val tag = reader.readTag()
            if (tag.wireType == WIRE_LENGTH_DELIMITED) {
                val value = reader.readString()
                when (tag.fieldNumber) {
                    1 -> groupId = value
                    2 -> artifactId = value
                    5 -> version = value
                }
            } else {
                reader.skip(tag.wireType)
            }
        }
        return if (groupId != null && artifactId != null) {
            BundleDependencyInfo(groupId = groupId, artifactId = artifactId, version = version ?: "")
        } else null
    }
}
