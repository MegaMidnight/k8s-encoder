fun main() {
    val nodeChunkCreator = NodeChunkCreator()
    val nodes = nodeChunkCreator.createNodes()
    val chunks = nodeChunkCreator.createChunks(nodes)
    val chunkQueue = SharedChunkQueue(chunks)
    val config = Config(nodes, chunks)
    val connector = RabbitMQConnector()

    // Produce messages to RabbitMQ
    val producerConnection = connector.connect()
    val producer = RabbitMQProducer(producerConnection, chunkQueue)
    producer.sendChunks()
    producer.close()


    // Consume messages from RabbitMQ
    val consumerConnection = connector.connect()
    val consumer = RabbitMQConsumer(consumerConnection, config, connector)
    // Connect and clear the Redis db before consuming messages
    val redis = RedisConnector().connect()
    redis.use { jedis ->
        consumer.retryRedisCommand { jedis.del("messageIds") }
    }
    // Off she goes
    consumer.consumeChunks()
}
