package com.megamidnight.k8sencoder

import com.megamidnight.k8sencoder.util.MockClearingExtension
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockClearingExtension::class)
@DisplayName("NodeChunkCreator")
class NodeChunkCreatorTest {

    private var originalNodeCount: String? = null
    private var originalInputVideoUrl: String? = null
    private var originalS3Bucket: String? = null
    private lateinit var mockS3Service: S3Service
    private lateinit var mockVideoSplitter: VideoSplitter

    @BeforeEach
    fun setUp() {
        // Store original system properties
        originalNodeCount = System.getProperty("NODE_COUNT")
        originalInputVideoUrl = System.getProperty("INPUT_VIDEO_URL")
        originalS3Bucket = System.getProperty("S3_BUCKET")

        // Clear system properties
        System.clearProperty("NODE_COUNT")
        System.clearProperty("INPUT_VIDEO_URL")
        System.clearProperty("S3_BUCKET")

        // Setup mocks
        mockS3Service = mockk(relaxed = true)
        mockVideoSplitter = mockk(relaxed = true) {
            every { cleanup() } returns Unit
        }
    }

    @AfterEach
    fun tearDown() {
        // Restore original system properties
        if (originalNodeCount != null) {
            System.setProperty("NODE_COUNT", originalNodeCount!!)
        } else {
            System.clearProperty("NODE_COUNT")
        }
        if (originalInputVideoUrl != null) {
            System.setProperty("INPUT_VIDEO_URL", originalInputVideoUrl!!)
        } else {
            System.clearProperty("INPUT_VIDEO_URL")
        }
        if (originalS3Bucket != null) {
            System.setProperty("S3_BUCKET", originalS3Bucket!!)
        } else {
            System.clearProperty("S3_BUCKET")
        }
    }

    @Nested
    @DisplayName("Node Creation")
    inner class NodeCreation {
        @Test
        @DisplayName("should create nodes with valid input")
        fun createNodesWithValidInput() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_VIDEO_URL", "s3://bucket/video.mkv")
            System.setProperty("S3_BUCKET", "output-bucket")

            val expectedChunks = listOf(
                "s3://output-bucket/chunks/chunk-1.mkv",
                "s3://output-bucket/chunks/chunk-2.mkv"
            )

            every { mockVideoSplitter.splitVideo(any(), 2) } returns expectedChunks

            val creator = NodeChunkCreator(mockS3Service, mockVideoSplitter)

            // when
            val nodes = creator.createNodes()

            // then
            assertThat(nodes)
                .hasSize(2)
                .containsExactly(
                    Node("node1", expectedChunks[0]),
                    Node("node2", expectedChunks[1])
                )

            verify { mockVideoSplitter.splitVideo("s3://bucket/video.mkv", 2) }
            verify { mockVideoSplitter.cleanup() }
        }

        @Test
        @DisplayName("should throw exception when NODE_COUNT is not set")
        fun throwExceptionWhenNodeCountNotSet() {
            // given
            System.clearProperty("NODE_COUNT")
            System.setProperty("INPUT_VIDEO_URL", "s3://bucket/video.mkv")
            System.setProperty("S3_BUCKET", "output-bucket")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator(mockS3Service, mockVideoSplitter)
            }.also {
                assertThat(it.message).isEqualTo("NODE_COUNT is not set")
            }
        }

        @Test
        @DisplayName("should throw exception when INPUT_VIDEO_URL is not set")
        fun throwExceptionWhenInputVideoUrlNotSet() {
            // given
            System.setProperty("NODE_COUNT", "1")
            System.clearProperty("INPUT_VIDEO_URL")
            System.setProperty("S3_BUCKET", "output-bucket")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator(mockS3Service, mockVideoSplitter)
            }.also {
                assertThat(it.message).isEqualTo("INPUT_VIDEO_URL is not set")
            }
        }

        @Test
        @DisplayName("should throw exception when S3_BUCKET is not set")
        fun throwExceptionWhenS3BucketNotSet() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_VIDEO_URL", "s3://bucket/video.mkv")
            System.clearProperty("S3_BUCKET")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator(mockS3Service, mockVideoSplitter)
            }.also {
                assertThat(it.message).isEqualTo("S3_BUCKET is not set")
            }
        }
    }

    @Nested
    @DisplayName("Chunk Creation")
    inner class ChunkCreation {
        @Test
        @DisplayName("should create chunks from nodes with video splitting")
        fun createChunksFromNodes() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_VIDEO_URL", "s3://bucket/video.mkv")
            System.setProperty("S3_BUCKET", "output-bucket")

            val expectedChunks = listOf(
                "s3://output-bucket/chunks/chunk-1.mkv",
                "s3://output-bucket/chunks/chunk-2.mkv"
            )

            every { mockVideoSplitter.splitVideo("s3://bucket/video.mkv", 2) } returns expectedChunks

            val creator = NodeChunkCreator(mockS3Service, mockVideoSplitter)
            val nodes = creator.createNodes()

            // when
            val chunks = creator.createChunks(nodes)

            // then
            assertThat(chunks)
                .hasSize(2)
                .containsExactly(
                    Chunk(expectedChunks[0], "node1"),
                    Chunk(expectedChunks[1], "node2")
                )

            verify(exactly = 1) { mockVideoSplitter.splitVideo("s3://bucket/video.mkv", 2) }
            verify(exactly = 1) { mockVideoSplitter.cleanup() }
        }

        @Test
        @DisplayName("should maintain node-chunk relationship after video splitting")
        fun maintainNodeChunkRelationship() {
            // given
            System.setProperty("NODE_COUNT", "3")
            System.setProperty("INPUT_VIDEO_URL", "s3://bucket/video.mkv")
            System.setProperty("S3_BUCKET", "output-bucket")

            val expectedChunks = listOf(
                "s3://output-bucket/chunks/chunk-1.mkv",
                "s3://output-bucket/chunks/chunk-2.mkv",
                "s3://output-bucket/chunks/chunk-3.mkv"
            )

            every { mockVideoSplitter.splitVideo("s3://bucket/video.mkv", 3) } returns expectedChunks

            val creator = NodeChunkCreator(mockS3Service, mockVideoSplitter)
            val nodes = creator.createNodes()

            // when
            val chunks = creator.createChunks(nodes)

            // then
            nodes.zip(chunks).forEachIndexed { index, (node, chunk) ->
                assertThat(node.chunk).isEqualTo(expectedChunks[index])
                assertThat(node.name).isEqualTo("node${index + 1}")
                assertThat(chunk.urlLocation).isEqualTo(expectedChunks[index])
                assertThat(chunk.nodeName).isEqualTo("node${index + 1}")
            }

            verify(exactly = 1) { mockVideoSplitter.splitVideo("s3://bucket/video.mkv", 3) }
            verify(exactly = 1) { mockVideoSplitter.cleanup() }
        }
    }
}
