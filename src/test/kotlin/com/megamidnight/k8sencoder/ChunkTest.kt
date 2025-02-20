package com.megamidnight.k8sencoder

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.assertj.core.api.Assertions.assertThat
import com.megamidnight.k8sencoder.util.MockClearingExtension
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested

@ExtendWith(MockClearingExtension::class)
@DisplayName("Chunk")
class ChunkTest {

    @Nested
    @DisplayName("Creation")
    inner class Creation {
        @Test
        @DisplayName("should create Chunk with valid parameters")
        fun createWithValidParams() {
            // given
            val urlLocation = "s3://bucket/video/chunk1.mp4"
            val nodeName = "node1"

            // when
            val chunk = Chunk(urlLocation, nodeName)

            // then
            assertThat(chunk.urlLocation).isEqualTo(urlLocation)
            assertThat(chunk.nodeName).isEqualTo(nodeName)
        }

        @Test
        @DisplayName("should handle empty strings")
        fun handleEmptyStrings() {
            // given
            val chunk = Chunk("", "")

            // then
            assertThat(chunk.urlLocation).isEmpty()
            assertThat(chunk.nodeName).isEmpty()
        }

        @Test
        @DisplayName("should handle whitespace strings")
        fun handleWhitespaceStrings() {
            // given
            val chunk = Chunk(" ", "\t")

            // then
            assertThat(chunk.urlLocation).isBlank()
            assertThat(chunk.nodeName).isBlank()
        }

        @Test
        @DisplayName("should handle various URL formats")
        fun handleVariousUrlFormats() {
            // given
            val urlFormats = listOf(
                "s3://bucket/path/file.mp4",
                "https://domain.com/path/file.mp4",
                "file:///path/to/file.mp4",
                "/absolute/path/file.mp4",
                "relative/path/file.mp4"
            )

            // then
            urlFormats.forEach { url ->
                val chunk = Chunk(url, "node1")
                assertThat(chunk.urlLocation).isEqualTo(url)
            }
        }
    }

    @Nested
    @DisplayName("Data class functionality")
    inner class DataClassFunctionality {
        @Test
        @DisplayName("should implement equals and hashCode correctly")
        fun implementEqualsAndHashCode() {
            // given
            val chunk1 = Chunk("url1", "node1")
            val chunk2 = Chunk("url1", "node1")
            val chunk3 = Chunk("url2", "node1")
            val chunk4 = Chunk("url1", "node2")

            // then
            assertThat(chunk1)
                .isEqualTo(chunk2)
                .isNotEqualTo(chunk3)
                .isNotEqualTo(chunk4)
                .hasSameHashCodeAs(chunk2)

            assertThat(chunk1.hashCode())
                .isNotEqualTo(chunk3.hashCode())
                .isNotEqualTo(chunk4.hashCode())
        }

        @Test
        @DisplayName("should implement toString correctly")
        fun implementToString() {
            // given
            val chunk = Chunk("test-url", "test-node")

            // when
            val toString = chunk.toString()

            // then
            assertThat(toString)
                .contains("test-url")
                .contains("test-node")
                .contains("Chunk")
        }

        @Test
        @DisplayName("should support copy operation")
        fun supportCopy() {
            // given
            val original = Chunk("original-url", "original-node")

            // when
            val copyWithNewUrl = original.copy(urlLocation = "new-url")
            val copyWithNewNode = original.copy(nodeName = "new-node")
            val exactCopy = original.copy()

            // then
            assertThat(copyWithNewUrl)
                .isNotEqualTo(original)
                .extracting("urlLocation", "nodeName")
                .containsExactly("new-url", "original-node")

            assertThat(copyWithNewNode)
                .isNotEqualTo(original)
                .extracting("urlLocation", "nodeName")
                .containsExactly("original-url", "new-node")

            assertThat(exactCopy)
                .isEqualTo(original)
                .extracting("urlLocation", "nodeName")
                .containsExactly("original-url", "original-node")
        }
    }
}