// Version: 1.0
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
    private val spacesAccessKey = System.getenv("SPACES_ACCESS_KEY")?: throw IllegalArgumentException("SPACES_ACCESS_KEY is not set")
    private val spacesSecretKey = System.getenv("SPACES_SECRET_KEY")?: throw IllegalArgumentException("SPACES_SECRET_KEY is not set")

    fun encode(inputFileUrl: String, outputFile: String, params: List<String>) {
    try {
        val inputFile = inputFileUrl.substringAfterLast("/")
        val command = buildCommand(inputFile, outputFile, params)
        val process: Process? = ProcessBuilder(command).inheritIO().start()
        process?.waitFor()

        val exitCode = process?.exitValue()
        if (exitCode == 0) {
            println("Encoding completed")
            if (Files.exists(Paths.get(outputFile))) {
                try {
                    uploadFile(outputFile, "encodes/$outputFile")
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