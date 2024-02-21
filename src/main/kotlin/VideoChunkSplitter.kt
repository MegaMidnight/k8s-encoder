import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class VideoChunkSplitter {
    fun splitFile(inputFile: String, nodeCount: Int) = runBlocking {
        val duration = getDuration(inputFile)
        val chunkDuration = duration / nodeCount
        launch { for (i in 0 until nodeCount) {
            val outputFileName = "chunk${i + 1}.mkv"
            val command = "mkvmerge -o $outputFileName --split timecodes:${i * chunkDuration}:${(i + 1) * chunkDuration} $inputFile"
            val process = ProcessBuilder(command).inheritIO().start()
            process.waitFor()
        } }
    }

    private fun getDuration(inputFile: String): Int {
        val command = "ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 $inputFile"
        val process = ProcessBuilder(command).inheritIO().start()
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        val duration = reader.readLine().toInt()
        process.waitFor()
        return duration
    }

}