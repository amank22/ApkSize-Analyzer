package com.gi.apksize.models

import java.io.File

data class AnalyzerOptions(

    /**
     * Set whether the paths provided in the json/arguments are relative to current directory or absolute paths.
     */
    val arePathsAbsolute: Boolean = false,

    /**
     * Input apk file to test with. (This is must)
     */
    val inputFilePath: String = "",

    /**
     * Input proguard mapping file to test with. (Optional)
     */
    val inputFileProguardPath: String = "",

    /**
     * Output folder path to put all the output files. (This is must)
     */
    val outputFolderPath: String = "",


    /**
     * The app name to print in the html & pdf report
     * @default empty
     */
    val appName: String = "",
    /**
     * Boolean whether to generate a HTML report for this analyzer.
     * @Default true
     */
    val generateHtmlReport: Boolean = true,
    /**
     * Boolean whether to generate a PDF report for this analyzer.
     * @Default true
     */
    val generatePdfReport: Boolean = true,
    /**
     * This is the size filter for top images, top files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    val topFilesImagesSizeLimiter: Long = 10240L,
    /**
     * This is the size filter for filtered files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    val filteredFilesSizeLimiter: Long = 51200L,
    /**
     * This is the max number of count that will be added to json data.
     * These are for top images, top files, filtered files.
     * If the list of items is less than this number, full list is returned else sliced to only this count.
     * This will be a positive integer.
     */
    val filesListMaxCount: Int = 20,
    //region dex constants
    /**
     * We classify few special packages (app related mostly) and show them as a separate item in the output json.
     * This is just to filter out and see data for your own code.
     * This checks if the package name starts with this prefix.
     * If you have any code other than this package, it might miss this filtering.
     */
    val appPackagePrefix: List<String> = listOf(""),
    /**
     * Every dex package & file we process has a depth to it.
     * Like com -> 1, com.goibibo -> 2, com.goibibo.hotels -> 3
     * It starts with 1.
     * We filter the packages that we output in json by this constants.
     * This is the minimum depth that will be filtered out to reduce the noise.
     * We don't want packages like 'com' to come in our result.
     * Change it to filter out even more.
     * A positive integer.
     * Depth starts with 1.
     */
    val dexPackagesMinDepth: Int = 2,
    /**
     * This is the max count for the list of app packages (special appPackagePrefix filtered list)
     * Positive integer.
     */
    val appPackagesMaxCount: Int = 20,
    /**
     * This is the max count of dex packages in the output json.
     * Positive integer.
     */
    val dexPackagesMaxCount: Int = 30,
    /**
     * This is the minimum size in bytes that we use to filter.
     * Any packages/filter below this size is filtered and not included in the output json.
     * Size in bytes.
     */
    val dexPackagesSizeLimiter: Long = 51200L,
    //endregion

    /**
     * Group ID prefixes that identify your app's own modules in the dependencies list.
     * Dependencies whose groupId starts with any of these prefixes are classified as "App Modules".
     * Example: ["com.makemytrip", "com.mmt", "com.goibibo"]
     */
    val appModulePrefixes: List<String> = listOf(),

    //region AAB Analysis
    /**
     * For AAB inputs, generate an install-time APK (base + install-time feature modules)
     * and run file/dex analysis on that APK instead of directly on bundle entries.
     *
     * If generation fails (for example missing aapt2), analyzer falls back to bundle-based analysis.
     */
    val useInstallTimeApkForAabAnalysis: Boolean = true,

    /**
     * Optional path to a bundletool device-spec JSON.
     * When provided, conditional install-time modules are included only if they match this device.
     */
    val aabDeviceSpecPath: String = "",
    //endregion

    /**
     * Path to a bundletool JAR file for AAB analysis when bundletool is not
     * embedded in the analyzer JAR (i.e. when using the lite variant).
     * The tool will shell out to:
     *   java -jar <bundletoolJarPath> build-apks ...
     *   java -jar <bundletoolJarPath> get-size total ...
     * If empty and bundletool is not on the classpath, AAB analysis is skipped.
     */
    val bundletoolJarPath: String = "",

    //region Aapt Configs

    /**
     * aapt2 executable file path according to the system.
     * If this path is given then only resources stats are generated.
     * This is absolute path of aapt2 file.
     */
    val aapt2Executor: String = "",
    //endregion

    //region Diff
    /**
     * Set this value to true if you want to compare 2 apks.
     * Program will ignore second apk file if this is false
     * Default value is false.
     */
    val isDiffMode: Boolean = false,

    /**
     * Apk path according to abs/relative argument of the apk which needs to be compared to 1st one.
     * Default path is empty.
     * isDiffMode must be true for this comparison mode to enable.
     */
    val compareFilePath: String = "",

    /**
     * Apk proguard mapping file for the second/comparing file.
     * Default path is empty.
     * isDiffMode must be true for this comparison mode to enable.
     */
    val compareFileProguardPath: String = "",

    /**
     * This is the size limiter for differences of dex packages.
     * Any package size increase/decrease below this value will be discarded from report.
     * Default : 10000 (~10kb).
     * Value must be in bytes.
     */
    val diffSizeLimiter: Long = 10000L,

    /**
     * This is a check to enable/disable file-to-file comparison of both the apks.
     * This comparison usually takes really long with big apks (Around 4-8 mins for 50 MB apks)
     * Dex package comparison will still run.
     */
    val disableFileByFileComparison: Boolean = false,

    //endregion

    /**
     * Execution timeout in minutes.
     * Keep it according to use. For local, you can keep in large as desired. For CI, you can keep it a desired
     * level where it can fail if there is an issue.
     * Default is 10 minutes.
     */
    val executionTimeOut: Long = 10,

    //region LOB Analysis
    /**
     * Path to directory or zip file containing module mapping files
     * (module-metadata.json, resource-mapping.json, package-mapping.json)
     * produced by the module-size-analysis Gradle plugin.
     * When set, LOB (functional unit) size analysis is performed.
     * Supports both a directory path and a .zip file path.
     */
    val moduleMappingsPath: String = "",
    //endregion
) {

    /**
     * Returns path according to `arePathsAbsolute` value.
     * For relative paths, appends the base dir path.
     */
    /**
     * Detects the input file type based on the file extension.
     * Returns [InputFileType.AAB] for .aab files, [InputFileType.APK] otherwise.
     */
    fun inputFileType(): InputFileType {
        return if (inputFilePath.endsWith(".aab", ignoreCase = true)) {
            InputFileType.AAB
        } else {
            InputFileType.APK
        }
    }

    fun getPath(path: String): String {
        val file = File(path)
        return if (arePathsAbsolute || file.isAbsolute) {
            file.canonicalPath
        } else {
            val baseDir = File(System.getProperty("user.dir"))
            baseDir.resolve(path).canonicalPath
        }
    }

}