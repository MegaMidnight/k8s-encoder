package com.megamidnight.k8sencoder

class NodeChunkCreator(
    s3Service: S3Service = S3Service(),
    videoSplitter: VideoSplitter? = null
) {
    private val nodeCount = (System.getenv("NODE_COUNT") ?: System.getProperty("NODE_COUNT"))?.toInt() 
        ?: throw IllegalArgumentException("NODE_COUNT is not set")
    private val inputVideoUrl = System.getenv("INPUT_VIDEO_URL") ?: System.getProperty("INPUT_VIDEO_URL")
        ?: throw IllegalArgumentException("INPUT_VIDEO_URL is not set")
    private val s3Bucket = System.getenv("S3_BUCKET") ?: System.getProperty("S3_BUCKET")
        ?: throw IllegalArgumentException("S3_BUCKET is not set")

    private val splitter = videoSplitter ?: VideoSplitter(s3Service, s3Bucket)
    private var chunkUrls: List<String>? = null

    fun createNodes(): List<Node> {
        // Split video and get chunk URLs if not already done
        if (chunkUrls == null) {
            try {
                chunkUrls = splitter.splitVideo(inputVideoUrl, nodeCount)
            } finally {
                splitter.cleanup()
            }
        }

        return List(nodeCount) { index ->
            Node("node${index + 1}", chunkUrls!![index])
        }
    }

    fun createChunks(nodes: List<Node>): List<Chunk> {
        return nodes.map { Chunk(it.chunk, it.name) }
    }
}
