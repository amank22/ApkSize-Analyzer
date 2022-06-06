package com.gi.apksize.ui

import com.gi.apksize.models.ApkFileData
import com.gi.apksize.models.ApkGroupSizes
import com.gi.apksize.models.ApkStats
import com.gi.apksize.models.DexPackageModel
import org.celtric.kotlin.html.*
import java.text.NumberFormat
import java.util.*

object HtmlGenerator {

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
                        apkResourcesStats(apkStats) +
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
            "Biggest Images", "Top Packaged Files", "Dex Packages", "App Packages",
            "Apk Group Sizes", "Top Filtered Files"
        )
        return columns {
            val item3List = items.chunked(3)
            item3List.map { list ->
                column {
                    list.map {
                        div("tile is-child") {
                            when (it) {
                                "Biggest Images" -> topGenericFilesPanel(apkStats.topImages, it)
                                "Top Packaged Files" -> topGenericFilesPanel(apkStats.topFiles, it)
                                "App Packages" -> dexPackagesPanel(apkStats.appPackages, it)
                                "Dex Packages" -> dexPackagesPanel(apkStats.dexPackages, it)
                                "Top Filtered Files" -> topGenericFilesPanel(apkStats.topFilteredFiles, it)
                                "Apk Group Sizes" -> groupSizesPanel(apkStats.groupSizes, it)
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
        return div("tile is-parent is-vertical is-6") {
            content()
        }
    }

    private fun header(): BlockElement {
        return div(classes = "container") {
            h1(classes = "title is-2 has-text-danger has-text-centered has-text-weight-bold") {
                "Apk Size Analyzer"
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
                        "Apk Raw Size"
                    } + p(classes = "title") {
                        "${apkStats.apkSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Apk Download Size"
                    } + p(classes = "title") {
                        "${apkStats.downloadSizeInMb} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "React Bundle Size"
                    } + p(classes = "title") {
                        "${apkStats.reactBundleSizeInMb ?: 0} MB"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Total Classes"
                    } + p(classes = "title has-text-grey") {
                        giveFormattedNumberText(apkStats.dexStats?.classCount)
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Total Defined Methods"
                    } + p(classes = "title has-text-grey") {
                        giveFormattedNumberText(apkStats.dexStats?.definedMethodCount)
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "Total Referenced Methods"
                    } + p(classes = "title has-text-grey") {
                        giveFormattedNumberText(apkStats.dexStats?.referencedMethodCount)
                    }
                }
            }
        }
    }

    private fun apkResourcesStats(apkStats: ApkStats): BlockElement {
        val resourceMap = apkStats.resourcesMap ?: return div { }
        val nav = nav(classes = "columns is-mobile is-centered is-multiline pt-3") {
            div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. Of Drawables"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("drawable", 0L)}"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. of Strings"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("string", 0L)}"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. of raw files"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("raw", 0L)}"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. of layouts"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("layout", 0L)}"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. of mipmap"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("mipmap", 0L)}"
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "No. of font"
                    } + p(classes = "title") {
                        "${resourceMap.getOrDefault("font", 0L)}"
                    }
                }
            }
        }
        return div(classes = "container") {
            h1(classes = "title has-text-info has-text-centered has-text-weight-bold mt-6") {
                "Resources Insights"
            } + nav
        }
    }

    private fun giveFormattedNumberText(number: Int?): String {
        return try {
            NumberFormat.getNumberInstance(Locale.ENGLISH)
                .format(number)
        } catch (e: NumberFormatException) {
            "Unknown"
        }
    }

    private fun topGenericFilesPanel(items: List<ApkFileData>?, title: String): BlockElement {
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

    private fun dexPackagesPanel(dexPackages: List<DexPackageModel>?, title: String): BlockElement {
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

    private fun panelItemRow(apkFileData: ApkFileData): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    apkFileData.simpleFileName
                } + p(classes = "subtitle has-text-danger") {
                    "${apkFileData.sizeInKb} Kb"
                }
            } +
                    span(classes = "tag is-info is-light is-hidden-mobile") {
                        apkFileData.fileType.fileSubType
                    }
        }
    }

    private fun panelItemDexRow(dexPackage: DexPackageModel): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    dexPackage.basePackage
                } + p(classes = "subtitle has-text-danger") {
                    "${dexPackage.packageSizeKb} Kb"
                }
            }
        }
    }

    private fun groupSizesPanel(map: HashMap<String, ApkGroupSizes>?, title: String): BlockElement {
        return article(classes = "panel") {
            p(classes = "panel-heading") {
                title
            } +
                    if (!map.isNullOrEmpty()) {
                        map.map {
                            panelItemGroupRow(it.key, it.value)
                        }
                    } else {
                        emptyList<String>().map { div {} }
                    }
        }
    }

    private fun panelItemGroupRow(groupName: String, group: ApkGroupSizes): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    groupName
                } + p(classes = "subtitle has-text-danger") {
                    "${group.groupSizeMb} MB"
                } + if (!group.subGroups.isNullOrEmpty()) {
                    div(classes = "mt-4") {
                        strong("Top SubGroups")
                    }
                } else {
                    div {}
                } + div(classes = "content is-small") {
                    ul {
                        group.subGroups?.map {
                            li {
                                text(it.key) + " (" + strong { "${it.value.sizeInKb} Kb" } + ")"
                            }
                        } ?: emptyList<String>().map { li {} }
                    }
                }
            }
        }
    }

}