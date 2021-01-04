![Logo](/screenshots/ApkSize.svg)

ApkSize Analyzer is a command line java program to analyze the size of Apk and give report in multiple formats
which you can then use in CI/CD environments (jenkins etc) to get deeper insights for your apk.

It can also be used to compare two APKs and generate report of the differences. You can do it in Android Studio also right?
This will give you differences on source packages level so that you know what code exactly increased and not just differences in dex files.

#### Features:
* Analyzes Apk for files, dex, resources.
* Package level differences between two APKs.
* The report generates JSON, HTML, pdf for easy integration and reading.
* You can provide a specific app package prefix which will be reported separately for easy classification.
* The package sizes are calculated as combination of all the dex files.
* Included a separate list for the images listed according to decreasing size.
* Most of the lists are sorted in the decreasing order of their sizes.

Example command to run analyzer:

This needs a json file as input which contains all the arguments as defined below.
This is done because of large number of variables you can change in customise result according to your needs.
You can find sample config.json in samples directory of this project.

`java -jar apkSize-0.2-beta-all.jar relative --config=input/config.json`
<br><br>

**How to compare 2 apks?**
For comparing 2 apks, along with apk paths, there are arguments for comparing.
`isDiffMode` must be true. You can find the sample config for comparing in samples directory.

![Screenshot](/screenshots/apkSizeScreenshot.png)
<br>
![Screenshot](/screenshots/sample-compare-report.png)

<br><br>
**Arguments:**
0. (REQUIRED) relative/abs -> Tells how to look for the path you have given whether relative to the current folder or absolute paths.
1. (REQUIRED) --config -> json file path containing all the required arguments.

##### **Config Json Parameters:**


    /**
     * Must
     * Input apk file to test with.
     */
    inputFilePath = ""

    /**
     * Input proguard mapping file to test with. (Optional)
     */
    inputFileProguardPath = ""

    /**
     * Output folder path to put all the output files. (This is must)
     */
    outputFolderPath = ""
    /**
    * The app name to print in the html & pdf report
    * @default empty
    */
    appName = ""
    /**
    * Boolean whether to generate a HTML report for this analyzer.
    * @Default true
    */
    generateHtmlReport = true
    /**
     * This is the size filter for top images, top files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    topFilesImagesSizeLimiter = 10240L
    /**
     * This is the size filter for filtered files.
     * This is the minimum size above which these data will be recorded and others below this will be discarded.
     * This is in bytes.
     */
    filteredFilesSizeLimiter = 51200L
    /**
     * This is the max number of count that will be added to json data.
     * These are for top images, top files, filtered files.
     * If the list of items is less than 30, full list is returned else sliced to only this count.
     * This will be a positive integer.
     */
    filesListMaxCount = 20
    
    //dex constants
    /**
     * We classify few special packages (app related mostly) and show them as a separate item in the output json.
     * This is just to filter out and see data for your our code.
     * This checks if the package name starts with this prefix.
     * If you have any code other than this package, it might miss this filtering.
     */
    appPackagePrefix = ""
    /**
     * Every dex package & file we process has a depth to it.
     * Like com -> 1, com.apkDemo -> 2, com.apkDemo.home -> 3
     * It starts with 1.
     * We filter the packages that we output in json by this constants.
     * This is the minimum depth that will be filtered out to reduce the noise.
     * We don't want packages like 'com' to come in our result.
     * Change it to filter out even more.
     * A positive integer.
     * Depth starts with 1.
     */
    dexPackagesMinDepth = 2
    /**
     * This is the max count for the list of app packages (special appPackagePrefix filtered list)
     * Positive integer.
     */
    appPackagesMaxCount = 20
    /**
     * This is the max count of dex packages in the output json.
     * Positive integer.
     */
    dexPackagesMaxCount = 30
    /**
     * This is the minimum size in bytes that we use to filter.
     * Any packages/filter below this size is filtered and not included in the output json.
     * Size in bytes.
     */
    dexPackagesSizeLimiter = 51200L
    /**
    * aapt2 executable file path according to the system.
    * If this path is given then only resources stats are generated.
    * This is absolute path of aapt2 file.
    */
    aapt2Executor = ""
    /**
     * Set this value to true if you want to compare 2 apks.
     * Program will ignore second apk file if this is false
     * Default value is false.
     */
    isDiffMode = false

    /**
     * Apk path according to abs/relative argument of the apk which needs to be compared to 1st one.
     * Default path is empty.
     * isDiffMode must be true for this comparison mode to enable.
     */
    compareFilePath = ""

    /**
     * Apk proguard mapping file for the second/comparing file.
     * Default path is empty.
     * isDiffMode must be true for this comparison mode to enable.
     */
    compareFileProguardPath = ""

    /**
     * This is the size limiter for differences of dex packages.
     * Any package size increase/decrease below this value will be discarded from report.
     * Default : 10000 (~10kb).
     * Value must be in bytes.
     */
    diffSizeLimiter = 10000L

    /**
     * This is a check to enable/disable file-to-file comparison of both the apks.
     * This comparison usually takes really long with big apks (Around 4-8 mins for 50 MB apks)
     * Dex package comparison will still run.
     */
    disableFileByFileComparison = false
    
    
#### Future tasks:
* Add Test Cases
* Integrate with CI/CD
* Make proguard or other minification tool work and reduce the size
* Make HTML report for appPackages more readable
* Make it as a gradle plugin
* Work to support the bundle files too. Currently, bundle files doesn't give dex results. 

Note: These items are not in the order

