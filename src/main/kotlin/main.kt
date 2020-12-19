import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.tasks.ApkSizeTask
import com.google.gson.Gson
import java.io.File

fun main(args: Array<String>) {
    println("Analysing apk!")
    var analyzerOptions = AnalyzerOptions()

    val absOrRelative = args[0]
    if (absOrRelative != "abs" && absOrRelative != "relative") {
        println("First argument should be abs or relative. Relates to the path to look for.")
        return
    }
    val isAbsolutePaths = absOrRelative == "abs"

    val secondArgument = args.getOrNull(1)
    if (secondArgument == null) {
        println("Pass the --config file path or separate arguments")
        return
    }
    if (secondArgument.startsWith("--config=")) {
        val jsonConfigPath = secondArgument.removePrefix("--config=")
        val configFile = File(jsonConfigPath)
        if (configFile.exists() && configFile.canRead()) {
            val configString = configFile.readText()
            try {
                analyzerOptions = Gson().fromJson(configString, AnalyzerOptions::class.java)
                if (analyzerOptions == null) {
                    println("Config file should not be empty")
                    return
                }
            } catch (e: Exception) {
                println("Error reading config file : ${e.localizedMessage}")
                return
            }
        } else {
            println("Config file does not exists or can't be read")
            return
        }
    } else {

        val input = args[1]
        if (input.isBlank()) {
            println("Input should not be empty")
            return
        }
        analyzerOptions.inputFilePath = input
        val output = args[2]
        if (output.isBlank()) {
            println("Output should not be empty")
            return
        }
        analyzerOptions.outputFolderPath = output
        var proguard = args.elementAtOrNull(3)
        if (proguard?.startsWith("--") == true) {
            proguard = null
        }
        analyzerOptions.inputFileProguardPath = proguard ?: ""
        updateOptions(args, analyzerOptions)
    }
    analyzerOptions.arePathsAbsolute = isAbsolutePaths
    ApkSizeTask.evaluate(analyzerOptions)
    println("Analysed apk. You can find the files in the output directory you gave.")
}

fun updateOptions(args: Array<String>, analyzerOptions: AnalyzerOptions) {
    args.forEach {
        if (!it.startsWith("--") && !it.contains("=")) return@forEach
        val optionsSplit = it.split("=")
        val optionKey = optionsSplit[0]
        val optionValue = optionsSplit[1]
        when (optionKey) {
            "--appName" -> {
                analyzerOptions.appName = optionValue
            }
            "--generatePdfReport" -> {
                analyzerOptions.generatePdfReport = optionValue != "false"
            }
            "--generateHtmlReport" -> {
                analyzerOptions.generateHtmlReport = optionValue != "false"
            }
            "--topFilesImagesSizeLimiter" -> {
                analyzerOptions.topFilesImagesSizeLimiter = optionValue.toLong()
            }
            "--filteredFilesSizeLimiter" -> {
                analyzerOptions.filteredFilesSizeLimiter = optionValue.toLong()
            }
            "--dexPackagesSizeLimiter" -> {
                analyzerOptions.dexPackagesSizeLimiter = optionValue.toLong()
            }
            "--aapt2Executor" -> {
                analyzerOptions.aapt2Executor = optionValue
            }
            "--filesListMaxCount" -> {
                analyzerOptions.filesListMaxCount = optionValue.toInt()
            }
            "--dexPackagesMinDepth" -> {
                analyzerOptions.dexPackagesMinDepth = optionValue.toInt()
            }
            "--appPackagesMaxCount" -> {
                analyzerOptions.appPackagesMaxCount = optionValue.toInt()
            }
            "--dexPackagesMaxCount" -> {
                analyzerOptions.dexPackagesMaxCount = optionValue.toInt()
            }
            "--appPackagePrefix" -> {
                analyzerOptions.appPackagePrefix = optionValue
            }
        }
    }
}
