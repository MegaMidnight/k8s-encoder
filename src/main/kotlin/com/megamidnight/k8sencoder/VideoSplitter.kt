package com.megamidnight.k8sencoder

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

class VideoSplitter(
    private val s3Service: S3Service,
    private val s3Bucket: String
) {
    private val tempDir: Path = Files.createTempDirectory("video-splitter")

    fun splitVideo(s3VideoUrl: String, nodeCount: Int): List<String> {
        val videoFile = downloadVideo(s3VideoUrl)
        val duration = getVideoDuration(videoFile)
        val chunkDuration = duration / nodeCount
        val chunks = splitIntoChunks(videoFile, chunkDuration, nodeCount)
        return uploadChunks(chunks)
    }

    private fun downloadVideo(s3VideoUrl: String): File {
        val localFile = tempDir.resolve("input-${UUID.randomUUID()}.mkv").toFile()
        s3Service.downloadVideo(s3VideoUrl, localFile.toPath())
        return localFile
    }

    private fun getVideoDuration(videoFile: File): Long {
        val process = ProcessBuilder()
            .command("mkvmerge", "-i", videoFile.absolutePath)
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .start()

        val output = process.inputStream.bufferedReader().readText()
        // Extract duration in seconds from mkvmerge output
        val durationPattern = "Duration: (\\d+)".toRegex()
        return durationPattern.find(output)?.groupValues?.get(1)?.toLong()
            ?: throw IllegalStateException("Could not determine video duration")
    }

    private fun splitIntoChunks(videoFile: File, chunkDuration: Long, nodeCount: Int): List<File> {
        val chunks = mutableListOf<File>()

        for (i in 0 until nodeCount) {
            val startTime = i * chunkDuration
            val outputFile = tempDir.resolve("chunk-${i + 1}-${UUID.randomUUID()}.mkv").toFile()

            val process = ProcessBuilder()
                .command(
                    "mkvmerge",
                    "-o", outputFile.absolutePath,
                    "--split", "duration:${chunkDuration}s",
                    "--split-points", "${startTime}s",
                    videoFile.absolutePath
                )
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val error = process.errorStream.bufferedReader().readText()
                throw RuntimeException("Failed to split video: $error")
            }

            chunks.add(outputFile)
        }

        return chunks
    }

    private fun uploadChunks(chunks: List<File>): List<String> {
        return chunks.mapIndexed { index, chunk ->
            val s3Key = "chunks/chunk-${index + 1}-${UUID.randomUUID()}.mkv"
            s3Service.uploadChunk(chunk, s3Bucket, s3Key)
        }
    }

    fun cleanup() {
        tempDir.toFile().deleteRecursively()
    }
}
