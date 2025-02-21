package com.megamidnight.k8sencoder

import com.rabbitmq.client.Connection
import com.rabbitmq.client.Channel
import java.nio.charset.StandardCharsets

class RabbitMQProducer(
    private val connection: Connection,
    private val chunkQueue: SharedChunkQueue
) {
    private val channel: Channel = connection.createChannel()
    private val queueName = "chunks"

    init {
        // Declare the queue (create if doesn't exist)
        channel.queueDeclare(queueName, false, false, false, null)
    }

    fun sendChunks() {
        chunkQueue.getChunks().forEach { chunk ->
            val message = """
                {
                    "urlLocation": "${chunk.urlLocation}",
                    "nodeName": "${chunk.nodeName}"
                }
            """.trimIndent()
            
            channel.basicPublish(
                "", // default exchange
                queueName,
                null, // no message properties
                message.toByteArray(StandardCharsets.UTF_8)
            )
        }
    }

    fun close() {
        channel.close()
        connection.close()
    }
}