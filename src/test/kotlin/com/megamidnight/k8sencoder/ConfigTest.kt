package com.megamidnight.k8sencoder

import com.megamidnight.k8sencoder.util.MockClearingExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatList
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockClearingExtension::class)
@DisplayName("Config")
class ConfigTest {

    @Nested
    @DisplayName("Creation")
    inner class Creation {
        @Test
        @DisplayName("should create Config with valid parameters")
        fun createWithValidParams() {
            // given
            val nodes = listOf(
                Node("node1", "chunk1"),
                Node("node2", "chunk2")
            )
            val chunks = listOf(
                Chunk("url1", "node1"),
                Chunk("url2", "node2")
            )

            // when
            val config = Config.create(nodes, chunks)

            // then
            assertThatList(config.nodes).containsExactlyElementsOf(nodes)
            assertThatList(config.chunks).containsExactlyElementsOf(chunks)
        }

        @Test
        @DisplayName("should handle null inputs")
        fun handleNullInputs() {
            // given
            val nodes: List<Node>? = null
            val chunks: List<Chunk>? = null

            // when
            val config = Config.create(nodes ?: emptyList(), chunks ?: emptyList())

            // then
            assertThatList(config.nodes).isEmpty()
            assertThatList(config.chunks).isEmpty()
        }

        @Test
        @DisplayName("should handle empty lists")
        fun handleEmptyLists() {
            // when
            val config = Config.create(emptyList(), emptyList())

            // then
            assertThatList(config.nodes).isEmpty()
            assertThatList(config.chunks).isEmpty()
        }

        @Test
        @DisplayName("should preserve list order")
        fun preserveListOrder() {
            // given
            val nodes = listOf(
                Node("node1", "chunk1"),
                Node("node2", "chunk2"),
                Node("node3", "chunk3")
            )
            val chunks = listOf(
                Chunk("url1", "node1"),
                Chunk("url2", "node2"),
                Chunk("url3", "node3")
            )

            // when
            val config = Config.create(nodes, chunks)

            // then
            assertThatList(config.nodes)
                .containsExactlyElementsOf(nodes)
                .isSortedAccordingTo(compareBy { it.name })

            assertThatList(config.chunks)
                .containsExactlyElementsOf(chunks)
                .isSortedAccordingTo(compareBy { it.urlLocation })
        }

        @Test
        @DisplayName("should ensure list immutability")
        fun ensureListImmutability() {
            // given
            val mutableNodes = mutableListOf(Node("node1", "chunk1"))
            val mutableChunks = mutableListOf(Chunk("url1", "node1"))

            // when
            val config = Config.create(mutableNodes, mutableChunks)
            mutableNodes.add(Node("node2", "chunk2"))
            mutableChunks.add(Chunk("url2", "node2"))

            // then
            assertThatList(config.nodes)
                .hasSize(1)
                .containsExactly(Node("node1", "chunk1"))
            assertThatList(config.chunks)
                .hasSize(1)
                .containsExactly(Chunk("url1", "node1"))
            assertThatList(mutableNodes).hasSize(2)
            assertThatList(mutableChunks).hasSize(2)
        }
    }

    @Nested
    @DisplayName("Data class functionality")
    inner class DataClassFunctionality {
        @Test
        @DisplayName("should implement equals and hashCode correctly")
        fun implementEqualsAndHashCode() {
            // given
            val nodes1 = listOf(Node("node1", "chunk1"))
            val chunks1 = listOf(Chunk("url1", "node1"))
            val nodes2 = listOf(Node("node2", "chunk2"))
            val chunks2 = listOf(Chunk("url2", "node2"))

            val config1 = Config.create(nodes1, chunks1)
            val config2 = Config.create(nodes1, chunks1)
            val config3 = Config.create(nodes2, chunks1)
            val config4 = Config.create(nodes1, chunks2)

            // then
            assertThat(config1)
                .isEqualTo(config2)
                .isNotEqualTo(config3)
                .isNotEqualTo(config4)
                .hasSameHashCodeAs(config2)

            assertThat(config1.hashCode())
                .isNotEqualTo(config3.hashCode())
                .isNotEqualTo(config4.hashCode())
        }

        @Test
        @DisplayName("should implement toString correctly")
        fun implementToString() {
            // given
            val nodes = listOf(Node("test-node", "test-chunk"))
            val chunks = listOf(Chunk("test-url", "test-node"))
            val config = Config.create(nodes, chunks)

            // when
            val toString = config.toString()

            // then
            assertThat(toString)
                .contains("test-node")
                .contains("test-chunk")
                .contains("test-url")
                .contains("Config")
        }

        @Test
        @DisplayName("should support copy operation")
        fun supportCopy() {
            // given
            val originalNodes = listOf(Node("node1", "chunk1"))
            val originalChunks = listOf(Chunk("url1", "node1"))
            val original = Config.create(originalNodes, originalChunks)

            val newNodes = listOf(Node("node2", "chunk2"))
            val newChunks = listOf(Chunk("url2", "node2"))

            // when
            val copyWithNewNodes = Config.create(newNodes, original.chunks)
            val copyWithNewChunks = Config.create(original.nodes, newChunks)
            val exactCopy = Config.create(original.nodes, original.chunks)

            // then
            assertThat(copyWithNewNodes).isNotEqualTo(original)
            assertThatList(copyWithNewNodes.nodes).containsExactlyElementsOf(newNodes)
            assertThatList(copyWithNewNodes.chunks).containsExactlyElementsOf(original.chunks)

            assertThat(copyWithNewChunks).isNotEqualTo(original)
            assertThatList(copyWithNewChunks.nodes).containsExactlyElementsOf(original.nodes)
            assertThatList(copyWithNewChunks.chunks).containsExactlyElementsOf(newChunks)

            assertThat(exactCopy).isEqualTo(original)
            assertThatList(exactCopy.nodes).containsExactlyElementsOf(originalNodes)
            assertThatList(exactCopy.chunks).containsExactlyElementsOf(originalChunks)
        }
    }

    @Nested
    @DisplayName("List operations")
    inner class ListOperations {
        @Test
        @DisplayName("should maintain list immutability")
        fun maintainListImmutability() {
            // given
            val mutableNodes = mutableListOf(Node("node1", "chunk1"))
            val mutableChunks = mutableListOf(Chunk("url1", "node1"))
            val config = Config.create(mutableNodes, mutableChunks)

            // when
            mutableNodes.add(Node("node2", "chunk2"))
            mutableChunks.add(Chunk("url2", "node2"))

            // then
            assertThat(config.nodes).hasSize(1)
            assertThat(config.chunks).hasSize(1)
        }

        @Test
        @DisplayName("should handle large lists")
        fun handleLargeLists() {
            // given
            val largeNodes = (1..1000).map { Node("node$it", "chunk$it") }
            val largeChunks = (1..1000).map { Chunk("url$it", "node$it") }

            // when
            val config = Config.create(largeNodes, largeChunks)

            // then
            assertThatList(config.nodes)
                .hasSize(1000)
                .first().extracting("name").isEqualTo("node1")
            assertThatList(config.nodes)
                .last().extracting("name").isEqualTo("node1000")

            assertThatList(config.chunks)
                .hasSize(1000)
                .first().extracting("urlLocation").isEqualTo("url1")
            assertThatList(config.chunks)
                .last().extracting("urlLocation").isEqualTo("url1000")
        }
    }
}
