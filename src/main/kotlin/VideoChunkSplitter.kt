import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VideoChunkSplitter {
    fun splitFile(inputFileUlr: String, nodeCount: Int): List<String> = runBlocking {
        val inputFile = inputFileUlr.substringAfterLast("/")
        val duration = getDuration(inputFile)
        val chunkDuration = duration / nodeCount
        val outputFiles = mutableListOf<String>()
        launch {
            for (i in 0 until nodeCount) {
                val outputFileName = "chunk-00${i + 1}.mkv"
                outputFiles.add(outputFileName)
                val command = "mkvmerge -o $outputFileName --split timecodes:${i * chunkDuration}:${(i + 1) * chunkDuration} $inputFile"
                val process = ProcessBuilder(command).inheritIO().start()
                process.waitFor()
            }
        }
        outputFiles
    }

    private fun getDuration(inputFile: String): Int {
        val command = "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $inputFile"
        val process = ProcessBuilder(command).start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val duration = reader.readLine().toInt()
        process.waitFor()
        return duration
    }

}