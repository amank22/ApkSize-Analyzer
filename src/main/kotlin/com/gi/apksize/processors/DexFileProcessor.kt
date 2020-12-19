package com.gi.apksize.processors

import com.android.tools.apk.analyzer.ArchiveTreeStructure
import com.android.tools.apk.analyzer.Archives
import com.android.tools.apk.analyzer.FilteredTreeModel
import com.android.tools.apk.analyzer.dex.*
import com.android.tools.apk.analyzer.dex.tree.DexElementNode
import com.android.tools.apk.analyzer.dex.tree.DexPackageNode
import com.android.tools.proguard.ProguardMap
import com.android.utils.ILogger
import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DexPackageModel
import com.intellij.util.containers.SortedList
import org.jetbrains.kotlin.utils.addToStdlib.sumByLong
import org.jf.dexlib2.dexbacked.DexBackedDexFile
import java.io.File
import java.nio.file.Path
import javax.swing.tree.TreeModel

object DexFileProcessor {

    /**
     * Calculates stats in the dex files (java/kotlin code) with sizes & other things.
     */
    fun calculateDexStats(
        apk: Path,
        apkStats: ApkStats,
        proguardMappingFile: File?,
        analyzerOptions: AnalyzerOptions,
        isCompareFile: Boolean = false,
        needAppPackages: Boolean = true
    ) {
        val arch = Archives.open(apk, CustomLogger())
        arch.use {
            val treeStructure = ArchiveTreeStructure.create(arch)
            val dexFilePaths = treeStructure.children.asSequence()
                .filter { it.data.summaryDisplayString.endsWith(".dex") }.map {
                    it.data.path
                }.toList()
            println("No. of Dex Files: ${dexFilePaths.size}")
            val dexBackedDexList = mutableListOf<DexBackedDexFile>()
            val dexPackagesList = SortedList<DexPackageModel> { t, t2 ->
                t2.basePackageSize.compareTo(t.basePackageSize)
            }
            dexFilePaths.forEach {
                val dexFile = DexFiles.getDexFile(it)
                dexBackedDexList.add(dexFile)
                println("Dex File: ${it.fileName}")
                generateDexPackageModel(proguardMappingFile, dexFile, dexPackagesList, analyzerOptions)
            }
            val uniquePackagesMap = dexPackagesList.groupBy { it.basePackage }

            val uniquePackageList = uniquePackagesMap.map {
                val list = it.value
                val item = list[0]
                if (list.size > 1) {
                    val size = list.sumByLong { i -> i.basePackageSize }
                    item.basePackageSize = size
                }
                item.packageSizeKb = item.basePackageSize / ApkFileProcessor.BYTE_TO_KB_DIVIDER
                item
            }.filterNot { it.depth <= analyzerOptions.dexPackagesMinDepth }
            val appPackages = uniquePackageList.filter { it.basePackage.startsWith(analyzerOptions.appPackagePrefix) }
                .take(analyzerOptions.appPackagesMaxCount)
            val dexStats = DexFileStats.create(dexBackedDexList)
            if (!isCompareFile) {
                apkStats.dexStats = dexStats
                apkStats.dexPackages = uniquePackageList.take(analyzerOptions.dexPackagesMaxCount)
                if (needAppPackages)
                    apkStats.appPackages = appPackages
            } else {
                apkStats.comparedDexStats = dexStats
                apkStats.comparedDexPackages = uniquePackageList.take(analyzerOptions.dexPackagesMaxCount)
                if (needAppPackages)
                    apkStats.comparedAppPackages = appPackages
            }
        }
    }

    /**
     * Generates Dex Model with packages with the proguard file
     */
    private fun generateDexPackageModel(
        proguardMappingFile: File?,
        dexBackedDex: DexBackedDexFile,
        dexPackagesList: MutableList<DexPackageModel>,
        analyzerOptions: AnalyzerOptions
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
        val options = DexViewFilters()
        options.isShowFields = false
        options.isShowMethods = false
        options.isShowReferencedNodes = false
        options.isShowRemovedNodes = false
        val filteredTreeModel: FilteredTreeModel<DexElementNode> = FilteredTreeModel(
            rootNode.firstChild,
            options
        )
        createNodes(filteredTreeModel, rootNode.firstChild as DexElementNode, dexPackagesList, 1, analyzerOptions)
    }

    /**
     * This method creates nodes for the packages in the dex
     */
    private fun createNodes(
        model: TreeModel,
        node: DexElementNode,
        dexPackagesList: MutableList<DexPackageModel>,
        depth: Int,
        analyzerOptions: AnalyzerOptions
    ) {
        if (node.size < analyzerOptions.dexPackagesSizeLimiter) return
        val routePath = node.path.drop(1).joinToString(".") {
            (it as DexElementNode).name
        }
        val packageModel = DexPackageModel(routePath, node.size, depth)
        val count = model.getChildCount(node)
        for (i in 0 until count) {
            createNodes(
                model,
                node.getChildAt(i) as DexElementNode,
                dexPackagesList,
                depth + 1,
                analyzerOptions
            )
        }
        dexPackagesList.add(packageModel)
    }

    class CustomLogger : ILogger {
        override fun error(t: Throwable?, msgFormat: String?, vararg args: Any?) {
            println("Error: $msgFormat")
        }

        override fun warning(msgFormat: String?, vararg args: Any?) {
            println("Warning: $msgFormat")
        }

        override fun info(msgFormat: String?, vararg args: Any?) {
            println("Info: $msgFormat")
        }

        override fun verbose(msgFormat: String?, vararg args: Any?) {
            println("Verbose: $msgFormat")
        }

    }

}