package com.megamidnight.k8sencoder

import com.megamidnight.k8sencoder.util.MockClearingExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockClearingExtension::class)
@DisplayName("NodeChunkCreator")
class NodeChunkCreatorTest {

    private var originalNodeCount: String? = null
    private var originalInputFileUrl: String? = null

    @BeforeEach
    fun setUp() {
        // Store original environment variables
        originalNodeCount = System.getenv("NODE_COUNT")
        originalInputFileUrl = System.getenv("INPUT_FILE_URL")

        // Clear environment variables
        System.clearProperty("NODE_COUNT")
        System.clearProperty("INPUT_FILE_URL")
    }

    @AfterEach
    fun tearDown() {
        // Restore original environment variables
        if (originalNodeCount != null) {
            System.setProperty("NODE_COUNT", originalNodeCount!!)
        } else {
            System.clearProperty("NODE_COUNT")
        }
        if (originalInputFileUrl != null) {
            System.setProperty("INPUT_FILE_URL", originalInputFileUrl!!)
        } else {
            System.clearProperty("INPUT_FILE_URL")
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
            System.setProperty("INPUT_FILE_URL", "url1,url2")
            val creator = NodeChunkCreator()

            // when
            val nodes = creator.createNodes()

            // then
            assertThat(nodes)
                .hasSize(2)
                .containsExactly(
                    Node("node1", "url1"),
                    Node("node2", "url2")
                )
        }

        @Test
        @DisplayName("should throw exception when NODE_COUNT is not set")
        fun throwExceptionWhenNodeCountNotSet() {
            // given
            System.clearProperty("NODE_COUNT")
            System.setProperty("INPUT_FILE_URL", "url1")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator()
            }.also {
                assertThat(it.message).isEqualTo("NODE_COUNT is not set")
            }
        }

        @Test
        @DisplayName("should throw exception when INPUT_FILE_URL is not set")
        fun throwExceptionWhenInputFileUrlNotSet() {
            // given
            System.setProperty("NODE_COUNT", "1")
            System.clearProperty("INPUT_FILE_URL")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator()
            }.also {
                assertThat(it.message).isEqualTo("INPUT_FILE_URL is not set")
            }
        }

        @Test
        @DisplayName("should throw exception when node count doesn't match URL count")
        fun throwExceptionWhenCountsMismatch() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_FILE_URL", "url1")

            // then
            assertThrows<IllegalArgumentException> {
                NodeChunkCreator()
            }.also {
                assertThat(it.message).isEqualTo("Mismatch in the number of nodes and number of chunks.")
            }
        }
    }

    @Nested
    @DisplayName("Chunk Creation")
    inner class ChunkCreation {
        @Test
        @DisplayName("should create chunks from nodes")
        fun createChunksFromNodes() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_FILE_URL", "url1,url2")
            val creator = NodeChunkCreator()
            val nodes = creator.createNodes()

            // when
            val chunks = creator.createChunks(nodes)

            // then
            assertThat(chunks)
                .hasSize(2)
                .containsExactly(
                    Chunk("url1", "node1"),
                    Chunk("url2", "node2")
                )
        }

        @Test
        @DisplayName("should maintain node-chunk relationship")
        fun maintainNodeChunkRelationship() {
            // given
            System.setProperty("NODE_COUNT", "2")
            System.setProperty("INPUT_FILE_URL", "url1,url2")
            val creator = NodeChunkCreator()
            val nodes = creator.createNodes()

            // when
            val chunks = creator.createChunks(nodes)

            // then
            nodes.zip(chunks).forEach { (node: Node, chunk: Chunk) ->
                assertThat(node.chunk).isEqualTo(chunk.urlLocation)
                assertThat(node.name).isEqualTo(chunk.nodeName)
            }
        }
    }
}
