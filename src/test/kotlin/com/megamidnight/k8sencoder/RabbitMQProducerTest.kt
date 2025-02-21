package com.megamidnight.k8sencoder

import com.rabbitmq.client.*
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import com.megamidnight.k8sencoder.util.MockClearingExtension
import java.nio.charset.StandardCharsets

@ExtendWith(MockClearingExtension::class)
@DisplayName("RabbitMQProducer")
class RabbitMQProducerTest {
    private lateinit var mockConnection: Connection
    private lateinit var mockChannel: Channel
    private lateinit var mockChunkQueue: SharedChunkQueue
    private lateinit var producer: RabbitMQProducer

    @BeforeEach
    fun setUp() {
        mockConnection = mockk(relaxed = true)
        mockChannel = mockk(relaxed = true)
        mockChunkQueue = mockk(relaxed = true)

        every { mockConnection.createChannel() } returns mockChannel
        every { mockChannel.queueDeclare(any(), any(), any(), any(), any()) } returns mockk()

        producer = RabbitMQProducer(mockConnection, mockChunkQueue)
    }

    @Nested
    @DisplayName("sendChunks")
    inner class SendChunks {
        @Test
        @DisplayName("should send all chunks to queue")
        fun sendAllChunksToQueue() {
            // given
            val chunks = listOf(
                Chunk("s3://bucket/chunk1.mkv", "node1"),
                Chunk("s3://bucket/chunk2.mkv", "node2")
            )
            every { mockChunkQueue.getChunks() } returns chunks
            val messageSlots = mutableListOf<ByteArray>()

            // when
            producer.sendChunks()

            // then
            verify(exactly = chunks.size) { 
                mockChannel.basicPublish(
                    "",
                    "chunks",
                    null,
                    capture(messageSlots)
                )
            }

            // Verify message content
            messageSlots.forEachIndexed { index, message ->
                val messageStr = message.toString(StandardCharsets.UTF_8)
                val chunk = chunks[index]
                assert(messageStr.contains(chunk.urlLocation)) { "Message should contain URL: ${chunk.urlLocation}" }
                assert(messageStr.contains(chunk.nodeName)) { "Message should contain node name: ${chunk.nodeName}" }
            }
        }

        @Test
        @DisplayName("should handle empty chunk queue")
        fun handleEmptyChunkQueue() {
            // given
            every { mockChunkQueue.getChunks() } returns emptyList()

            // when
            producer.sendChunks()

            // then
            verify(exactly = 0) { 
                mockChannel.basicPublish(any(), any(), any(), any())
            }
        }

        @Test
        @DisplayName("should handle channel errors")
        fun handleChannelErrors() {
            // given
            val chunks = listOf(Chunk("s3://bucket/chunk1.mkv", "node1"))
            every { mockChunkQueue.getChunks() } returns chunks
            every { 
                mockChannel.basicPublish(any(), any(), any(), any())
            } throws RuntimeException("Channel error")

            // when/then
            assertThrows<RuntimeException> {
                producer.sendChunks()
            }
        }
    }

    @Nested
    @DisplayName("close")
    inner class Close {
        @Test
        @DisplayName("should close channel and connection")
        fun closeChannelAndConnection() {
            // when
            producer.close()

            // then
            verify(exactly = 1) {
                mockChannel.close()
                mockConnection.close()
            }
        }

        @Test
        @DisplayName("should handle close errors")
        fun handleCloseErrors() {
            // given
            every { mockChannel.close() } throws RuntimeException("Close error")

            // when/then
            assertThrows<RuntimeException> {
                producer.close()
            }
        }
    }
}
