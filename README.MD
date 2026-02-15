![Logo](/screenshots/ApkSize.svg)

ApkSize Analyzer is a command line tool to analyze the size of APK and AAB (Android App Bundle) files and give reports in multiple formats
which you can then use in CI/CD environments (Jenkins etc) to get deeper insights for your app.

It can also be used to compare two APKs and generate a report of the differences. You can do it in Android Studio also right?
This will give you differences on source packages level so that you know what code exactly increased and not just differences in dex files.

**Available as:** JVM JAR (requires JDK 21+) **or** native binary (no JDK needed, instant startup).

#### Features:
* Analyzes **APK** and **AAB** files for files, dex, resources.
* Package level differences between two APKs.
* The report generates JSON, HTML, pdf for easy integration and reading.
* You can provide a specific app package prefix which will be reported separately for easy classification.
* The package sizes are calculated as combination of all the dex files.
* Included a separate list for the images listed according to decreasing size.
* Most of the lists are sorted in the decreasing order of their sizes.

**AAB-specific features:**
* Per-module breakdown (base, feature modules) with component sizes (DEX, resources, assets, native libs)
* Resource counting directly from resources.pb (no aapt2 needed)
* Manifest info extraction (package name, version, min/target SDK)
* Bundle config analysis (split dimensions, compression settings)
* Maven dependency list from bundle metadata

#### Download

Grab the latest release from [GitHub Releases](../../releases). Each release includes:

| Artifact | Description |
|----------|-------------|
| `apkSize-*-full.jar` | Fat JAR with bundletool embedded (~48 MB). Analyzes both APK and AAB standalone. |
| `apkSize-*-lite-r8.jar` | R8-minified JAR (~5 MB). For AAB, supply an external bundletool JAR. |
| `apksize-linux-x64` | Native binary for Linux x64 (no JDK needed). |
| `apksize-macos-arm64` | Native binary for macOS Apple Silicon (no JDK needed). |
| `apksize-macos-x64` | Native binary for macOS Intel (no JDK needed). |

#### Usage

This needs a JSON config file as input which contains all the arguments as defined below.
You can find sample configs in the `samples/configs/` directory.

**Full JAR (APK or AAB):**

`java -jar apkSize-0.4.0-alpha-full.jar abs --config=input/config.json`

**Lite JAR (AAB with external bundletool):**

`java -jar apkSize-0.4.0-alpha-lite-r8.jar abs --config=input/config.json --bundletoolJarPath=/path/to/bundletool-all.jar`

**Native binary (no JDK needed):**

`./apksize abs --config=input/config.json --bundletoolJarPath=/path/to/bundletool-all.jar`

> **Note:** JARs require JDK 21+ to run. Native binaries have no runtime dependencies.

<br>

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
     * Input APK or AAB file to analyze.
     * File type is auto-detected from the extension (.apk or .aab).
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
    
    
**AAB Example:**

`java -jar apkSize-0.4.0-alpha-full.jar abs --config=samples/configs/config-aab.json`

Just point `inputFilePath` to an `.aab` file instead of an `.apk` file. The tool auto-detects the file type.
For AAB files, resource analysis is done via the bundletool library (no `aapt2Executor` needed).

> **Tip:** If you use the lite JAR or native binary, set `bundletoolJarPath` either as a CLI argument
> (`--bundletoolJarPath=/path/to/bundletool-all.jar`) or directly in the config JSON:
> ```json
> { "bundletoolJarPath": "/path/to/bundletool-all.jar" }
> ```

Note: Comparison (diff) mode is not yet supported for AAB files.

#### LOB (Line-of-Business) Size Analysis

LOB analysis attributes every byte in your APK/AAB to a **functional unit** (LOB) — such as "Hotels", "Flights",
"Payments", etc. This lets you answer: *"How much of the app size does each team/feature own?"*

It requires three mapping files produced by a Gradle plugin that runs during your app build:

