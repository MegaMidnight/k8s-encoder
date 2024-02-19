import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Connection

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
                    val message = chunk.urlLocation
                    val properties = AMQP.BasicProperties.Builder()
                        .contentType("text/plain")
                        .deliveryMode(2)
                        .headers(mapOf("chunkUrl" to message)) // bug fix possibly for duplicate messages
                        .build()
                    channel.basicPublish("", queueName, properties, message.toByteArray())
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