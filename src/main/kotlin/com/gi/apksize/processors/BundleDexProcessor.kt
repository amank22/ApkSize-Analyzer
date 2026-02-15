package com.gi.apksize.processors

import com.android.tools.apk.analyzer.dex.*
import com.android.tools.apk.analyzer.dex.tree.DexElementNode
import com.android.tools.apk.analyzer.dex.tree.DexPackageNode
import com.android.tools.apk.analyzer.FilteredTreeModel
import com.android.tools.proguard.ProguardMap
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import com.gi.apksize.models.*
import com.gi.apksize.utils.Constants
import com.gi.apksize.utils.Printer
import java.io.File
import java.nio.file.Files
import javax.swing.tree.TreeModel

/**
 * DEX package analysis for AAB files.
 * Extracts DEX files from all bundle modules, writes them to temp files,
 * then reuses the same package tree analysis logic as [DexFileProcessor].
 *
 * ProGuard mapping is resolved in this order:
 * 1. Embedded mapping from AAB at BUNDLE-METADATA/com.android.tools.build.obfuscation/proguard.map
 * 2. Externally provided mapping file via [DataHolder.primaryFile.proguardFile]
 */
class BundleDexProcessor(
    private val bundleHolder: BundleHolder,
    private val lobContext: LobContext? = null,
) : SimpleProcessor() {

    companion object {
        private const val OBFUSCATION_NAMESPACE = "com.android.tools.build.obfuscation"
        private const val PROGUARD_MAP_FILE = "proguard.map"
    }

    override val name: String = "Bundle Dex Files"

    /**
     * Resolves the ProGuard mapping file to use for deobfuscation.
     * Prefers the embedded mapping from AAB metadata; falls back to an externally provided file.
     */
    private fun resolveProguardFile(dataHolder: DataHolder): File? {
        // Try embedded mapping from AAB metadata first
        try {
            val metadata = bundleHolder.appBundle.bundleMetadata
            val embeddedMapping = metadata.getFileAsByteSource(OBFUSCATION_NAMESPACE, PROGUARD_MAP_FILE)
            if (embeddedMapping.isPresent) {
                val tempFile = Files.createTempFile("bundle-proguard-", ".map")
                embeddedMapping.get().copyTo(Files.newOutputStream(tempFile))
                Printer.log("Using embedded ProGuard mapping from AAB metadata")
                return tempFile.toFile()
            }
        } catch (e: Exception) {
            Printer.log("Could not extract embedded ProGuard mapping: ${e.message}")
        }

        // Fall back to externally provided mapping file
        val externalFile = dataHolder.primaryFile.proguardFile
        if (externalFile != null) {
            Printer.log("Using externally provided ProGuard mapping: ${externalFile.absolutePath}")
        } else {
            Printer.log("No ProGuard mapping available — DEX packages will show obfuscated names")
        }
        return externalFile
    }

    override fun process(dataHolder: DataHolder, apkStats: ApkStats) {
        val appBundle = bundleHolder.appBundle
        val analyzerOptions = dataHolder.analyzerOptions
        val proguardFile = resolveProguardFile(dataHolder)

        val dexBackedDexList = mutableListOf<DexBackedDexFile>()
        val dexPackagesList = mutableListOf<DexPackageModel>()
        val lobDexPackagesList = if (lobContext != null) mutableListOf<DexPackageModel>() else null
        val treeRoot = DexPackageTreeModel("root", 0L, 0, 0L)

        for ((moduleName, module) in appBundle.modules) {
            val dexEntries = module.findEntriesUnderPath(
                com.android.tools.build.bundletool.model.BundleModule.DEX_DIRECTORY
            ).toList()

            Printer.log("Module '${moduleName.name}' has ${dexEntries.size} DEX entries")

            for (dexEntry in dexEntries) {
                try {
                    // Write DEX content to a temp file for DexFiles parsing
                    val tempDexFile = Files.createTempFile("bundle-dex-", ".dex")
                    dexEntry.content.copyTo(Files.newOutputStream(tempDexFile))

                    val dexFile = DexFiles.getDexFile(tempDexFile)
                    dexBackedDexList.add(dexFile)

                    Printer.log("Processing DEX: ${moduleName.name}/${dexEntry.path}")
                    generateDexPackageModel(proguardFile, dexFile, treeRoot, dexPackagesList, analyzerOptions, lobDexPackagesList)

                    Files.deleteIfExists(tempDexFile)
                } catch (e: Exception) {
                    Printer.log("Failed to process DEX entry ${dexEntry.path}: ${e.message}")
                }
            }
        }

        Printer.log("Processed all DEX files from AAB")

        // Clean up temp ProGuard file if it was extracted from AAB metadata
        if (proguardFile != null && proguardFile.name.startsWith("bundle-proguard-")) {
            proguardFile.delete()
        }

        // Collect unfiltered DEX packages for LOB analysis (no size filter, respects depth filter)
        if (lobContext != null && lobDexPackagesList != null && lobDexPackagesList.isNotEmpty()) {
            val lobUniqueList = lobDexPackagesList.groupBy { it.basePackage }.map { (pkg, entries) ->
                DexPackageModel(pkg, entries.sumOf { it.basePackageSize }, entries[0].depth)
            }
            lobContext.collectDexPackages(lobUniqueList)
        }

        val uniquePackagesMap = dexPackagesList.groupBy { it.basePackage }
        val uniquePackageList = uniquePackagesMap.map {
            val list = it.value
            val item = list[0]
            if (list.size > 1) {
                val size = list.sumOf { i -> i.basePackageSize }
                item.basePackageSize = size
            }
            item.packageSizeKb = item.basePackageSize / Constants.BYTE_TO_KB_DIVIDER
            item
        }.filterNot { it.depth <= analyzerOptions.dexPackagesMinDepth }
            .sortedByDescending { it.basePackageSize }

        val appPackages = uniquePackageList
            .filter { p -> analyzerOptions.appPackagePrefix.any { p.basePackage.startsWith(it) } }
            .take(analyzerOptions.appPackagesMaxCount)

        val dexStats = DexFileStats.create(dexBackedDexList)
        apkStats.dexStats = dexStats
        apkStats.dexPackages = uniquePackageList.take(analyzerOptions.dexPackagesMaxCount)
        apkStats.appPackages = appPackages
    }

    /**
     * Generates Dex Model with packages, optionally using a ProGuard mapping file.
     * Reuses the same tree-building logic as [DexFileProcessor].
     */
    private fun generateDexPackageModel(
        proguardMappingFile: File?,
        dexBackedDex: DexBackedDexFile,
        treeModel: DexPackageTreeModel,
        dexPackagesList: MutableList<DexPackageModel>,
        analyzerOptions: AnalyzerOptions,
        lobPackagesList: MutableList<DexPackageModel>? = null,
    ) {
        val proguardMappings = if (proguardMappingFile != null) {
            val proguardMap = ProguardMap()
            proguardMap.readFromFile(proguardMappingFile)
            ProguardMappings(proguardMap, null, null)
        } else {
            null
        }
        val rootNode: DexPackageNode =
            PackageTreeCreator(proguardMappings, proguardMappings != null)
                .constructPackageTree(dexBackedDex)

        // For LOB: traverse the full tree without size filter to capture all packages
        if (lobPackagesList != null) {
            for (i in 0 until rootNode.childCount) {
                val child = rootNode.getChildAt(i)
                if (child is DexPackageNode) {
                    collectPackagesForLob(child, 1, lobPackagesList, analyzerOptions.dexPackagesMinDepth)
                }
            }
        }

        val options = DexViewFilters()
        options.isShowFields = false
        options.isShowMethods = false
        options.isShowReferencedNodes = false
        options.isShowRemovedNodes = false
        val nodesIterator = rootNode.children().asIterator()
        while (nodesIterator.hasNext()) {
            val child = nodesIterator.next()
            val filteredTreeModel: FilteredTreeModel<DexElementNode> = FilteredTreeModel(
                child,
                options
            )
            createNodes(
                filteredTreeModel,
                child as DexElementNode,
                treeModel,
                dexPackagesList,
                1,
                analyzerOptions
            )
        }
    }

    /**
     * Recursively collects ALL packages from the DEX tree for LOB analysis (no size filter).
     * Only the depth filter is applied to avoid overlap with cumulative parent packages.
     */
    private fun collectPackagesForLob(
        node: DexElementNode,
        depth: Int,
        lobList: MutableList<DexPackageModel>,
        minDepth: Int,
    ) {
        if (depth > minDepth) {
            val routePath = node.path.drop(1).joinToString(".") { (it as DexElementNode).name }
            if (routePath.isNotEmpty()) {
                lobList.add(DexPackageModel(routePath, node.size, depth))
            }
        }
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i)
            if (child is DexPackageNode) {
                collectPackagesForLob(child, depth + 1, lobList, minDepth)
            }
        }
    }

    /**
     * Recursively creates package nodes for the DEX tree.
     */
    private fun createNodes(
        model: TreeModel,
        node: DexElementNode,
        treeModel: DexPackageTreeModel,
        dexPackagesList: MutableList<DexPackageModel>,
        depth: Int,
        analyzerOptions: AnalyzerOptions
    ) {
        if (node.size < analyzerOptions.dexPackagesSizeLimiter) return
        val routePath = node.path.drop(1).joinToString(".") {
            (it as DexElementNode).name
        }
        val packageModel = DexPackageModel(routePath, node.size, depth)
        val packageTreeModel = DexPackageTreeModel(routePath, node.size, depth)
        val oldChildren = treeModel.children.orEmpty()
        val updatedChildren = hashSetOf<DexPackageTreeModel>()
        var newConsumed = false
        oldChildren.forEach {
            if (it == packageTreeModel) {
                updatedChildren.add(packageTreeModel.copy(basePackageSize = it.basePackageSize + packageTreeModel.basePackageSize))
                newConsumed = true
            } else {
                updatedChildren.add(it)
            }
        }
        if (!newConsumed) {
            updatedChildren.add(packageTreeModel)
        }
        treeModel.children = updatedChildren
        val count = model.getChildCount(node)
        for (i in 0 until count) {
            createNodes(
                model,
                node.getChildAt(i) as DexElementNode,
                packageTreeModel,
                dexPackagesList,
                depth + 1,
                analyzerOptions
            )
        }
        dexPackagesList.add(packageModel)
    }
}
