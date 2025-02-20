import java.util.concurrent.ConcurrentLinkedQueue
import com.megamidnight.k8sencoder.Chunk

class SharedChunkQueue(chunks: List<Chunk>) {
    private val chunkQueue = ConcurrentLinkedQueue(chunks)

    fun getNextChunk(): Chunk? { // fighting the good fight against race conditions
        return chunkQueue.poll()
    }

    fun chunkListIsEmpty(): Boolean {
        return chunkQueue.isEmpty()
    }
}
