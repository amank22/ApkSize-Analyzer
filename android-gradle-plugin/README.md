# Module Size Analysis Gradle Plugin

A Gradle plugin for Android projects that scans every module and AAR/JAR dependency at build time, producing JSON mapping files that attribute every resource, class, asset, and native library to the Gradle module that owns it. These mappings are consumed by the [ApkSize Analyzer](https://github.com/amank22/ApkSize-Analyzer) CLI to calculate per-LOB (Line-of-Business) APK/AAB size breakdowns.

## Requirements

- **Android Gradle Plugin** 7.x or later
- **Gradle** 7.6 or later
- **JDK** 17 or later

## Installation

Apply the plugin in your **root** `build.gradle.kts`:

```kotlin
plugins {
    id("io.github.amank22.module-size-analysis") version "0.2.0"
}
```

Or in Groovy (`build.gradle`):

```groovy
plugins {
    id 'io.github.amank22.module-size-analysis' version '0.2.0'
}
```

## Configuration

Configure the plugin in your root `build.gradle.kts` using the `moduleSizeAnalysis` DSL block:

```kotlin
moduleSizeAnalysis {
    variant = "standardRelease"
    packageDepth = 4
    functionalUnitMapping = mapOf(
        "hotels"   to listOf("com.example:hotels*", ":hotels-*"),
        "flights"  to listOf("com.example:flights*", ":flights-*"),
        "payments" to listOf("com.example:pay*", ":payments"),
    )
}
```

### DSL Reference

| Property | Type | Default | Description |
|----------|------|---------|-------------|
| `variant` | `String` | `"release"` | Build variant to analyze (e.g. `"release"`, `"standardRelease"`). |
| `packageDepth` | `Int` | `3` | How many levels deep to group classes by package. E.g. depth 3 = `com.example.lib`, depth 4 = `com.example.lib.utils`. |
| `includePatterns` | `List<String>` | `["*"]` | Glob patterns for modules to include. Matched against `"group:artifact"` (remote) or `":project-path"` (local). |
| `excludePatterns` | `List<String>` | `[]` | Glob patterns for modules to exclude. Applied after `includePatterns`. |
| `includeLocalModules` | `Boolean` | `true` | Whether to include local project modules (`:xyz` dependencies). When `false`, only remote Maven/AAR/JAR dependencies are analyzed. |
| `enableModuleAnalysis` | `Boolean` | `true` | Enable the module analysis step (resources, classes, native libs per module). |
| `enableResourceMapping` | `Boolean` | `true` | Enable the resource mapping step (file path to module mapping). |
| `functionalUnitMapping` | `Map<String, List<String>>` | `{}` | Maps functional unit (LOB) names to lists of module glob patterns. Modules matching a pattern are assigned to that functional unit. Unmatched modules are auto-assigned to `"android_platform"` (androidx, com.google, etc.) or `"thirdparty"`. |
| `appModule` | `String?` | `null` | Target app module path (e.g. `":app"`, `":mobile"`). Auto-detected if null. |
| `outputFile` | `File?` | `null` | Output path for the module analysis JSON. Defaults to `<app>/build/reports/module-size-analysis.json`. |
| `resourceMappingFile` | `File?` | `null` | Output path for the resource mapping JSON. Defaults to `<app>/build/reports/resource-mapping.json`. |
| `metadataFile` | `File?` | `null` | Output path for the metadata JSON. Defaults to `<app>/build/reports/module-metadata.json`. |
| `packageMappingFile` | `File?` | `null` | Output path for the package mapping JSON. Defaults to `<app>/build/reports/package-mapping.json`. |
| `minPackageDepth` | `Int` | `3` | Minimum package depth to include in package mapping. Filters out shallow/noise packages. |
| `packageOverrides` | `Map<String, String>` | `{}` | Manual overrides for specific package prefixes. Value can be `"ignore"` to exclude, or an FU name to force-assign. |
| `resourceFUOverrides` | `Map<String, List<String>>` | `{}` | File-path-based FU overrides for resource mapping. Re-attributes specific files to an FU directly, bypassing module-level assignment. Key = FU name, value = list of glob patterns. |
| `resourceDirFUOverrides` | `Map<String, List<String>>` | `{}` | Directory-based FU overrides. All resource files in these directories are attributed directly to the FU. Paths are relative to the app module project dir. |

## Running

After configuring, run the analysis task on your app module:

```bash
./gradlew :app:analyzeModuleSizes
```

The task is automatically registered on every `com.android.application` module in the project.

### Command-Line Property Overrides

You can override DSL properties from the command line without editing `build.gradle.kts`:

```bash
./gradlew :app:analyzeModuleSizes \
    -PmoduleSizeAnalysis.variant=standardRelease \
    -PmoduleSizeAnalysis.packageDepth=4 \
    -PmoduleSizeAnalysis.includePatterns="com.example:*,:app-*" \
    -PmoduleSizeAnalysis.excludePatterns="*test*" \
    -PmoduleSizeAnalysis.includeLocalModules=true \
    -PmoduleSizeAnalysis.enableModuleAnalysis=true \
    -PmoduleSizeAnalysis.enableResourceMapping=true \
    -PmoduleSizeAnalysis.outputFile=build/custom-output.json
```

## Output Files

The task produces four JSON files (by default in `<app>/build/reports/`):

### `module-metadata.json`

Lists all discovered Gradle modules and groups them into functional units (LOBs).

```json
{
  "generatedAt": "2026-02-15T10:30:00Z",
  "variant": "standardRelease",
  "packageDepth": 4,
  "projectName": "MyApp",
  "appModule": ":app",
  "dynamicFeatures": ["feature_hotels"],
  "modules": [":app", "com.example:hotels:1.0", "androidx.core:core:1.12.0", ...],
  "functionalUnits": {
    "hotels": [1],
    "android_platform": [2],
    "thirdparty": [3, 4]
  }
}
```

### `module-size-analysis.json`

Per-module breakdown of declared resources (by type), classes (by package), and native libraries.

```json
{
  "modules": {
    "com.example:hotels:1.0": {
      "type": "remote_aar",
      "resources": { "declared": { "total": 142, "byType": { "drawable": 45, "layout": 30, ... } } },
      "packages": { "com.example.hotels": 85, "com.example.hotels.ui": 42 },
      "classCount": 127,
      "nativeLibs": [{ "abi": "arm64-v8a", "name": "libnative.so", "sizeBytes": 524288 }]
    }
  },
  "summary": { "totalModules": 120, "totalClasses": 15000, ... }
}
```

### `resource-mapping.json`

Maps every file path (resources, assets, native libs) to the module index that owns it.

```json
{
  "resourceMapping": {
    "base/res/drawable/hotel_icon.webp": [1],
    "base/res/layout/activity_main.xml": [0],
    "feature_hotels/res/drawable/banner.webp": [5]
  },
  "summary": { "totalMappedFiles": 3200, "collisions": 12, "uniqueModules": 95 }
}
```

### `package-mapping.json`

Maps every DEX package to `[moduleIndex, classCount]` pairs for proportional size attribution.

```json
{
  "packageMapping": {
    "com.example.hotels": [[1, 85]],
    "com.example.hotels.ui": [[1, 42]],
    "com.example.shared.utils": [[1, 10], [2, 5]]
  },
  "summary": { "totalPackages": 450, "collisions": 23 }
}
```

## Integration with ApkSize Analyzer CLI

The three mapping files produced by this plugin (`module-metadata.json`, `resource-mapping.json`, `package-mapping.json`) are consumed by the [ApkSize Analyzer](https://github.com/amank22/ApkSize-Analyzer) CLI to perform LOB (Line-of-Business) size attribution.

### How it works

1. **Pass the mappings to the CLI** by setting `moduleMappingsPath` in your config JSON to the directory (or `.zip`) containing the three files:

    ```json
    {
      "inputFilePath": "/path/to/app-release.aab",
      "outputFolderPath": "/path/to/output",
      "moduleMappingsPath": "/path/to/build/reports/",
      "appPackagePrefix": ["com.example"],
      "appModulePrefixes": ["com.example"]
    }
    ```

2. **The CLI loads the mappings** via `LobContext.load()`, which parses all three JSON files and builds reverse lookup maps from module indices to functional unit names.

3. **File attribution** -- Each file in the APK/AAB is matched against `resource-mapping.json` to find which module (and therefore which LOB) owns it. Files are categorized as resources, assets, native libs, or other.

4. **DEX attribution** -- Each DEX package is matched against `package-mapping.json`. When a package maps to multiple modules, bytes are split proportionally by class count. The analyzer walks up the package hierarchy (e.g. `a.b.c.d` -> `a.b.c` -> `a.b`) to find ancestor mappings for unmapped leaf packages.

5. **Fallback attribution** -- Known platform packages (`android`, `androidx`, `dagger`, etc.) are auto-attributed to `android_platform`. Non-app packages are attributed to `thirdparty` when `appPackagePrefix` is configured. This reduces unmatched noise without requiring mapping files for third-party code.

6. **DEX normalization** -- Package-level sizes from dexlib2 may not perfectly match raw `.dex` file bytes. The analyzer applies proportional normalization so LOB totals reconcile with actual file sizes.

### Output

When `moduleMappingsPath` is set, the CLI produces a `lobAnalysis` section in `apkstats.json`:

```json
{
  "lobSizes": {
    "hotels": { "code": 5242880, "resources": 1048576, "assets": 0, "nativeLibs": 0, "other": 524288, "total": 6815744 },
    "flights": { "code": 3145728, "resources": 524288, "assets": 262144, "nativeLibs": 0, "other": 131072, "total": 4063232 },
    "android_platform": { "code": 2097152, "resources": 0, "assets": 0, "nativeLibs": 0, "other": 0, "total": 2097152 },
    "thirdparty": { "code": 1048576, "resources": 262144, "assets": 0, "nativeLibs": 524288, "other": 0, "total": 1835008 }
  },
  "summary": {
    "totalAttributedBytes": 14811136,
    "totalUnattributedBytes": 524288,
    "coveragePercent": 96.6
  }
}
```

Additional detail files are also produced:

| File | Content |
|------|---------|
| `lob-unmatched-details.json` | Files and DEX packages that could not be attributed to any LOB. |
| `lob-attributed-details.json` | Per-LOB detailed list of every file and DEX package attributed. |
| `lob-dex-overhead-details.json` | DEX structural overhead breakdown (headers, string pools, etc.). |

## Full Example

```kotlin
// root build.gradle.kts
plugins {
    id("io.github.amank22.module-size-analysis") version "0.2.0"
}

moduleSizeAnalysis {
    variant = "standardRelease"
    packageDepth = 4

    functionalUnitMapping = mapOf(
        "hotels"   to listOf("com.example:hotels*", ":hotels-*", ":hotel-booking"),
        "flights"  to listOf("com.example:flights*", ":flights-*"),
        "payments" to listOf("com.example:pay*", ":payments"),
        "platform" to listOf(":core-*", ":common-*", "com.example:platform*"),
    )

    // Exclude test modules from analysis
    excludePatterns = listOf("*test*", "*mock*")

    // Override specific package attribution
    packageOverrides = mapOf(
        "hilt_aggregated_deps" to "ignore",
        "(default)" to "ignore",
    )

    // Override specific resource files to a different FU
    resourceFUOverrides = mapOf(
        "hotels" to listOf("**/hotel_*.webp", "**/hotel_*.xml"),
    )
}
```

Then run:

```bash
./gradlew :app:analyzeModuleSizes
```

And feed the output to the ApkSize Analyzer CLI:

```bash
java -jar apkSize.jar abs --config=config.json
# where config.json has "moduleMappingsPath": "app/build/reports/"
```

## License

This plugin is part of the [ApkSize Analyzer](https://github.com/amank22/ApkSize-Analyzer) project.
