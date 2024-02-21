import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection
import java.util.*

class RabbitMQProducer(private val connection: Connection, private val chunkQueue: SharedChunkQueue) {
    private val queueName: String = System.getenv("CHUNK_QUEUE") ?: throw IllegalArgumentException("CHUNK_QUEUE is not set")

    init {
        if (connection.isOpen) {
            println("Successfully connected to RabbitMQ.")
        } else {
            println("Failed to connect to RabbitMQ.")
        }
    }

    fun sendChunks() {
        val channel = connection.createChannel()

        try {
            while (!chunkQueue.chunkListIsEmpty()) {
                val chunk = chunkQueue.getNextChunk()
                if (chunk != null) {
                    val messageId = UUID.randomUUID().toString()
                    val message = "${chunk.urlLocation},$messageId"
                    val properties = AMQP.BasicProperties.Builder()
                        .contentType("text/plain")
                        .deliveryMode(2)
                        .correlationId(messageId) // The savior of duplicate messages
                        .build()
                    channel.basicPublish("", queueName, properties, message.toByteArray(Charsets.UTF_8)) // Publish directly to the queue
                    println(" [v] Sent '$message'")
                }
            }
            println("All messages sent successfully.")
        } catch (e: Exception) {
            println("An error occurred while sending chunks: ${e.message}")
            e.printStackTrace()
        }
    }

    fun close() {
        connection.close()
        println("Connection to RabbitMQ closed.")
    }
}