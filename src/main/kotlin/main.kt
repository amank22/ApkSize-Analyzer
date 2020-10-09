import com.gi.apksize.models.AnalyzerOptions
import com.gi.apksize.tasks.ApkSizeTask

fun main(args: Array<String>) {
    println("Analysing apk!")
    println(args.joinToString())
    val absOrRelative = args[0]
    if (absOrRelative != "abs" && absOrRelative != "relative") {
        println("First argument should be abs or relative. Relates to the path to look for.")
        return
    }
    val isAbsolutePaths = absOrRelative == "abs"
    val input = args[1]
    if (input.isBlank()) {
        println("Input should not be empty")
        return
    }
    val output = args[2]
    if (output.isBlank()) {
        println("Output should not be empty")
        return
    }
    var proguard = args.elementAtOrNull(3)
    if (proguard?.startsWith("--") == true) {
        proguard = null
    }
    val analyzerOptions = AnalyzerOptions()
    updateOptions(args, analyzerOptions)
    ApkSizeTask.evaluateSize(isAbsolutePaths, input, output, proguard, analyzerOptions)
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
