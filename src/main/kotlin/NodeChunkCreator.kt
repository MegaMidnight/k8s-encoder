class NodeChunkCreator {
    private val nodeCount = KubernetesService().getNodeCount()
    private val inputFileUrls = VideoChunkSplitter().splitFile(System.getenv("INPUT_FILE")
        ?: throw IllegalArgumentException("INPUT_FILE is not set"), nodeCount)

    init {
        if (nodeCount != inputFileUrls.size) {
            throw IllegalArgumentException("Mismatch in the number of nodes and number of chunks.")
        }
    }

    fun createNodes(): List<Node> {
        val nodeNames = List(nodeCount) { "node${it + 1}" } // Generate node names as node1, node2, ..., nodeCount
        val nodeChunkMapping = nodeNames.zip(inputFileUrls).toMap() // This ensures that each node is associated with a unique chunk
        return nodeChunkMapping.map { Node(it.key, it.value) }
    }

    fun createChunks(nodes: List<Node>): List<Chunk> {
        return nodes.map { Chunk(it.chunk, it.name) } // this ensures that each chunk is associated with a unique node
    }
}