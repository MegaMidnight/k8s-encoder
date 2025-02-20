package com.megamidnight.k8sencoder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.assertj.core.api.Assertions.assertThat
import com.megamidnight.k8sencoder.util.MockClearingExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@ExtendWith(MockClearingExtension::class)
@DisplayName("Node")
class NodeTest {

    @Nested
    @DisplayName("Creation")
    inner class Creation {
        @Test
        @DisplayName("should create Node with valid parameters")
        fun createWithValidParams() {
            // given
            val name = "test-node"
            val chunk = "test-chunk"

            // when
            val node = Node(name, chunk)

            // then
            assertThat(node.name).isEqualTo(name)
            assertThat(node.chunk).isEqualTo(chunk)
        }

        @Test
        @DisplayName("should handle empty strings")
        fun handleEmptyStrings() {
            // given
            val node = Node("", "")

            // then
            assertThat(node.name).isEmpty()
            assertThat(node.chunk).isEmpty()
        }

        @Test
        @DisplayName("should handle whitespace strings")
        fun handleWhitespaceStrings() {
            // given
            val node = Node(" ", "\t")

            // then
            assertThat(node.name).isBlank()
            assertThat(node.chunk).isBlank()
        }
    }

    @Nested
    @DisplayName("Data class functionality")
    inner class DataClassFunctionality {
        @Test
        @DisplayName("should implement equals and hashCode correctly")
        fun implementEqualsAndHashCode() {
            // given
            val node1 = Node("node1", "chunk1")
            val node2 = Node("node1", "chunk1")
            val node3 = Node("node2", "chunk1")
            val node4 = Node("node1", "chunk2")

            // then
            assertThat(node1)
                .isEqualTo(node2)
                .isNotEqualTo(node3)
                .isNotEqualTo(node4)
                .hasSameHashCodeAs(node2)

            assertThat(node1.hashCode())
                .isNotEqualTo(node3.hashCode())
                .isNotEqualTo(node4.hashCode())
        }

        @Test
        @DisplayName("should implement toString correctly")
        fun implementToString() {
            // given
            val node = Node("test-node", "test-chunk")

            // when
            val toString = node.toString()

            // then
            assertThat(toString)
                .contains("test-node")
                .contains("test-chunk")
                .contains("Node")
        }

        @Test
        @DisplayName("should support copy operation")
        fun supportCopy() {
            // given
            val original = Node("original", "chunk")

            // when
            val copyWithNewName = original.copy(name = "copy")
            val copyWithNewChunk = original.copy(chunk = "new-chunk")
            val exactCopy = original.copy()

            // then
            assertThat(copyWithNewName)
                .isNotEqualTo(original)
                .extracting("name", "chunk")
                .containsExactly("copy", "chunk")

            assertThat(copyWithNewChunk)
                .isNotEqualTo(original)
                .extracting("name", "chunk")
                .containsExactly("original", "new-chunk")

            assertThat(exactCopy)
                .isEqualTo(original)
                .extracting("name", "chunk")
                .containsExactly("original", "chunk")
        }
    }
}