| File | Purpose |
|------|---------|
| `module-metadata.json` | Lists all Gradle modules and groups them into functional units (LOBs). |
| `resource-mapping.json` | Maps every file path (res, assets, native libs) to module indices. |
| `package-mapping.json` | Maps every DEX package to `[moduleIndex, classCount]` pairs. |

**How to enable:**

Set `moduleMappingsPath` in your config JSON to the directory (or `.zip`) containing the three files:

```json
{
  "inputFilePath": "/path/to/app-release.aab",
  "outputFolderPath": "/path/to/output",
  "moduleMappingsPath": "/path/to/module-mappings/",
  "appPackagePrefix": ["com.yourapp"],
  "appModulePrefixes": ["com.yourapp"]
}
```

`appPackagePrefix` and `appModulePrefixes` improve accuracy by letting the analyzer distinguish your
app's own code/files from third-party libraries during fallback attribution.

**How it works:**

1. **File attribution** — Each file in the APK/AAB is matched against `resource-mapping.json` to find
   which module (and therefore which LOB) owns it. Files are categorized as `resources`, `assets`,
   `nativeLibs`, or `other`.

2. **DEX attribution** — Each DEX package is matched against `package-mapping.json`. When a package
   maps to multiple modules, bytes are split proportionally by class count. The analyzer walks up
   the package hierarchy (e.g. `a.b.c.d` → `a.b.c` → `a.b`) to find ancestor mappings for
   unmapped leaf packages.

3. **Fallback attribution** — Known platform packages (`android`, `androidx`, `dagger`, etc.) are
   auto-attributed to `android_platform`. Non-app packages are attributed to `thirdparty`. This
   reduces unmatched noise without mapping files for third-party code.

4. **DEX normalization** — Package-level sizes from dexlib2 may not perfectly match raw `.dex` file
   bytes. The analyzer applies proportional normalization so LOB totals reconcile with actual file sizes.

**Output:**

When `moduleMappingsPath` is set, the output directory will contain:

| File | Content |
|------|---------|
| `apkstats.json` | Main report — includes `lobAnalysis` with per-LOB size breakdown (code, resources, assets, nativeLibs, other, total) and a coverage summary. |
| `lob-unmatched-details.json` | Files and DEX packages that could not be attributed to any LOB — useful for improving mapping coverage. |
| `lob-attributed-details.json` | Per-LOB detailed list of every file and DEX package attributed — useful for auditing. |
| `lob-dex-overhead-details.json` | DEX structural overhead breakdown (headers, string pools, etc.) not attributable to individual packages. |

**Example `lobAnalysis` output:**

```json
{
  "lobSizes": {
    "hotels": { "code": 5242880, "resources": 1048576, "assets": 0, "nativeLibs": 0, "other": 524288, "total": 6815744 },
    "flights": { "code": 3145728, "resources": 524288, "assets": 262144, "nativeLibs": 0, "other": 131072, "total": 4063232 }
  },
  "summary": {
    "totalAttributedBytes": 10878976,
    "totalUnattributedBytes": 524288,
    "coveragePercent": 95.4
  }
}
```

#### Building from Source

```bash
./gradlew clean assemble          # Builds full + lite-r8 JARs
./gradlew nativeImageLite          # Builds native binary (requires GRAALVM_HOME)
```

* **JDK 21+** (auto-provisioned by the Gradle wrapper via foojay toolchain resolver)
* **Gradle 8.14.4** (bundled via wrapper)
* **Kotlin 2.3.10**
* **GraalVM CE 21+** (only for native binary builds; set `GRAALVM_HOME`)

See [README-DEVELOPMENT.md](README-DEVELOPMENT.md) for full development setup, GraalVM config regeneration, and CI/CD details.

#### Future tasks:
* Add Test Cases
* Make HTML report for appPackages more readable
* Make it as a Gradle plugin
* AAB comparison (diff) mode support
* PDF report generation

Note: These items are not in the order

