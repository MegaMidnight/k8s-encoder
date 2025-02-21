package com.megamidnight.k8sencoder

class SharedChunkQueue(private val chunks: List<Chunk>) {
    private var currentIndex = 0
    private val lock = Any()

    fun getChunks(): List<Chunk> = chunks

    fun getNextChunk(): Chunk? = synchronized(lock) {
        if (currentIndex < chunks.size) {
            chunks[currentIndex++]
        } else {
            null
        }
    }

    fun hasMoreChunks(): Boolean = synchronized(lock) {
        currentIndex < chunks.size
    }
}