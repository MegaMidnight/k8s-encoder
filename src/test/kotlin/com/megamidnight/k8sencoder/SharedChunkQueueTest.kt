package com.megamidnight.k8sencoder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest

@DisplayName("SharedChunkQueue")
class SharedChunkQueueTest {
    private lateinit var chunks: List<Chunk>
    private lateinit var queue: SharedChunkQueue

    @BeforeEach
    fun setUp() {
        chunks = listOf(
            Chunk("s3://bucket/chunk1.mkv", "node1"),
            Chunk("s3://bucket/chunk2.mkv", "node2"),
            Chunk("s3://bucket/chunk3.mkv", "node3")
        )
        queue = SharedChunkQueue(chunks)
    }

    @Test
    @DisplayName("should return all chunks")
    fun returnAllChunks() {
        // when
        val result = queue.getChunks()

        // then
        assertThat(result).containsExactlyElementsOf(chunks)
    }

    @Test
    @DisplayName("should return chunks in sequence")
    fun returnChunksInSequence() {
        // when/then
        chunks.forEach { expectedChunk ->
            val chunk = queue.getNextChunk()
            assertThat(chunk).isEqualTo(expectedChunk)
        }

        // No more chunks
        assertThat(queue.getNextChunk()).isNull()
    }

    @Test
    @DisplayName("should track remaining chunks")
    fun trackRemainingChunks() {
        // initially has chunks
        assertThat(queue.hasMoreChunks()).isTrue()

        // consume all chunks
        repeat(chunks.size) {
            queue.getNextChunk()
        }

        // no more chunks
        assertThat(queue.hasMoreChunks()).isFalse()
    }

    @Test
    @DisplayName("should handle concurrent access")
    fun handleConcurrentAccess() = runTest {
        val workerCount = 3
        val results = mutableListOf<Chunk?>()
        val jobs = List(workerCount) {
            async {
                val chunk = queue.getNextChunk()
                synchronized(results) {
                    results.add(chunk)
                }
            }
        }

        // Wait for all workers to complete
        jobs.awaitAll()

        // Verify results
        assertThat(results)
            .filteredOn { it != null }
            .hasSize(chunks.size.coerceAtMost(workerCount))
            .containsAnyElementsOf(chunks)

        // Verify no duplicates
        assertThat(results.filterNotNull().distinct())
            .hasSameSizeAs(results.filterNotNull())
    }

    @Test
    @DisplayName("should handle empty queue")
    fun handleEmptyQueue() {
        // given
        val emptyQueue = SharedChunkQueue(emptyList())

        // when/then
        assertThat(emptyQueue.hasMoreChunks()).isFalse()
        assertThat(emptyQueue.getNextChunk()).isNull()
        assertThat(emptyQueue.getChunks()).isEmpty()
    }
}