package com.gi.apksize.processors

import com.android.bundle.AppDependenciesOuterClass.AppDependencies
import com.gi.apksize.models.*
import com.gi.apksize.utils.Printer

/**
 * Extracts AAB-specific metadata not available in APKs:
 * - AndroidManifest info (package name, version, min/target SDK)
 * - BundleConfig.pb (compression, splits, optimization flags)
 * - App dependencies from BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb
 */
class BundleMetadataProcessor(private val bundleHolder: BundleHolder) : SimpleProcessor() {

    override val name: String = "Bundle Metadata"

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val appBundle = bundleHolder.appBundle

        // Pass app module prefixes from config so the HTML generator can group dependencies
        apkStats.appModulePrefixes = dataHolder.analyzerOptions.appModulePrefixes

        extractManifestInfo(appBundle, apkStats)
        extractBundleConfig(appBundle, apkStats)
        extractDependencies(appBundle, apkStats)
    }

    /**
     * Extracts manifest info from the base module's AndroidManifest.xml.
     */
    private fun extractManifestInfo(appBundle: com.android.tools.build.bundletool.model.AppBundle, apkStats: ApkStats) {
        try {
            val baseModule = appBundle.baseModule
            val manifest = baseModule.androidManifest

            apkStats.manifestPackageName = manifest.packageName
            apkStats.manifestVersionCode = manifest.versionCode.orElse(null)
            apkStats.manifestVersionName = manifest.versionName.orElse(null)
            apkStats.manifestMinSdk = manifest.minSdkVersion.orElse(null)
            apkStats.manifestTargetSdk = manifest.targetSdkVersion.orElse(null)

            Printer.log("Manifest: package=${manifest.packageName}, " +
                    "versionCode=${apkStats.manifestVersionCode}, " +
                    "minSdk=${apkStats.manifestMinSdk}, " +
                    "targetSdk=${apkStats.manifestTargetSdk}")
        } catch (e: Exception) {
            Printer.log("Failed to extract manifest info: ${e.message}")
        }
    }

    /**
     * Extracts bundle configuration from BundleConfig.pb.
     */
    private fun extractBundleConfig(appBundle: com.android.tools.build.bundletool.model.AppBundle, apkStats: ApkStats) {
        try {
            val config = appBundle.bundleConfig

            val splitDimensions = config.optimizations.splitsConfig.splitDimensionList
                .filter { !it.negate }
                .map { it.value.name }

            val bundleConfigInfo = BundleConfigInfo(
                bundletoolVersion = config.bundletool.version,
                compressionUncompressedGlobs = config.compression.uncompressedGlobList.toList(),
                splitDimensions = splitDimensions,
                uncompressNativeLibs = config.optimizations.uncompressNativeLibraries.enabled,
                uncompressDex = config.optimizations.uncompressDexFiles.enabled
            )
            apkStats.bundleConfig = bundleConfigInfo

            Printer.log("Bundle config: bundletool=${bundleConfigInfo.bundletoolVersion}, " +
                    "splits=${bundleConfigInfo.splitDimensions}, " +
                    "uncompressNative=${bundleConfigInfo.uncompressNativeLibs}")
        } catch (e: Exception) {
            Printer.log("Failed to extract bundle config: ${e.message}")
        }
    }

    /**
     * Extracts app dependencies from BUNDLE-METADATA/com.android.tools.build.libraries/dependencies.pb.
     */
    private fun extractDependencies(appBundle: com.android.tools.build.bundletool.model.AppBundle, apkStats: ApkStats) {
        try {
            val metadata = appBundle.bundleMetadata
            val depsNamespace = "com.android.tools.build.libraries"
            val depsFileName = "dependencies.pb"

            val depsFile = metadata.getFileAsByteSource(depsNamespace, depsFileName)
            if (!depsFile.isPresent) {
                Printer.log("No dependencies.pb found in bundle metadata")
                return
            }

            val appDependencies = AppDependencies.parseFrom(depsFile.get().read())
            val dependencyList = appDependencies.libraryList.mapNotNull { library ->
                if (library.hasMavenLibrary()) {
                    val maven = library.mavenLibrary
                    BundleDependencyInfo(
                        groupId = maven.groupId,
                        artifactId = maven.artifactId,
                        version = maven.version
                    )
                } else {
                    null
                }
            }.distinctBy { "${it.groupId}:${it.artifactId}:${it.version}" }
                .sortedBy { "${it.groupId}:${it.artifactId}" }

            apkStats.bundleDependencies = dependencyList
            Printer.log("Found ${dependencyList.size} dependencies in bundle metadata")
        } catch (e: Exception) {
            Printer.log("Failed to extract dependencies: ${e.message}")
        }
    }
}
