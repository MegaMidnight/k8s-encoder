// Version: 1.0
import java.nio.file.Files
import java.nio.file.Paths

class FFmpegEncoder(private val ffmpegPath: String) {

    fun encode(inputFileUrl: String, outputFile: String, params: List<String>) {
    try {
        val inputFile = inputFileUrl.substringAfterLast("/")
        val videoFileHandler = VideoFileHandler()
        val command = buildCommand(inputFile, outputFile, params)
        val process: Process? = ProcessBuilder(command).inheritIO().start()
        process?.waitFor()

        val exitCode = process?.exitValue()
        if (exitCode == 0) {
            println("Encoding completed")
            if (Files.exists(Paths.get(outputFile))) {
                try {
                    videoFileHandler.uploadFile(outputFile, "encodes/$outputFile")
                }catch (e: Exception){
                    println("An error occurred during upload: ${e.message}")
                }
            } else {
                println("Output file does not exist: $outputFile")
            }
        } else {
            println("An error occurred during the encoding process")
        }

    } catch (e: Exception) {
        println("An error occurred: ${e.message}")
        throw e
    }
}

    private fun buildCommand(inputFile: String, outputFile: String, params: List<String>): List<String> {
        return listOf(ffmpegPath, "-y", "-i", inputFile) + params + outputFile
    }

}