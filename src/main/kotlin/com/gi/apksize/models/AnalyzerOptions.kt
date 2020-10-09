package com.gi.apksize.models

class AnalyzerOptions {

    /**
     * The app name to print in the html & pdf report
     * @default empty
     */
    var appName = ""
    /**
     * Boolean whether to generate a HTML report for this analyzer.
     * @Default true
     */
    var generateHtmlReport = true
    /**
     * Boolean whether to generate a PDF report for this analyzer.
     * @Default true
     */
    var generatePdfReport = true
    /**
     * This is the size filter for top images, top files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    var topFilesImagesSizeLimiter = 10240L
    /**
     * This is the size filter for filtered files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    var filteredFilesSizeLimiter = 51200L
    /**
     * This is the max number of count that will be added to json data.
     * These are for top images, top files, filtered files.
     * If the list of items is less than this number, full list is returned else sliced to only this count.
     * This will be a positive integer.
     */
    var filesListMaxCount = 20
    //region dex constants
    /**
     * We classify few special packages (app related mostly) and show them as a separate item in the output json.
     * This is just to filter out and see data for your our code.
     * This checks if the package name starts with this prefix.
     * If you have any code other than this package, it might miss this filtering.
     */
    var appPackagePrefix = ""
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
    var dexPackagesMinDepth = 2
    /**
     * This is the max count for the list of app packages (special appPackagePrefix filtered list)
     * Positive integer.
     */
    var appPackagesMaxCount = 20
    /**
     * This is the max count of dex packages in the output json.
     * Positive integer.
     */
    var dexPackagesMaxCount = 30
    /**
     * This is the minimum size in bytes that we use to filter.
     * Any packages/filter below this size is filtered and not included in the output json.
     * Size in bytes.
     */
    var dexPackagesSizeLimiter = 51200L
    //endregion

    //region Aapt Configs

    /**
     * aapt2 executable file path according to the system.
     * If this path is given then only resources stats are generated.
     * This is absolute path of aapt2 file.
     */
    var aapt2Executor = ""
    //endregion

}