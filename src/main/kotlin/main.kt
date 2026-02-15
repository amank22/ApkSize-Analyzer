import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.tasks.ApkSizeTask
import com.gi.apksize.utils.Printer
import com.google.gson.Gson
import java.io.File

fun main(args: Array<String>) {
    Printer.log("Analysing input file!")
    var analyzerOptions = AnalyzerOptions()
    if (args.isEmpty()) {
        Printer.error("Pass arguments to run the program")
        return
    }
    val absOrRelative = args[0]
    if (absOrRelative != "abs" && absOrRelative != "relative") {
        Printer.log("First argument should be abs or relative. Relates to the path to look for.")
        return
    }
    val isAbsolutePaths = absOrRelative == "abs"

    val secondArgument = args.getOrNull(1)
    if (secondArgument == null) {
        Printer.log("Pass the --config file path or separate arguments")
        return
    }
    if (secondArgument.startsWith("--config=")) {
        val configInput = secondArgument.removePrefix("--config=").trim()
        if (configInput.isBlank()) {
            Printer.log("Config input should not be empty")
            return
        }
        val configString = if (configInput.startsWith("{")) {
            // Supports passing config directly as JSON: --config={"inputFilePath":"..."}
            configInput
        } else {
            val configFile = File(configInput)
            if (configFile.exists() && configFile.canRead()) {
                configFile.readText()
            } else {
                Printer.log("Config file does not exists or can't be read")
                return
            }
        }
        try {
            analyzerOptions = Gson().fromJson(configString, AnalyzerOptions::class.java)
            if (analyzerOptions == null) {
                Printer.log("Config should not be empty")
                return
            }
        } catch (e: Exception) {
            Printer.log("Error reading config : ${e.localizedMessage}")
            return
        }
    } else {

        val input = args[1]
        if (input.isBlank()) {
            Printer.log("Input should not be empty")
            return
        }
        analyzerOptions = analyzerOptions.copy(inputFilePath = input)
        val output = args[2]
        if (output.isBlank()) {
            Printer.log("Output should not be empty")
            return
        }
        analyzerOptions = analyzerOptions.copy(outputFolderPath = output)
        var proguard = args.elementAtOrNull(3)
        if (proguard?.startsWith("--") == true) {
            proguard = null
        }
        analyzerOptions = analyzerOptions.copy(inputFileProguardPath = proguard ?: "")
        analyzerOptions = updateOptions(args, analyzerOptions)
    }
    analyzerOptions = analyzerOptions.copy(arePathsAbsolute = isAbsolutePaths)
    ApkSizeTask.evaluate(analyzerOptions)
    val fileTypeLabel = if (analyzerOptions.inputFileType() == com.gi.apksize.models.InputFileType.AAB) "AAB" else "APK"
    Printer.log("Analysed $fileTypeLabel. You can find the output files at ${analyzerOptions.outputFolderPath}.")
}

fun updateOptions(args: Array<String>, analyzerOptions: AnalyzerOptions): AnalyzerOptions {
    var options = analyzerOptions
    args.forEach {
        if (!it.startsWith("--") && !it.contains("=")) return@forEach
        val optionsSplit = it.split("=")
        val optionKey = optionsSplit[0]
        val optionValue = optionsSplit[1]
        when (optionKey) {
            "--appName" -> {
                options = options.copy(appName = optionValue)
            }
            "--generatePdfReport" -> {
                options = options.copy(generatePdfReport = optionValue != "false")
            }
            "--generateHtmlReport" -> {
                options = options.copy(generateHtmlReport = optionValue != "false")
            }
            "--topFilesImagesSizeLimiter" -> {
                options = options.copy(topFilesImagesSizeLimiter = optionValue.toLong())
            }
            "--filteredFilesSizeLimiter" -> {
                options = options.copy(filteredFilesSizeLimiter = optionValue.toLong())
            }
            "--dexPackagesSizeLimiter" -> {
                options = options.copy(dexPackagesSizeLimiter = optionValue.toLong())
            }
            "--aapt2Executor" -> {
                options = options.copy(aapt2Executor = optionValue)
            }
            "--filesListMaxCount" -> {
                options = options.copy(filesListMaxCount = optionValue.toInt())
            }
            "--dexPackagesMinDepth" -> {
                options = options.copy(dexPackagesMinDepth = optionValue.toInt())
            }
            "--appPackagesMaxCount" -> {
                options = options.copy(appPackagesMaxCount = optionValue.toInt())
            }
            "--dexPackagesMaxCount" -> {
                options = options.copy(dexPackagesMaxCount = optionValue.toInt())
            }
            "--appPackagePrefix" -> {
                options = options.copy(appPackagePrefix = listOf(optionValue))
            }
            "--moduleMappingsPath" -> {
                options = options.copy(moduleMappingsPath = optionValue)
            }
            "--useInstallTimeApkForAabAnalysis" -> {
                options = options.copy(useInstallTimeApkForAabAnalysis = optionValue != "false")
            }
            "--aabDeviceSpecPath" -> {
                options = options.copy(aabDeviceSpecPath = optionValue)
            }
        }
    }
    return options
}
