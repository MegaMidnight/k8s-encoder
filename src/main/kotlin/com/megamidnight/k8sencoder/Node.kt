package com.megamidnight.k8sencoder

/**
 * Represents a node in the distributed video encoding system
 *
 * @property name The name of the node
 * @property chunk The chunk identifier this node is processing
 */
data class Node(
    val name: String,
    val chunk: String
)