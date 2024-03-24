import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Delivery
import java.io.File
import java.io.IOException

class RabbitMQConsumer(private var connection: Connection, private val config: Config, private val connector: RabbitMQConnector) {

    private val redisCommandsExecutor = RedisCommandsExecutor()
    fun consumeChunks() {
        val autoAck = false
        var outputFileName: String?
        val currentDeliveryTag: Long? = null
        val jedis = RedisConnector().connect()


        try {
            val channel = connection.createChannel()
            channel.basicQos(1) // Process only one message at a time
            val chunkQueue = System.getenv("CHUNK_QUEUE") ?: throw IllegalArgumentException("CHUNK_QUEUE is not set")
            val ffmpegPath = System.getenv("FFMPEG_PATH") ?: throw IllegalArgumentException("FFMPEG_PATH is not set")
            val encoder = FFmpegEncoder(ffmpegPath)
            val videoFileHandler = VideoFileHandler()
            val ffmpegParameters = System.getenv("FFMPEG_PARAMETERS") ?: throw IllegalArgumentException("FFMPEG_PARAMETERS is not set")
            val ffmpegParamsAsAList = ffmpegParameters.split(" ").toList()

            val deliverCallback = fun(_: String, delivery: Delivery) {
                try {
                    val message = String(delivery.body, Charsets.UTF_8)
                    val parts = message.splitToSequence(",")
                        .toList() // This bug was an all-nighter to fix duplicate messages
                    if (parts.size == 2) {
                        val chunkUrl = parts[0]
                        val messageId = parts[1]
                        val chunk = config.chunks.find { it.urlLocation == chunkUrl }
                        if (chunk != null && !retryRedisCommand { jedis.sismember("messageIds", messageId) } ){
                            retryRedisCommand { jedis.sadd("messageIds", messageId) }
                            val node = config.nodes.find { it.name == chunk.nodeName }
                            if (node != null ) {
                                val inputFile = node.chunk
                                val downloadedFile = videoFileHandler.downloadFile(chunkUrl)
                                channel.basicAck(delivery.envelope.deliveryTag, true)
                                println("Message acknowledged: ${String(delivery.body)}")
                                outputFileName = inputFile.substringAfterLast("/").substringBeforeLast(".mkv") + "-encode.mkv"
                                println("Encoding file...$outputFileName")
                                try {
                                    encoder.encode(downloadedFile, outputFileName!!, ffmpegParamsAsAList)
                                } catch (e: Exception) {
                                    println("An error occurred during the encoding process: ${e.message}")
                                    e.printStackTrace()
                                } finally {
                                    File(downloadedFile).delete()
                                }
                                println("Encoding complete: $outputFileName")
                                videoFileHandler.uploadFile(outputFileName!!, "encodes/$outputFileName")
                                Thread.sleep(1000)
                            } else {
                                println("Node not found for chunk: $message")
                            }
                        } else {
                            println("Chunk not found or message already processed: $message")
                        }
                    } else {
                        println("Invalid message format: $message")
                    }
                } catch (e: Exception) {
                    println("An error occurred during the processing of the message: ${e.message}")
                    e.printStackTrace() // Log the stack trace of the exception

                    // Log the message that caused the exception
                    val message = String(delivery.body)
                    println("Message that caused the exception: $message")

                    // Reject the message and requeue it
                    currentDeliveryTag?.let { deliveryTag ->
                        channel.basicNack(deliveryTag, false, true)
                    }
                }
            }

            val cancelCallback: (String) -> Unit = { consumerTag ->
                println("Consumer $consumerTag has been cancelled")

                currentDeliveryTag?.let { deliveryTag ->
                    // Reject the message and requeue it
                    channel.basicNack(deliveryTag, false, true)
                }
            }

            channel.basicConsume(chunkQueue, autoAck, deliverCallback, cancelCallback)

        } catch (e: IOException) {
            println("An error occurred during the processing of the message: ${e.message}")

            if (e.cause is AlreadyClosedException) {
                println("Connection to RabbitMQ was closed. Attempting to reconnect...")

                // Attempt to reconnect to RabbitMQ
                var attempts = 0
                while (attempts < 57) { // this is plenty of time to wait for the RabbitMQ to come back up
                    try {
                        connection = connector.connect()
                        println("Reconnected to RabbitMQ")
                        break
                    } catch (e: Exception) {
                        println("Failed to reconnect to RabbitMQ. Retrying in 6.8 seconds...")
                        Thread.sleep(6800) // Wait for 6.8 seconds before retrying to connect
                        attempts++
                    }
                }

                if (attempts == 57) {
                    throw IOException("Failed to reconnect to RabbitMQ after 57 attempts", e)
                }

                // After reconnecting, resume consuming messages
                return consumeChunks()
            }
        } catch (e: Exception) {
            println("An error occurred during the processing of the message: ${e.message}")
            e.printStackTrace()
        }

    }

    fun <T> retryRedisCommand(command: () -> T): T {
        return redisCommandsExecutor.retryRedisCommand(command)
    }

}