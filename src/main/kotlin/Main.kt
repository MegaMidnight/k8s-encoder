data class Node(val name: String, val chunk: String)

data class Config(val nodes: List<Node>, val chunks: List<Chunk>)

data class Chunk(val urlLocation: String, val nodeName: String) // Add a nodeName property to the Chunk class
fun main() {
    val nodeCount = System.getenv("NODE_COUNT")?.toInt() ?: throw IllegalArgumentException("NODE_COUNT is not set")
    val inputFileUrls = System.getenv("INPUT_FILE_URL")?.split(",") ?: throw IllegalArgumentException("INPUT_FILE_URL is not set")

    if (nodeCount != inputFileUrls.size) {
        println("Mismatch in the number of nodes and number of chunks.")
        return
    }

    val nodeNames = List(nodeCount) { "node${it + 1}" } // Generate node names as node1, node2, ..., node8

    val nodeChunkMapping = nodeNames.zip(inputFileUrls).toMap() // This ensures that each node is associated with a unique chunk

    val nodes = nodeChunkMapping.map { Node(it.key, it.value) }
    val chunks = nodes.map { Chunk(it.chunk, it.name) } // Create a Chunk object for each chunk URL
    val chunkQueue = SharedChunkQueue(chunks)
    val config = Config(nodes, chunks)
    val connector = RabbitMQConnector()
    val connection = connector.connect()

    // Produce messages to RabbitMQ
    val producer = RabbitMQProducer(connection, chunkQueue)
    producer.sendChunks()

    // Consume messages from RabbitMQ
    val consumer = RabbitMQConsumer(connection, config, connector)
    consumer.consumeMessages()

    producer.close()
}