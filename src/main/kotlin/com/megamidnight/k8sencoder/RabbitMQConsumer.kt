package com.megamidnight.k8sencoder

import com.rabbitmq.client.*
import redis.clients.jedis.Jedis
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class RabbitMQConsumer(
    private val connection: Connection,
    private val encoder: FFmpegEncoder = FFmpegEncoder("/usr/bin/ffmpeg")
) {
    private val channel: Channel = connection.createChannel()
    private val queueName = "chunks"
    private val maxRetries = 3

    init {
        channel.queueDeclare(queueName, false, false, false, null)
    }

    fun consumeChunks() {
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                val messageStr = String(body, StandardCharsets.UTF_8)
                val json = JSONObject(messageStr)
                val chunk = Chunk(
                    json.getString("urlLocation"),
                    json.getString("nodeName")
                )

                try {
                    processChunk(chunk)
                    channel.basicAck(envelope.deliveryTag, false)
                } catch (e: Exception) {
                    handleProcessingError(envelope.deliveryTag, e)
                }
            }
        }

        channel.basicConsume(queueName, false, consumer)
    }

    private fun processChunk(chunk: Chunk) {
        val outputFile = "${chunk.nodeName}-output.mkv"
        encoder.encode(chunk.urlLocation, outputFile, listOf("-c:v", "libx265"))
    }

    private fun handleProcessingError(deliveryTag: Long, error: Exception) {
        Jedis(
            System.getenv("REDIS_HOST") ?: "localhost",
            (System.getenv("REDIS_PORT") ?: "6379").toInt()
        ).use { jedis ->
            val retryCount = jedis.get("retry:$deliveryTag")?.let { count ->
                if (count.isBlank()) 0 else count.toInt()
            } ?: 0
            if (retryCount < maxRetries) {
                jedis.set("retry:$deliveryTag", (retryCount + 1).toString())
                channel.basicNack(deliveryTag, false, true)
            } else {
                jedis.del("retry:$deliveryTag")
                channel.basicNack(deliveryTag, false, false)
                throw RuntimeException("Failed to process chunk after $maxRetries retries", error)
            }
        }
    }

    fun close() {
        channel.close()
        connection.close()
    }
}
