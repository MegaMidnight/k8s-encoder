import java.util.concurrent.ConcurrentLinkedQueue

class SharedChunkQueue(chunks: List<Chunk>) {
    private val chunkQueue = ConcurrentLinkedQueue(chunks)

    fun getNextChunk(): Chunk? { // fighting the good fight against race conditions
        println("getNextChunk called. Number in chunkQueue: ${chunkQueue.size}.")
        return chunkQueue.poll()
    }

    fun chunkListIsEmpty(): Boolean {
        println("chunkListIsEmpty called. Number in chunkQueue: ${chunkQueue.size}.")
        return chunkQueue.isEmpty()
    }
}