package com.megamidnight.k8sencoder

/**
 * Represents a video chunk in the distributed encoding system
 *
 * @property urlLocation The URL location where the chunk is stored
 * @property nodeName The name of the node processing this chunk
 */
data class Chunk(
    val urlLocation: String,
    val nodeName: String
)