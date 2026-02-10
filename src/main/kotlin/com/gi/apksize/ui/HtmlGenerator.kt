package com.gi.apksize.ui

import com.gi.apksize.models.*
import org.celtric.kotlin.html.*
import java.lang.IllegalArgumentException
import java.text.CharacterIterator
import java.text.NumberFormat
import java.text.StringCharacterIterator
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

    private fun isAab(apkStats: ApkStats): Boolean =
        apkStats.inputFileType == InputFileType.AAB

    private fun fileTypeLabel(apkStats: ApkStats): String =
        if (isAab(apkStats)) "AAB" else "APK"

    private fun body(apkStats: ApkStats): BlockElement {
        return body {
            section(classes = "section") {
                header(apkStats) + appName(apkStats) +
                        manifestInfoSection(apkStats) +
                        apkFileSizeStats(apkStats) +
                        apkResourcesStats(apkStats) +
                        estimatedDownloadSizesSection(apkStats) +
                        bundleModulesSection(apkStats) +
                        apkDataColumns(apkStats) +
                        bundleDependenciesSection(apkStats) +
                        bundleConfigSection(apkStats) +
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
            "Biggest Images", "Biggest other Files", "Dex Packages", "App Packages",
            "File Group Sizes", "Top Packaged Files"
        )
        return columns {
            val item3List = items.chunked(1)
            item3List.map { list ->
                column {
                    list.map {
                        div("tile is-child") {
                            when (it) {
                                "Biggest Images" -> topGenericFilesPanel(apkStats.topImages, it)
                                "Top Packaged Files" -> topGenericFilesPanel(apkStats.topFiles, it)
                                "App Packages" -> dexPackagesPanel(apkStats.appPackages, it)
                                "Dex Packages" -> dexPackagesPanel(apkStats.dexPackages, it)
                                "Biggest other Files" -> topGenericFilesPanel(apkStats.topFilteredFiles, it)
                                "File Group Sizes" -> groupSizesPanel(apkStats.groupSizes, it)
                                else -> div { }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun columns(content: () -> Any): BlockElement {
        return div("tile is-ancestor is-mobile mt-6") {
            content()
        }
    }

    private fun column(content: () -> Any): BlockElement {
        return div("tile is-parent is-horizontal is-full") {
            content()
        }
    }

    private fun header(apkStats: ApkStats): BlockElement {
        val label = fileTypeLabel(apkStats)
        return div(classes = "container") {
            h1(classes = "title is-2 has-text-danger has-text-centered has-text-weight-bold") {
                "$label Size Analyzer"
            } + p(classes = "subtitle is-5 has-text-centered is-family-secondary") {
                text("Deeper Insights for your ") + strong(label) + "!"
            }
        }
    }

    private fun apkFileSizeStats(apkStats: ApkStats): BlockElement {
        val label = fileTypeLabel(apkStats)
        return nav(classes = "columns is-mobile is-centered is-multiline mt-6 pt-3") {
            div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "$label Raw Size"
                    } + p(classes = "title") {
                        humanReadableByteCountSI(apkStats.apkSize)
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "$label Download Size"
                    } + p(classes = "title") {
                        humanReadableByteCountSI(apkStats.downloadSize)
                    }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") {
                        "React Bundle Size"
                    } + p(classes = "title") {
                        humanReadableByteCountSI(apkStats.reactBundleSize)
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
            NumberFormat.getNumberInstance(Locale.US)
                .format(number)
        } catch (e: NumberFormatException) {
            "Unknown"
        } catch (e: IllegalArgumentException) {
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
                div(classes = "tags mb-1") {
                    span(classes = "tag is-info is-light") {
                        apkFileData.fileType.fileSubType
                    } + if (!apkFileData.moduleName.isNullOrEmpty()) {
                        listOf(span(classes = "tag is-success is-light") {
                            apkFileData.moduleName
                        })
                    } else {
                        listOf(text(""))
                    }
                } +
                        p(classes = "title is-size-5 has-text-weight-light") {
                            apkFileData.simpleFileName
                        } + p(classes = "subtitle has-text-danger") {
                    humanReadableByteCountSI(apkFileData.sizeInBytes)
                }
            }
        }
    }

    private fun panelItemDexRow(dexPackage: DexPackageModel): BlockElement {
        return div(classes = "panel-block") {
            div(classes = "container") {
                p(classes = "title is-size-5 has-text-weight-light") {
                    dexPackage.basePackage
                } + p(classes = "subtitle has-text-danger") {
                    humanReadableByteCountSI(dexPackage.basePackageSize) // TODO:: Check whether this is correct or not
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
                    humanReadableByteCountSI(group.groupSize)
                } + if (!group.subGroups.isNullOrEmpty()) {
                    div(classes = "mt-4") {
                        strong("Top SubGroups")
                    }
                } else {
                    div {}
                } + div(classes = "content is-small") {
                    ul {
                        group.subGroups.orEmpty().asIterable().sortedByDescending { it.value.size }.map {
                            li {
                                text(it.key) + " (" + strong { humanReadableByteCountSI(it.value.size) } + ")"
                            }
                        }
                    }
                }
            }
        }
    }

    // region AAB-specific sections

    /**
     * Shows manifest info (package name, version, SDK levels) for AAB analysis.
     */
    private fun manifestInfoSection(apkStats: ApkStats): BlockElement {
        if (!isAab(apkStats) || apkStats.manifestPackageName.isNullOrBlank()) return div { }
        return nav(classes = "columns is-mobile is-centered is-multiline mt-4 pt-3") {
            div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") { "Package" } +
                            p(classes = "title is-size-5") { apkStats.manifestPackageName!! }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") { "Version" } +
                            p(classes = "title is-size-5") {
                                "${apkStats.manifestVersionName ?: "?"} (${apkStats.manifestVersionCode ?: "?"})"
                            }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") { "Min SDK" } +
                            p(classes = "title is-size-5") { "${apkStats.manifestMinSdk ?: "?"}" }
                }
            } + div(classes = "column is-narrow has-text-centered") {
                div {
                    p(classes = "heading") { "Target SDK" } +
                            p(classes = "title is-size-5") { "${apkStats.manifestTargetSdk ?: "?"}" }
                }
            }
        }
    }

    /**
     * Shows per-module breakdown for AAB analysis.
     */
    private fun bundleModulesSection(apkStats: ApkStats): BlockElement {
        val modules = apkStats.bundleModules
        if (modules.isNullOrEmpty()) return div { }
        return div(classes = "container mt-6") {
            h1(classes = "title has-text-info has-text-centered has-text-weight-bold") {
                "Module Breakdown"
            } + p(classes = "subtitle is-6 has-text-centered has-text-grey") {
                "Compressed sizes per module as stored in the AAB (${modules.size} modules)"
            } + div(classes = "columns is-multiline") {
                modules.map { module ->
                    div(classes = "column is-one-third") {
                        div(classes = "box") {
                            // Header: module name + total size
                            p(classes = "title is-5 mb-1") {
                                module.moduleName
                            } + p(classes = "title is-4 has-text-danger mb-3") {
                                humanReadableByteCountSI(module.totalSize)
                            } + div(classes = "tags mb-2") {
                                span(classes = "tag is-info is-light") {
                                    module.deliveryLabel
                                } + span(classes = "tag is-light") {
                                    "${module.fileCount} files"
                                } + span(classes = "tag is-warning is-light") {
                                    "${module.sizePercentage}% of AAB"
                                } + if (module.nativeAbis.isNotEmpty()) {
                                    module.nativeAbis.map { abi ->
                                        span(classes = "tag is-success is-light") { abi }
                                    }
                                } else {
                                    listOf(text(""))
                                }
                            } + p(classes = "is-size-7 has-text-grey mb-3") {
                                module.deliveryDescription
                            } + div(classes = "content is-small") {
                                table(classes = "table is-fullwidth is-narrow") {
                                    thead {
                                        tr {
                                            th { "Component" } + th { "Size" }
                                        }
                                    } + tbody {
                                        tr {
                                            td { "Code / DEX" } +
                                                    td { strong { humanReadableByteCountSI(module.dexSize) } }
                                        } + tr {
                                            td { "Resources" } +
                                                    td { strong { humanReadableByteCountSI(module.resourcesSize) } }
                                        } + tr {
                                            td { "Assets" } +
                                                    td { strong { humanReadableByteCountSI(module.assetsSize) } }
                                        } + tr {
                                            td {
                                                text("Native Libs") +
                                                        if (module.nativeAbis.isNotEmpty()) {
                                                            text(" (${module.nativeAbis.joinToString(", ")})")
                                                        } else {
                                                            text("")
                                                        }
                                            } +
                                                    td { strong { humanReadableByteCountSI(module.nativeLibsSize) } }
                                        } + tr {
                                            td { "Other" } +
                                                    td { strong { humanReadableByteCountSI(module.otherSize) } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows estimated download sizes for different device configurations (low/mid/high-end).
     * These approximate what a user would download from the Play Store.
     */
    private fun estimatedDownloadSizesSection(apkStats: ApkStats): BlockElement {
        val sizes = apkStats.estimatedDeviceSizes
        if (sizes.isNullOrEmpty()) return div { }
        return div(classes = "container mt-6") {
            h1(classes = "title has-text-info has-text-centered has-text-weight-bold") {
                "Estimated Download Sizes"
            } + p(classes = "subtitle is-6 has-text-centered has-text-grey") {
                "Approximate Play Store download sizes for different device tiers (base module install)"
            } + div(classes = "columns is-multiline is-centered") {
                sizes.map { size ->
                    div(classes = "column is-one-third") {
                        div(classes = "box") {
                            p(classes = "title is-4 has-text-centered") {
                                size.configName
                            } + p(classes = "subtitle is-6 has-text-centered has-text-grey") {
                                "${size.abi} | ${size.screenDensityDpi}dpi | SDK ${size.sdkVersion}"
                            } + div(classes = "has-text-centered mb-4") {
                                p(classes = "title is-3 has-text-danger") {
                                    humanReadableByteCountSI(size.totalDownloadBytes)
                                } + p(classes = "heading") {
                                    "Download Size"
                                }
                            } + div(classes = "has-text-centered mb-4") {
                                p(classes = "title is-5 has-text-grey") {
                                    humanReadableByteCountSI(size.totalDiskBytes)
                                } + p(classes = "heading") {
                                    "On-disk Size"
                                }
                            } + div(classes = "content is-small") {
                                table(classes = "table is-fullwidth is-narrow") {
                                    thead {
                                        tr {
                                            th { "Component" } + th { "Download" }
                                        }
                                    } + tbody {
                                        tr {
                                            td { "Code / DEX" } +
                                                    td { strong { humanReadableByteCountSI(size.dexDownloadBytes) } }
                                        } + tr {
                                            td { "Resources" } +
                                                    td { strong { humanReadableByteCountSI(size.resourcesDownloadBytes) } }
                                        } + tr {
                                            td { "Assets" } +
                                                    td { strong { humanReadableByteCountSI(size.assetsDownloadBytes) } }
                                        } + tr {
                                            td { "Native Libs" } +
                                                    td { strong { humanReadableByteCountSI(size.nativeLibsDownloadBytes) } }
                                        } + tr {
                                            td { "Other" } +
                                                    td { strong { humanReadableByteCountSI(size.otherDownloadBytes) } }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows Maven dependencies extracted from AAB metadata, grouped by category:
     * - App Modules (matched via appModulePrefixes config)
     * - Android / Google (androidx.*, com.google.*, com.android.*)
     * - Third-party (everything else)
     */
    private fun bundleDependenciesSection(apkStats: ApkStats): BlockElement {
        val deps = apkStats.bundleDependencies
        if (deps.isNullOrEmpty()) return div { }

        val appPrefixes = apkStats.appModulePrefixes
        val platformPrefixes = listOf("androidx.", "com.google.", "com.android.", "org.jetbrains.")

        data class DepGroup(
            val title: String,
            val tagClass: String,
            val deps: List<BundleDependencyInfo>
        )

        val appModuleDeps = if (appPrefixes.isNotEmpty()) {
            deps.filter { dep -> appPrefixes.any { prefix -> dep.groupId.startsWith(prefix) } }
        } else {
            emptyList()
        }
        val platformDeps = deps.filter { dep ->
            platformPrefixes.any { prefix -> dep.groupId.startsWith(prefix) } &&
                    dep !in appModuleDeps
        }
        val thirdPartyDeps = deps - appModuleDeps.toSet() - platformDeps.toSet()

        val groups = mutableListOf<DepGroup>()
        if (appModuleDeps.isNotEmpty()) {
            groups.add(DepGroup("App Modules (${appModuleDeps.size})", "is-success", appModuleDeps))
        }
        if (platformDeps.isNotEmpty()) {
            groups.add(DepGroup("Android / Google / Kotlin (${platformDeps.size})", "is-info", platformDeps))
        }
        if (thirdPartyDeps.isNotEmpty()) {
            groups.add(DepGroup("Third-party (${thirdPartyDeps.size})", "is-warning", thirdPartyDeps))
        }

        return div(classes = "container mt-6") {
            h1(classes = "title has-text-info has-text-centered has-text-weight-bold") {
                "Dependencies (${deps.size})"
            } + p(classes = "subtitle is-6 has-text-centered has-text-grey") {
                "Maven libraries from dependencies.pb"
            } + div(classes = "columns is-multiline") {
                groups.map { group ->
                    div(classes = "column is-one-third") {
                        article(classes = "panel") {
                            p(classes = "panel-heading") { group.title } +
                                    group.deps.map { dep ->
                                        div(classes = "panel-block") {
                                            div(classes = "container") {
                                                span(classes = "tag ${group.tagClass} is-light mr-2") {
                                                    dep.groupId
                                                } + p(classes = "title is-size-6 has-text-weight-light") {
                                                    dep.artifactId
                                                } + p(classes = "subtitle is-size-7 has-text-grey") {
                                                    dep.version
                                                }
                                            }
                                        }
                                    }
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows bundle configuration details for AAB analysis.
     */
    private fun bundleConfigSection(apkStats: ApkStats): BlockElement {
        val config = apkStats.bundleConfig ?: return div { }

        // Convert raw case-insensitive globs like "**.[pP][nN][gG]" to clean extensions like ".png"
        val cleanedExtensions = config.compressionUncompressedGlobs.map { glob ->
            glob.replace("**", "")
                .replace(Regex("\\[([a-zA-Z0-9])([a-zA-Z0-9])\\]") ) { match ->
                    match.groupValues[1].lowercase()
                }
        }.sorted().distinct()

        return div(classes = "container mt-6") {
            h1(classes = "title has-text-info has-text-centered has-text-weight-bold") {
                "Bundle Configuration"
            } + article(classes = "panel") {
                p(classes = "panel-heading") { "BundleConfig.pb" } +
                        div(classes = "panel-block") {
                            div(classes = "container") {
                                div(classes = "columns is-multiline") {
                                    div(classes = "column is-half") {
                                        p(classes = "heading") { "Bundletool Version" } +
                                                p(classes = "title is-5") { config.bundletoolVersion }
                                    } + div(classes = "column is-half") {
                                        p(classes = "heading") { "Split Dimensions" } +
                                                div(classes = "tags") {
                                                    if (config.splitDimensions.isNotEmpty()) {
                                                        config.splitDimensions.map { dim ->
                                                            span(classes = "tag is-info is-light") { dim }
                                                        }
                                                    } else {
                                                        listOf(span(classes = "tag is-light") { "None" })
                                                    }
                                                }
                                    } + div(classes = "column is-half") {
                                        p(classes = "heading") { "Uncompress Native Libs" } +
                                                span(classes = "tag ${if (config.uncompressNativeLibs) "is-success" else "is-light"}") {
                                                    "${config.uncompressNativeLibs}"
                                                }
                                    } + div(classes = "column is-half") {
                                        p(classes = "heading") { "Uncompress DEX" } +
                                                span(classes = "tag ${if (config.uncompressDex) "is-success" else "is-light"}") {
                                                    "${config.uncompressDex}"
                                                }
                                    } + div(classes = "column is-full") {
                                        p(classes = "heading") { "Uncompressed File Types (${cleanedExtensions.size})" } +
                                                div(classes = "tags") {
                                                    if (cleanedExtensions.isNotEmpty()) {
                                                        cleanedExtensions.map { ext ->
                                                            span(classes = "tag is-warning is-light") { ext }
                                                        }
                                                    } else {
                                                        listOf(span(classes = "tag is-light") { "None" })
                                                    }
                                                }
                                    }
                                }
                            }
                        }
            }
        }
    }

    // endregion

    private fun humanReadableByteCountSI(bytes: Long?): String {
        if (bytes == null) return "0 bytes"
        var bytes1 = bytes
        if (-1000 < bytes1 && bytes1 < 1000) {
            return "$bytes1 B"
        }
        val ci: CharacterIterator = StringCharacterIterator("kMGTPE")
        while (bytes1 <= -999950 || bytes1 >= 999950) {
            bytes1 /= 1000
            ci.next()
        }
        return String.format("%.1f %cB", bytes1 / 1000.0, ci.current())
    }

}
