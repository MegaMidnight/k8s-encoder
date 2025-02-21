package com.megamidnight.k8sencoder

import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

class FFmpegEncoder(private val ffmpegPath: String) {
    private val spacesBucket = System.getenv("SPACES_BUCKET") ?: throw IllegalArgumentException("SPACES_BUCKET is not set")

    fun encode(inputFileUrl: String, outputFile: String, params: List<String>) {
        try {
            val inputFile = inputFileUrl.substringAfterLast("/")
            val command = buildCommand(inputFile, outputFile, params)
            val process: Process? = ProcessBuilder(command).inheritIO().start()
            process?.waitFor()

            val exitCode = process?.exitValue()
            if (exitCode == 0) {
                println("Encoding completed")
                if (Files.exists(Paths.get(outputFile))) {
                    try {
                        uploadFile(outputFile, "encodes/$outputFile")
                        println("Upload completed")
                    } catch (e: Exception) {
                        println("Upload failed: ${e.message}")
                        throw e
                    }
                } else {
                    println("Output file not found")
                    throw RuntimeException("Output file not found: $outputFile")
                }
            } else {
                println("Encoding failed with exit code: $exitCode")
                throw RuntimeException("FFmpeg encoding failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            println("Error during encoding: ${e.message}")
            throw e
        }
    }

    private fun buildCommand(inputFile: String, outputFile: String, params: List<String>): List<String> {
        return listOf(ffmpegPath, "-i", inputFile) + params + listOf(outputFile)
    }

    private fun uploadFile(localFile: String, s3Key: String) {
        val s3Service = S3Service()
        s3Service.uploadChunk(File(localFile), spacesBucket, s3Key)
    }
}
