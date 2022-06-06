package com.gi.apksize.processors

import com.android.tools.apk.analyzer.Archives
import com.android.tools.apk.analyzer.internal.ApkFileByFileDiffParser
import com.android.tools.apk.analyzer.internal.ApkFileByFileEntry
import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DataHolder
import com.gi.apksize.models.FileByFileSizeDiffModel
import com.gi.apksize.utils.Constants
import com.gi.apksize.utils.Printer
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

object FileToFileDiffProcessor {

    @OptIn(ExperimentalTime::class)
    fun diff(dataHolder: DataHolder, apkStats: ApkStats) {
        val analyzerOptions = dataHolder.analyzerOptions
        if (analyzerOptions.disableFileByFileComparison) return
        val apk = dataHolder.primaryFile.file.toPath()
        val apk2 = dataHolder.secondaryFile?.file?.toPath()
        val list = mutableListOf<FileByFileSizeDiffModel>()
        val customLogger = DexFileProcessor.CustomLogger()
        val arch = Archives.open(apk, customLogger)
        arch.use {
            val arch2 = Archives.open(apk2, customLogger)
            arch2.use { aContext2 ->
                try {
                    val diffTree = measureTimedValue {
                        ApkFileByFileDiffParser.createTreeNode(it, aContext2)
                    }
                    createNodes(diffTree.value, list, analyzerOptions)
                } catch (e: Throwable) {
                    Printer.log(e)
                }
            }
        }
        apkStats.fileDiffs = list.sortedByDescending { it.sizeDiff }
    }

    private fun createNodes(
        node: DefaultMutableTreeNode,
        list: MutableList<FileByFileSizeDiffModel>,
        analyzerOptions: AnalyzerOptions
    ) {
        val entry = node.userObject as ApkFileByFileEntry
        val sizeDiffOfNode = entry.newSize - entry.oldSize
        if (sizeDiffOfNode > analyzerOptions.diffSizeLimiter) {
            val path = entry.path.toString()
            val model = FileByFileSizeDiffModel(
                entry.name, entry.oldSize, entry.newSize,
                sizeDiffOfNode, sizeDiffOfNode / Constants.BYTE_TO_KB_DIVIDER, path
            )
            list.add(model)
        }
        val count = node.childCount
        for (i in 0 until count) {
            createNodes(
                node.getChildAt(i) as DefaultMutableTreeNode,
                list,
                analyzerOptions
            )
        }
    }

}