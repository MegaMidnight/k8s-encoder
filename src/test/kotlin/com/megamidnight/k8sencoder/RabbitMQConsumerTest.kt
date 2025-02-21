package com.megamidnight.k8sencoder

import com.rabbitmq.client.*
import io.mockk.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import com.megamidnight.k8sencoder.util.MockClearingExtension
import redis.clients.jedis.Jedis
import java.nio.charset.StandardCharsets

@ExtendWith(MockClearingExtension::class)
@DisplayName("RabbitMQConsumer")
class RabbitMQConsumerTest {
    private lateinit var mockConnection: Connection
    private lateinit var mockChannel: Channel
    private lateinit var mockEncoder: FFmpegEncoder
    private lateinit var mockJedis: Jedis
    private lateinit var consumer: RabbitMQConsumer

    @BeforeEach
    fun setUp() {
        // Setup environment variables
        System.setProperty("REDIS_HOST", "localhost")
        System.setProperty("REDIS_PORT", "6379")

        // Setup mocks
        mockConnection = mockk(relaxed = true)
        mockChannel = mockk(relaxed = true)
        mockEncoder = mockk(relaxed = true)
        mockJedis = mockk(relaxed = true)

        every { mockConnection.createChannel() } returns mockChannel
        every { mockChannel.queueDeclare(any(), any(), any(), any(), any()) } returns mockk()

        // Mock Jedis constructor
        mockkConstructor(Jedis::class)
        every { 
            anyConstructed<Jedis>().get(any<String>()) 
        } answers { 
            mockJedis.get(firstArg<String>()) 
        }
        every { 
            anyConstructed<Jedis>().set(any<String>(), any<String>()) 
        } answers { 
            mockJedis.set(firstArg<String>(), secondArg<String>()) 
        }
        every { 
            anyConstructed<Jedis>().del(any<String>()) 
        } answers { 
            mockJedis.del(firstArg<String>()) 
        }
        every { 
            anyConstructed<Jedis>().close() 
        } just Runs

        consumer = RabbitMQConsumer(mockConnection, mockEncoder)
    }

    @AfterEach
    fun tearDown() {
        System.clearProperty("REDIS_HOST")
        System.clearProperty("REDIS_PORT")
        unmockkConstructor(Jedis::class)
    }

    @Nested
    @DisplayName("consumeChunks")
    inner class ConsumeChunks {
        @Test
        @DisplayName("should process message successfully")
        fun processMessageSuccessfully() {
            // given
            val deliveryTag = 1L
            val chunk = Chunk("s3://bucket/video.mkv", "node1")
            val message = """
                {
                    "urlLocation": "${chunk.urlLocation}",
                    "nodeName": "${chunk.nodeName}"
                }
            """.trimIndent()

            every { mockEncoder.encode(any(), any(), any()) } just Runs

            // Capture the consumer to simulate message delivery
            val consumerSlot = slot<Consumer>()
            every { 
                mockChannel.basicConsume(any(), any(), capture(consumerSlot))
            } answers {
                // Simulate message delivery
                consumerSlot.captured.handleDelivery(
                    "consumerTag",
                    Envelope(deliveryTag, false, "", ""),
                    AMQP.BasicProperties(),
                    message.toByteArray(StandardCharsets.UTF_8)
                )
                "consumerTag"
            }

            // when
            consumer.consumeChunks()

            // then
            verify {
                mockEncoder.encode(chunk.urlLocation, "${chunk.nodeName}-output.mkv", listOf("-c:v", "libx265"))
                mockChannel.basicAck(deliveryTag, false)
            }
        }

        @Test
        @DisplayName("should handle processing error with retries")
        fun handleProcessingErrorWithRetries() {
            // given
            val deliveryTag = 1L
            val chunk = Chunk("s3://bucket/video.mkv", "node1")
            val message = """
                {
                    "urlLocation": "${chunk.urlLocation}",
                    "nodeName": "${chunk.nodeName}"
                }
            """.trimIndent()

            every { mockEncoder.encode(any(), any(), any()) } throws RuntimeException("Encoding failed")
            // Redis mocking is handled in setUp()
            every { mockJedis.get("retry:$deliveryTag") } returnsMany listOf("0", "1", "2")

            // Capture the consumer to simulate message delivery
            val consumerSlot = slot<Consumer>()
            every { 
                mockChannel.basicConsume(any(), any(), capture(consumerSlot))
            } answers {
                // Simulate message delivery
                consumerSlot.captured.handleDelivery(
                    "consumerTag",
                    Envelope(deliveryTag, false, "", ""),
                    AMQP.BasicProperties(),
                    message.toByteArray(StandardCharsets.UTF_8)
                )
                "consumerTag"
            }

            // when
            consumer.consumeChunks()

            // then
            verify {
                mockJedis.get("retry:$deliveryTag")
                mockJedis.set("retry:$deliveryTag", "1")
                mockChannel.basicNack(deliveryTag, false, true)
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
            consumer.close()

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
                consumer.close()
            }
        }
    }
}
