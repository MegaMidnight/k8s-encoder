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
        println("sendChunks called")
        val channel = connection.createChannel()

        try {
            while (!chunkQueue.chunkListIsEmpty()) {
                var chunk: Chunk? = chunkQueue.getNextChunk()
                while (chunk == null) {
                    println("No chunk available, retrying...")
                    Thread.sleep(1000) // Wait for 1 second before retrying
                    chunk = chunkQueue.getNextChunk()
                }

                val message = chunk.urlLocation
                channel.basicPublish("", queueName, null, message.toByteArray())
                println(" [v] Sent '$message'")
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