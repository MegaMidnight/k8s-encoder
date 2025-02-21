package com.megamidnight.k8sencoder

import io.mockk.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.util.UUID

@DisplayName("VideoSplitter")
class VideoSplitterTest {
    private lateinit var mockS3Service: S3Service
    private lateinit var videoSplitter: VideoSplitter
    private val s3Bucket = "test-bucket"

    @BeforeEach
    fun setUp() {
        mockS3Service = mockk(relaxed = true)
        videoSplitter = VideoSplitter(mockS3Service, s3Bucket)
    }

    @Nested
    @DisplayName("splitVideo")
    inner class SplitVideo {
        @Test
        @DisplayName("should split video and upload chunks")
        fun splitVideoAndUploadChunks(@TempDir tempDir: Path) {
            // given
            val s3VideoUrl = "s3://source-bucket/video.mkv"
            val nodeCount = 2
            val downloadedFile = tempDir.resolve("input-${UUID.randomUUID()}.mkv").toFile()
            downloadedFile.createNewFile()

            val expectedChunks = listOf(
                "s3://test-bucket/chunks/chunk-1.mkv",
                "s3://test-bucket/chunks/chunk-2.mkv"
            )

            // Mock S3 download
            every { mockS3Service.downloadVideo(s3VideoUrl, any()) } answers {
                val targetPath = secondArg<Path>()
                File(targetPath.toString()).createNewFile()
            }

            // Mock process execution
            val durationOutput = """
                |File 'input.mkv': container: Matroska
                |Duration: 120
            """.trimMargin()

            val mockDurationProcess = mockk<Process> {
                every { inputStream } returns durationOutput.byteInputStream()
                every { errorStream } returns "".byteInputStream()
                every { waitFor() } returns 0
                every { exitValue() } returns 0
            }

            val mockSplitProcess = mockk<Process> {
                every { inputStream } returns "".byteInputStream()
                every { errorStream } returns "".byteInputStream()
                every { waitFor() } returns 0
                every { exitValue() } returns 0
            }

            mockkConstructor(ProcessBuilder::class) {
                val mockBuilder = mockk<ProcessBuilder>()

                every { anyConstructed<ProcessBuilder>().command("mkvmerge", "-i", any()) } returns mockBuilder
                every { anyConstructed<ProcessBuilder>().command("mkvmerge", "-o", any(), "--split", any(), "--split-points", any(), any()) } returns mockBuilder
                every { mockBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE) } returns mockBuilder
                every { mockBuilder.redirectError(ProcessBuilder.Redirect.PIPE) } returns mockBuilder
                every { mockBuilder.start() } returnsMany listOf(mockDurationProcess, mockSplitProcess)

                // Mock S3 uploads
                every { 
                    mockS3Service.uploadChunk(any(), s3Bucket, match { it.startsWith("chunks/chunk-") })
                } returnsMany expectedChunks

                // when
                val result = videoSplitter.splitVideo(s3VideoUrl, nodeCount)

                // then
                assertThat(result).containsExactlyElementsOf(expectedChunks)
                verify { mockS3Service.downloadVideo(s3VideoUrl, any()) }
                verify(exactly = nodeCount) { mockS3Service.uploadChunk(any(), s3Bucket, any()) }
            }
        }

        @Test
        @DisplayName("should handle mkvmerge errors")
        fun handleMkvmergeErrors(@TempDir tempDir: Path) {
            // given
            val s3VideoUrl = "s3://source-bucket/video.mkv"
            val nodeCount = 2
            val downloadedFile = tempDir.resolve("input-${UUID.randomUUID()}.mkv").toFile()
            downloadedFile.createNewFile()

            // Mock S3 download
            every { mockS3Service.downloadVideo(s3VideoUrl, any()) } answers {
                val targetPath = secondArg<Path>()
                File(targetPath.toString()).createNewFile()
            }

            // Mock process execution with error
            val mockErrorProcess = mockk<Process> {
                every { inputStream } returns "Error: Invalid file format".byteInputStream()
                every { errorStream } returns "".byteInputStream()
                every { waitFor() } returns 1
                every { exitValue() } returns 1
            }

            mockkConstructor(ProcessBuilder::class) {
                val mockBuilder = mockk<ProcessBuilder>()

                every { anyConstructed<ProcessBuilder>().command("mkvmerge", "-i", any()) } returns mockBuilder
                every { mockBuilder.redirectOutput(ProcessBuilder.Redirect.PIPE) } returns mockBuilder
                every { mockBuilder.redirectError(ProcessBuilder.Redirect.PIPE) } returns mockBuilder
                every { mockBuilder.start() } returns mockErrorProcess

                // when/then
                assertThrows<RuntimeException> {
                    videoSplitter.splitVideo(s3VideoUrl, nodeCount)
                }
            }
        }

        @Test
        @DisplayName("should cleanup temporary files")
        fun cleanupTemporaryFiles(@TempDir tempDir: Path) {
            // given
            val s3VideoUrl = "s3://source-bucket/video.mkv"
            val nodeCount = 2

            // Mock S3 download to fail
            every { mockS3Service.downloadVideo(s3VideoUrl, any()) } throws RuntimeException("Download failed")

            // when
            assertThrows<RuntimeException> {
                videoSplitter.splitVideo(s3VideoUrl, nodeCount)
            }

            // then
            videoSplitter.cleanup()
            assertThat(tempDir.toFile().listFiles()).isEmpty()
        }
    }

    private fun mockProcess(output: String, exitValue: Int = 0): Process = mockk {
        every { inputStream } returns output.byteInputStream()
        every { errorStream } returns "".byteInputStream()
        every { waitFor() } returns exitValue
        every { exitValue() } returns exitValue
    }
}
