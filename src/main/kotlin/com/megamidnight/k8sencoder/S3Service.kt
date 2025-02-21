package com.megamidnight.k8sencoder

import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import java.io.File
import java.nio.file.Path

class S3Service(private val s3Client: S3Client = createDefaultS3Client()) {
    companion object {
        private fun createDefaultS3Client(): S3Client {
            val spacesEndpoint = System.getenv("SPACES_ENDPOINT") ?: throw IllegalArgumentException("SPACES_ENDPOINT is not set")
            val spacesAccessKey = System.getenv("SPACES_ACCESS_KEY") ?: throw IllegalArgumentException("SPACES_ACCESS_KEY is not set")
            val spacesSecretKey = System.getenv("SPACES_SECRET_KEY") ?: throw IllegalArgumentException("SPACES_SECRET_KEY is not set")

            return S3Client.builder()
                .endpointOverride(java.net.URI.create(spacesEndpoint))
                .credentialsProvider {
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(spacesAccessKey, spacesSecretKey)
                }
                .build()
        }
    }

    fun downloadVideo(s3Url: String, localPath: Path) {
        val (bucket, key) = parseS3Url(s3Url)
        val request = GetObjectRequest.builder()
            .bucket(bucket)
            .key(key)
            .build()

        s3Client.getObject(request, localPath)
    }

    fun uploadChunk(localFile: File, s3Bucket: String, s3Key: String): String {
        try {
            if (!localFile.exists()) {
                throw IllegalArgumentException("Local file does not exist: ${localFile.absolutePath}")
            }

            val request = PutObjectRequest.builder()
                .bucket(s3Bucket)
                .key(s3Key)
                .build()

            s3Client.putObject(request, RequestBody.fromFile(localFile))
            return "s3://$s3Bucket/$s3Key"
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw e
            }
        }
    }

    internal fun parseS3Url(s3Url: String): Pair<String, String> {
        try {
            if (!s3Url.startsWith("s3://")) {
                throw IllegalArgumentException("Invalid S3 URL format. Must start with 's3://'")
            }

            val parts = s3Url.removePrefix("s3://").split("/", limit = 2)
            if (parts.size != 2 || parts[0].isEmpty() || parts[1].isEmpty()) {
                throw IllegalArgumentException("Invalid S3 URL format. Must be in format 's3://bucket/key'")
            }

            return Pair(parts[0], parts[1])
        } catch (e: Exception) {
            when (e) {
                is IllegalArgumentException -> throw e
                else -> throw IllegalArgumentException("Invalid S3 URL format: ${e.message}")
            }
        }
    }
}
