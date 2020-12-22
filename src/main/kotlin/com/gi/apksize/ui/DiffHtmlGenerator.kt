package com.gi.apksize.ui

import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DexPackageDiffModel
import com.gi.apksize.models.FileByFileSizeDiffModel
import com.gi.apksize.processors.ApkFileProcessor
import org.celtric.kotlin.html.*
import java.text.NumberFormat
import java.util.*
import kotlin.math.absoluteValue

object DiffHtmlGenerator {

    fun getHtml(apkStats: ApkStats): String {
        return generate(apkStats)
    }

    private fun generate(apkStats: ApkStats): String {
        val document = doctype("html") + html { head() + body(apkStats) }
        return document.render()
    }

    private fun head(): BlockElement {
        return head {
            title("ApkSize Analyzer") +
                    meta(charset = "utf-8") +
                    meta(name = "viewport", content = "width=device-width, initial-scale=1") +
                    link(rel = "stylesheet", href = "https://cdn.jsdelivr.net/npm/bulma@0.9.1/css/bulma.min.css")
        }
    }

    private fun body(apkStats: ApkStats): BlockElement {
        return body {
            section(classes = "section") {
                header() + appName(apkStats) + apkFileSizeStats(apkStats) +
                        apkDataColumns(apkStats) +
                        footerByAman()
            }
        }
    }

    private fun footerByAman(): BlockElement {
        return footer("footer") {
            div("content has-text-centered") {
                p {
                    strong("Apk Size Analyzer") + " by " + a("https://github.com/amank22/ApkSize-Analyzer") {
                        strong("@amank22")
                    }
                }
            }
        }
    }

    private fun appName(apkStats: ApkStats): BlockElement {
        return if (!apkStats.apkName.isNullOrBlank()) {
            div("level mt-6") {
                p("title is-2 level-item has-text-centered has-text-info") {
                    apkStats.apkName!!
                }
            }
        } else {
            div { }
        }
    }

    private fun apkDataColumns(apkStats: ApkStats): BlockElement {
        val items = listOf(
            "File to File Differences", "Dex Packages Differences"
        )
        return columns {
            val item3List = items.chunked(1)
            item3List.map { list ->
                column {
                    list.map {
                        div("tile is-child") {
                            when (it) {
                                "File to File Differences" -> fileByFileDiffsPanel(apkStats.fileDiffs, it)
                                "Dex Packages Differences" -> dexPackagesPanel(apkStats.dexPackagesDiffs, it)
                                else -> div { }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun columns(content: () -> Any): BlockElement {
        return div("tile is-ancestor is-mobile m-6") {
            content()
        }
    }

    private fun column(content: () -> Any): BlockElement {
        return div("tile is-parent") {
            content()
        }
    }

    private fun header(): BlockElement {
        return div(classes = "container") {
            h1(classes = "title is-2 has-text-danger has-text-centered has-text-weight-bold") {
                "Apk Size Analyzer (Diff Tool)"
            } + p(classes = "subtitle is-5 has-text-centered is-family-secondary") {
                text("Deeper Insights for your ") + strong("Apk") + "!"
            }
        }
    }

    private fun apkFileSizeStats(apkStats: ApkStats): BlockElement {
        return nav(classes = "columns is-mobile is-centered is-multiline mt-6 pt-3") {
            div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Base Apk Raw Size"
                    } + p(classes = "title") {
                        "${apkStats.apkSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Base Apk Download Size"
                    } + p(classes = "title") {
                        "${apkStats.downloadSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Compared Apk Raw Size"
                    } + p(classes = "title") {
                        "${apkStats.compareApkSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Compared Apk Download Size"
                    } + p(classes = "title") {
                        "${apkStats.compareDownloadSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Diff in Raw Size"
                    } + p(classes = "title ${getColorClassForSize(apkStats.diffApkSize)}") {
                        "${getSignedTextForNumber(apkStats.diffApkSizeInMb)} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Diff in Download Size"
                    } + p(classes = "title ${getColorClassForSize(apkStats.diffDownloadSize)}") {
                        "${getSignedTextForNumber(apkStats.diffDownloadSizeInMb)} MB"
                    }
                }
            }
        }
    }

    private fun getColorClassForSize(number: Long?): String {
        return if (number ?: 0 > 0) {
            "has-text-danger"
        } else {
            "has-text-success"
        }
    }

    private fun getSignedTextForNumber(number: Long?) : String {
        val n = number?:0
        return when {
            n < 0 -> "-${n.absoluteValue}"
            else -> "+${n.absoluteValue}"
        }
    }
    private fun getSignedTextForNumber(number: Double?) : String {
        val n = number?:0.0
        return when {
            n < 0.0 -> "-${n.absoluteValue}"
            else -> "+${n.absoluteValue}"
        }
    }

    private fun giveFormattedNumberText(number: Int?): String {
        return try {
            NumberFormat.getNumberInstance(Locale("hi", "IN"))
                .format(number)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    private fun fileByFileDiffsPanel(items: List<FileByFileSizeDiffModel>?, title: String): BlockElement {
        return article(classes = "panel") {
            p(classes = "panel-heading") {
                title
            } +
                    if (!items.isNullOrEmpty()) {
                        items.map {
                            panelItemRow(it)
                        }
                    } else {
                        emptyList<String>().map { div {} }
                    }
        }
    }

    private fun dexPackagesPanel(dexPackages: List<DexPackageDiffModel>?, title: String): BlockElement {
        return article(classes = "panel") {
            p(classes = "panel-heading") {
                title
            } +
                    if (!dexPackages.isNullOrEmpty()) {
                        dexPackages.map {
                            panelItemDexRow(it)
                        }
                    } else {
                        emptyList<String>().map { div {} }
                    }
        }
    }

    private fun panelItemRow(diffModel: FileByFileSizeDiffModel): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    diffModel.name
                } + p(classes = "subtitle ${getColorClassForSize(diffModel.sizeDiff)}") {
                    "${getSignedTextForNumber(diffModel.sizeDiffKb)} Kb"
                } + p(classes = "is-medium has-text-grey is-italic has-text-weight-light") {
                    "Old Size: ${diffModel.oldSize / ApkFileProcessor.BYTE_TO_KB_DIVIDER} Kb"
                } + p(classes = "is-medium has-text-grey is-italic has-text-weight-light") {
                    "New Size: ${diffModel.newSize / ApkFileProcessor.BYTE_TO_KB_DIVIDER} Kb"
                } + p(classes = "is-medium has-text-grey is-italic has-text-weight-light") {
                    "Path: ${diffModel.path} Kb"
                }
            }
        }
    }

    private fun panelItemDexRow(dexPackage: DexPackageDiffModel): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    dexPackage.basePackage
                } + p(classes = "subtitle has-text-danger") {
                    "${getSignedTextForNumber(dexPackage.packageSizeDiffKb)} Kb"
                } + p(classes = "is-medium has-text-grey is-italic has-text-weight-light") {
                    "Old Size: ${dexPackage.oldPackageSizeKb} Kb"
                } + p(classes = "is-medium has-text-grey is-italic has-text-weight-light") {
                    "New Size: ${dexPackage.newPackageSizeKb} Kb"
                }
            }
        }
    }

}