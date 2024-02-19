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
    consumer.consumeChunks()
}
