import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class VideoFileHandler {
    private val spacesEndpoint = System.getenv("SPACES_ENDPOINT") ?: throw IllegalArgumentException("SPACES_ENDPOINT is not set")
    private val spacesBucket = System.getenv("SPACES_BUCKET") ?: throw IllegalArgumentException("SPACES_BUCKET is not set")
    private val spacesAccessKey = System.getenv("SPACES_ACCESS_KEY")?: throw IllegalArgumentException("SPACES_ACCESS_KEY is not set")
    private val spacesSecretKey = System.getenv("SPACES_SECRET_KEY")?: throw IllegalArgumentException("SPACES_SECRET_KEY is not set")

    fun downloadFile(fileUrl: String): String {
        println("Downloading file: $fileUrl")
        val url = URI(fileUrl).toURL()
        val fileName = url.file.split("/").last()
        val destination = File(fileName).toPath()

        val future = CompletableFuture.runAsync {
            url.openStream().use { inputStream ->
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        try {
            future.get(20, TimeUnit.MINUTES) // Wait for 20 minutes
            println("Download complete: $fileName") // Debug print statement
        } catch (e: TimeoutException) {
            println("Download did not complete within the expected time")
        } catch (e: InterruptedException) {
            println("Download was interrupted")
        } catch (e: ExecutionException) {
            println("An error occurred during the download: ${e.message}")
        }
        return fileName
    }

    fun uploadFile(filePath: String, destinationPath: String): PutObjectResponse {
        println("Uploading file...$filePath to $destinationPath")
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
}