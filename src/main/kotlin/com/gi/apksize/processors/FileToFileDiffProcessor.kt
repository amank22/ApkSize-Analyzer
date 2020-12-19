package com.gi.apksize.processors

import com.android.tools.apk.analyzer.Archives
import com.android.tools.apk.analyzer.internal.ApkFileByFileDiffParser
import com.android.tools.apk.analyzer.internal.ApkFileByFileEntry
import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.models.FileByFileSizeDiffModel
import com.gi.apksize.processors.ApkFileProcessor.BYTE_TO_KB_DIVIDER
import java.nio.file.Path
import javax.swing.tree.DefaultMutableTreeNode

object FileToFileDiffProcessor {

    fun diff(apk: Path, apk2: Path, analyzerOptions: AnalyzerOptions): List<FileByFileSizeDiffModel> {
        val list = mutableListOf<FileByFileSizeDiffModel>()
        val arch = Archives.open(apk, DexFileProcessor.CustomLogger())
        arch.use {
            val arch2 = Archives.open(apk2, DexFileProcessor.CustomLogger())
            arch2.use { aContext2 ->
                try {
                    val diffTree = ApkFileByFileDiffParser.createTreeNode(it, aContext2)
                    createNodes(diffTree, list, analyzerOptions)
                } catch (e: Throwable) {
                    println(e)
                }
            }
        }
        return list.sortedByDescending { it.sizeDiff }
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
            val model = FileByFileSizeDiffModel(entry.name, entry.oldSize, entry.newSize,
            sizeDiffOfNode, sizeDiffOfNode / BYTE_TO_KB_DIVIDER, path)
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