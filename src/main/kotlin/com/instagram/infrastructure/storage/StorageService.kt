package com.instagram.infrastructure.storage

/**
 * StorageService abstraction — the infrastructure boundary for file storage.
 *
 * Implementations:
 * - [S3StorageService] for production (AWS S3)
 * - [LocalStorageService] for local development / tests
 *
 * Teaching note: By programming to this interface, UserService and PostService
 * never import AWS SDK classes. You can swap the implementation in Koin's DI
 * module without touching a single line of business logic.
 */
interface StorageService {
    /**
     * Uploads bytes to storage and returns the public URL.
     * @param key Unique path/key within the bucket (e.g. "posts/uuid/0")
     * @param bytes Raw file bytes
     * @param contentType MIME type (e.g. "image/jpeg")
     */
    suspend fun upload(key: String, bytes: ByteArray, contentType: String): String

    /** Deletes a file by key. No-op if the file doesn't exist. */
    suspend fun delete(key: String)
}
