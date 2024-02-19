import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths

class FFmpegEncoder(private val ffmpegPath: String) {
    private val spacesEndpoint = System.getenv("SPACES_ENDPOINT") ?: throw IllegalArgumentException("SPACES_ENDPOINT is not set")
    private val spacesBucket = System.getenv("SPACES_BUCKET") ?: throw IllegalArgumentException("SPACES_BUCKET is not set")
    private val spacesAccessKey = System.getenv("SPACES_ACCESS_KEY")
    private val spacesSecretKey = System.getenv("SPACES_SECRET_KEY")

    fun encode(inputFileUrl: String, outputFile: String, params: List<String>) = runBlocking {
    try {
        val inputFile = inputFileUrl.substringAfterLast("/")
        val command = buildCommand(inputFile, outputFile, params)
        println("FFmpeg command: $command") // Debug print statement

        var process: Process? = null
        val job = launch {
            println("process coroutine launched")
            process = ProcessBuilder(command).inheritIO().start()
            process?.waitFor()
//          val reader = process.inputStream.bufferedReader()
//          while (true) {
//              val line = reader.readLine() ?: break
//              println(line)
//          }
        }

        job.join() // Wait for the coroutine to finish

        val exitCode = process?.exitValue()
        if (exitCode == 0) {
            println("Encoding completed")
            if (Files.exists(Paths.get(outputFile))) {
                runCatching {
                    uploadFile(outputFile, "encodes/$outputFile")
                }.onFailure { e ->
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
    fun uploadFile(filePath: String, destinationPath: String): PutObjectResponse {
        println("Uploading file to DigitalOcean Spaces...$filePath to $destinationPath")
        val credentials = AwsBasicCredentials.create(spacesAccessKey, spacesSecretKey)

        val s3 = S3Client.builder()
            .region(Region.US_WEST_2)
            .endpointOverride(URI.create(spacesEndpoint))
            .credentialsProvider { credentials }
            .build()

        val putObjectRequest = PutObjectRequest.builder()
            .bucket(spacesBucket)
            .key(destinationPath)
            .build()

        println("uploading completed")
        return s3.putObject(putObjectRequest, Paths.get(filePath))
    }

    private fun buildCommand(inputFile: String, outputFile: String, params: List<String>): List<String> {
        return listOf(ffmpegPath, "-y", "-i", inputFile) + params + outputFile
    }

}