# Changelog

All notable changes to the ApkSize Analyzer project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.4.0-alpha] - 2026-02-15

**AAB (Android App Bundle) is now fully supported for single-file analysis.**

### Known Issues
- Comparison (diff) mode is not yet supported for AAB files.

### Build System / Toolchain
- **Kotlin** upgraded from 1.9.0 to **2.3.10** (latest stable).
- **Gradle** upgraded from 7.3.3 to **8.14.4** (latest 8.x; includes GraalVM Native Image toolchain selection).
- **Shadow plugin** upgraded from 7.0.0 to **8.1.0**.
- **JVM toolchain** updated from 1.8 to **21** (LTS) via `kotlin { jvmToolchain(21) }`.
- Added **foojay toolchain resolver** plugin for automatic JDK/GraalVM provisioning.
- Migrated `kotlinx-metadata-jvm` dependency to `kotlin-metadata-jvm` (official in Kotlin 2.x).
- Removed explicit `kotlin-stdlib-jdk8` dependency (auto-included in Kotlin 2.x).
- Fixed deprecated Gradle APIs: `mainClassName` → `mainClass.set()`, `kotlinOptions` → `jvmToolchain`.
- Removed obsolete `-Xopt-in=kotlin.RequiresOptIn` compiler flag (stable in Kotlin 2.x).

### Added
- **Two JAR variants**: `full` (with bundletool, ~48 MB) and `lite` (R8-minified, ~8-10 MB without bundletool). Use the full JAR for AAB analysis, or the lite JAR with `bundletoolJarPath` config pointing to an external bundletool JAR.
- **LOB (Line-of-Business) analysis** — new `moduleMappingsPath` config option to supply module mapping files produced by the module-size-analysis Gradle plugin; enables per-LOB size attribution.
- New models: `LobAnalysisResult`, `LobContext`, and `ModuleMappingData` to support LOB size analysis.
- **Install-time APK generation for AAB analysis** — new `useInstallTimeApkForAabAnalysis` option (default `true`) generates an install-time APK from the bundle and runs file/dex analysis on it.
- New `aabDeviceSpecPath` config option to provide a bundletool device-spec JSON for conditional module filtering.
- New `bundletoolJarPath` config option for lite JAR users to point to an external bundletool JAR for AAB support via CLI.
- Enhanced `BundleStatsTask` with expanded AAB processing pipeline.
- Extended `HtmlGenerator` with richer reporting sections.
- Improved `DexFileProcessor` and `BundleDexProcessor` with additional analysis capabilities.
- Updated `ApkSizeTask` with broader analysis orchestration and lite JAR CLI fallback.

### Changed
- Build system now produces two shadow JAR variants (`full` and `lite`); R8 minification applies only to the lite JAR.
- Refactored `BundleFileProcessor` and `ApkGeneralFileProcessor` for improved handling.
- Updated `BundleSizeProcessor` with refinements.
- Updated `SingleStatsTask` to accommodate new analysis options.
- Revised CLI entry point (`main.kt`) for new feature flags and options.
- Updated sample AAB config (`config-aab.json`).
- Cleaned up shrink-rules.pro for lite-only R8 (removed bundletool keep rules).

### Removed
- Cleaned up obsolete sample output files (`apksize.json`, `index.html`).

## [0.3.1-beta] - 2024-10-10

### Fixed
- Compilation fixes and build stability improvements.
- Number locale correction in HTML generator.

### Changed
- Updated HTML report format.
- Removed experimental opt-in annotation.
- Added Gradle wrapper and ensured project compiles out-of-the-box.

## [0.3-beta] - 2022-07-27

### Fixed
- Locale fix (Hindi to US English) for consistent formatting.

### Added
- Detekt static analysis GitHub Actions workflow.

## [0.2-beta] - 2021-01-04

### Added
- APK comparison (diff) mode — compare two APKs side by side.
- HTML and PDF report generation for comparison reports.
- Sample output files.
- Project logo and branding.
- GitHub Pages support (Jekyll theme, `index.html`).
- Stale bot and label workflow for GitHub issue management.
- `FUNDING.yml` for sponsorship.

### Changed
- Improved README with updated documentation.
- Updated version and clarified path error messages.

## [0.1-beta] - 2020-10-09

### Added
- Initial release of ApkSize Analyzer.
- APK file analysis: files, dex packages, resources.
- JSON report output.
- Configurable analysis via JSON config file.
- App package prefix filtering for focused analysis.
- Top files and images listing sorted by size.
