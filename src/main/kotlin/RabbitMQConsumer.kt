import com.rabbitmq.client.AlreadyClosedException
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Delivery
import redis.clients.jedis.exceptions.JedisConnectionException
import java.io.File
import java.io.IOException
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class RabbitMQConsumer(private var connection: Connection, private val config: Config, private val connector: RabbitMQConnector) {
    fun consumeChunks() {
        println("Consuming messages called")
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
            val ffmpegParameters = listOf(
                "-max_muxing_queue_size", "1024",
                "-map", "0:0",
                "-c:v", "libx265",
                "-pix_fmt", "yuv420p10le",
                "-profile:v", "main10",
                "-x265-params", "aq-mode=0:repeat-headers=1:no-strong-intra-smoothing=1:bframes=1:b-adapt=0:frame-threads=0:colorprim=bt2020:transfer=smpte2084:colormatrix=bt2020nc:hdr10_opt=1:master-display=G(13250,34500)B(7500,3000)R(34000,16000)WP(15635,16450)L(40000000,50):max-cll=0,0:hdr10=1:chromaloc=2",
                "-crf:v", "30",
                "-preset:v", "ultrafast",
                "-map_metadata", "-1",
                "-map_chapters", "0",
                "-default_mode", "infer_no_subs"
            )

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
                                val inputFileUrl = node.chunk
                                val downloadedFile = downloadFile(inputFileUrl)
                                channel.basicAck(delivery.envelope.deliveryTag, true)
                                println("Message acknowledged: ${String(delivery.body)}")
                                outputFileName = inputFileUrl.substringAfterLast("/").substringBeforeLast(".mkv") + "-encode.mkv"
                                println("Encoding file...$outputFileName")
                                try {
                                    encoder.encode(downloadedFile, outputFileName!!, ffmpegParameters)
                                } catch (e: Exception) {
                                    println("An error occurred during the encoding process: ${e.message}")
                                    e.printStackTrace()
                                } finally {
                                    File(downloadedFile).delete()
                                }
                                println("Encoding complete: $outputFileName")
                                encoder.uploadFile(outputFileName!!, "encodes/$outputFileName")
                                jedis.close()
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

                // If all reconnection attempts fail, throw an exception
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
        val maxAttempts = 4
        (1..maxAttempts).forEach { attempt ->
            try {
                return command() // Execute the Redis command
            } catch (e: JedisConnectionException) {
                if (attempt == maxAttempts) throw e
                println("Failed to execute Redis operation. Attempt $attempt/$maxAttempts failed. Retrying...")
                Thread.sleep(1500) // Wait for 1.5 seconds before retrying
            } catch (e: Exception) {
                println("An error occurred during the Redis operation: ${e.message}")
            }
        }
        throw RuntimeException("Failed to execute Redis operation after $maxAttempts attempts")
    }

    private fun downloadFile(fileUrl: String): String {
        println("Downloading file: $fileUrl")
        val url = URI(fileUrl).toURL()
        val fileName = url.file.split("/").last()
        val destination = File(fileName).toPath()

        val future = CompletableFuture.runAsync {
            url.openStream().use { inputStream ->
                Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING)
            }
        }

        try {
            future.get(20, TimeUnit.MINUTES) // Wait for 20 minutes
            println("Download complete: $fileName") // Debug print statement
        } catch (e: TimeoutException) {
            println("Download did not complete within the expected time")
        } catch (e: InterruptedException) {
            println("Download was interrupted")
        } catch (e: ExecutionException) {
            println("An error occurred during the download: ${e.message}")
        } finally {
            future.cancel(true)
        }
        return fileName
    }
}