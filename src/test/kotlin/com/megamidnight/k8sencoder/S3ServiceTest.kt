package com.megamidnight.k8sencoder

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import java.nio.file.Path

@DisplayName("S3Service")
class S3ServiceTest {
    private lateinit var mockS3Client: S3Client
    private lateinit var s3Service: S3Service

    @BeforeEach
    fun setUp() {
        mockS3Client = mockk(relaxed = true)
        s3Service = S3Service(mockS3Client)
    }

    @Nested
    @DisplayName("downloadVideo")
    inner class DownloadVideo {
        @Test
        @DisplayName("should download video from S3")
        fun downloadVideoFromS3() {
            // given
            val s3Url = "s3://test-bucket/videos/test.mkv"
            val localPath = Path.of("/tmp/test.mkv")
            val getObjectRequestSlot = slot<GetObjectRequest>()

            // when
            s3Service.downloadVideo(s3Url, localPath)

            // then
            verify { mockS3Client.getObject(capture(getObjectRequestSlot), localPath) }
            assertThat(getObjectRequestSlot.captured.bucket()).isEqualTo("test-bucket")
            assertThat(getObjectRequestSlot.captured.key()).isEqualTo("videos/test.mkv")
        }

        @Test
        @DisplayName("should throw exception for invalid S3 URL")
        fun throwExceptionForInvalidS3Url() {
            // given
            val invalidS3Url = "invalid://url"
            val localPath = Path.of("/tmp/test.mkv")

            // when/then
            assertThrows<IllegalArgumentException> {
                s3Service.downloadVideo(invalidS3Url, localPath)
            }
        }

        @Test
        @DisplayName("should handle S3 errors during download")
        fun handleS3ErrorsDuringDownload() {
            // given
            val s3Url = "s3://test-bucket/videos/test.mkv"
            val localPath = Path.of("/tmp/test.mkv")
            every { mockS3Client.getObject(any<GetObjectRequest>(), any<Path>()) } throws S3Exception.builder()
                .message("Access denied")
                .build()

            // when/then
            assertThrows<S3Exception> {
                s3Service.downloadVideo(s3Url, localPath)
            }
        }
    }

    @Nested
    @DisplayName("uploadChunk")
    inner class UploadChunk {
        @Test
        @DisplayName("should upload chunk to S3")
        fun uploadChunkToS3(@TempDir tempDir: Path) {
            // given
            val tempFile = tempDir.resolve("test-chunk.mkv").toFile()
            tempFile.writeText("test content")

            val s3Bucket = "test-bucket"
            val s3Key = "chunks/chunk-1.mkv"
            val putObjectRequestSlot = slot<PutObjectRequest>()
            val requestBodySlot = slot<RequestBody>()

            // when
            val result = s3Service.uploadChunk(tempFile, s3Bucket, s3Key)

            // then
            verify { mockS3Client.putObject(capture(putObjectRequestSlot), capture(requestBodySlot)) }
            assertThat(putObjectRequestSlot.captured.bucket()).isEqualTo(s3Bucket)
            assertThat(putObjectRequestSlot.captured.key()).isEqualTo(s3Key)
            assertThat(result).isEqualTo("s3://$s3Bucket/$s3Key")
        }

        @Test
        @DisplayName("should handle S3 errors during upload")
        fun handleS3ErrorsDuringUpload(@TempDir tempDir: Path) {
            // given
            val tempFile = tempDir.resolve("test-chunk.mkv").toFile()
            tempFile.writeText("test content")

            val s3Bucket = "test-bucket"
            val s3Key = "chunks/chunk-1.mkv"
            every { mockS3Client.putObject(any<PutObjectRequest>(), any<RequestBody>()) } throws S3Exception.builder()
                .message("Access denied")
                .statusCode(403)
                .build()

            // when/then
            assertThrows<S3Exception> {
                s3Service.uploadChunk(tempFile, s3Bucket, s3Key)
            }
        }

        @Test
        @DisplayName("should throw exception when local file doesn't exist")
        fun throwExceptionWhenLocalFileDoesntExist(@TempDir tempDir: Path) {
            // given
            val nonExistentFile = tempDir.resolve("non-existent.mkv").toFile()
            val s3Bucket = "test-bucket"
            val s3Key = "chunks/chunk-1.mkv"

            // when/then
            assertThrows<IllegalArgumentException> {
                s3Service.uploadChunk(nonExistentFile, s3Bucket, s3Key)
            }
        }
    }

    @Nested
    @DisplayName("parseS3Url")
    inner class ParseS3Url {
        @Test
        @DisplayName("should parse valid S3 URLs")
        fun parseValidS3Urls() {
            // given
            val testCases = mapOf(
                "s3://test-bucket/file.mkv" to Pair("test-bucket", "file.mkv"),
                "s3://test-bucket/path/to/file.mkv" to Pair("test-bucket", "path/to/file.mkv"),
                "s3://my-bucket-123/some/deep/path/file.mkv" to Pair("my-bucket-123", "some/deep/path/file.mkv")
            )

            testCases.forEach { (url, expected) ->
                // when
                val (bucket, key) = s3Service.parseS3Url(url)

                // then
                assertThat(bucket).isEqualTo(expected.first)
                assertThat(key).isEqualTo(expected.second)
            }
        }

        @Test
        @DisplayName("should throw exception for invalid S3 URLs")
        fun throwExceptionForInvalidS3Urls() {
            // given
            val testCases = mapOf(
                "http://test-bucket/file.mkv" to "Invalid S3 URL format. Must start with 's3://'",
                "s3:/test-bucket/file.mkv" to "Invalid S3 URL format. Must start with 's3://'",
                "s3://test-bucket" to "Invalid S3 URL format. Must be in format 's3://bucket/key'",
                "s3://test-bucket/" to "Invalid S3 URL format. Must be in format 's3://bucket/key'",
                "s3:///file.mkv" to "Invalid S3 URL format. Must be in format 's3://bucket/key'",
                "s3://" to "Invalid S3 URL format. Must be in format 's3://bucket/key'",
                "invalid" to "Invalid S3 URL format. Must start with 's3://'"
            )

            testCases.forEach { (url, expectedMessage) ->
                // when/then
                val exception = assertThrows<IllegalArgumentException> {
                    s3Service.parseS3Url(url)
                }
                assertThat(exception.message).isEqualTo(expectedMessage)
            }
        }
    }
}
