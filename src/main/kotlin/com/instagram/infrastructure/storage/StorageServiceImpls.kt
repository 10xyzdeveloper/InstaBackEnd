package com.instagram.infrastructure.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectRequest

/**
 * AWS S3 implementation of StorageService.
 *
 * Teaching notes:
 * - S3Client is thread-safe and should be a singleton (Koin `single {}`).
 * - `withContext(Dispatchers.IO)` offloads the blocking S3 SDK call to the IO thread pool,
 *   preventing it from blocking Ktor's coroutine dispatcher.
 * - The returned URL is the public HTTPS URL for the object. In production
 *   you'd use a CDN (CloudFront) in front of S3, but the URL structure is the same.
 */
class S3StorageService(
    private val bucket: String,
    private val region: String
) : StorageService {

    private val s3Client: S3Client = S3Client.builder()
        .region(Region.of(region))
        .build()

    override suspend fun upload(key: String, bytes: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            s3Client.putObject(
                PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .contentType(contentType)
                    .build(),
                RequestBody.fromBytes(bytes)
            )
            "https://$bucket.s3.$region.amazonaws.com/$key"
        }

    override suspend fun delete(key: String): Unit =
        withContext(Dispatchers.IO) {
            s3Client.deleteObject(
                DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()
            )
        }
}

/**
 * Local filesystem implementation for development without AWS credentials.
 *
 * Files are saved in /tmp/instagram-media/ and served via a static route.
 */
class LocalStorageService : StorageService {
    private val baseDir = java.io.File(System.getProperty("java.io.tmpdir"), "instagram-media")
        .also { it.mkdirs() }

    override suspend fun upload(key: String, bytes: ByteArray, contentType: String): String =
        withContext(Dispatchers.IO) {
            val file = java.io.File(baseDir, key.replace("/", "_"))
            file.writeBytes(bytes)
            "http://localhost:8080/media/${key.replace("/", "_")}"
        }

    override suspend fun delete(key: String): Unit =
        withContext(Dispatchers.IO) {
            java.io.File(baseDir, key.replace("/", "_")).delete()
        }
}
