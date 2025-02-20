package com.megamidnight.k8sencoder

/**
 * Configuration for the distributed video encoding system
 *
 * @property nodes List of nodes participating in the encoding process
 * @property chunks List of video chunks to be processed
 */
data class Config(
    val nodes: List<Node>,
    val chunks: List<Chunk>
) {
    companion object {
        /**
         * Creates a new Config instance with defensive copies of the input lists
         *
         * @param nodes List of nodes participating in the encoding process
         * @param chunks List of video chunks to be processed
         * @return A new Config instance with immutable lists
         */
        fun create(nodes: List<Node>, chunks: List<Chunk>): Config {
            return Config(nodes.toList(), chunks.toList())
        }
    }
}
