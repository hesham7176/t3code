package com.t3code.explorer.data.network

import java.io.InputStream

sealed interface NetworkConnection {
    val id: String
    val displayName: String
    val endpoint: String
}

data class SmbConnection(override val id: String, override val displayName: String, override val endpoint: String) : NetworkConnection
data class FtpConnection(override val id: String, override val displayName: String, override val endpoint: String) : NetworkConnection
data class WebDavConnection(override val id: String, override val displayName: String, override val endpoint: String) : NetworkConnection

data class RemoteEntry(val name: String, val path: String, val isDirectory: Boolean, val size: Long, val modifiedAt: Long)

/** Provider boundary. Credentials belong in Android Keystore-backed storage, never in this model. */
interface RemoteFileSystem {
    val connection: NetworkConnection
    suspend fun list(path: String): Result<List<RemoteEntry>>
    suspend fun open(path: String): Result<InputStream>
    suspend fun upload(path: String, source: InputStream): Result<Unit>
    suspend fun delete(path: String): Result<Unit>
    suspend fun mkdir(path: String): Result<Unit>
}

class UnsupportedRemoteFileSystem(override val connection: NetworkConnection) : RemoteFileSystem {
    private fun <T> unsupported(): Result<T> = Result.failure(UnsupportedOperationException("No network provider configured"))
    override suspend fun list(path: String): Result<List<RemoteEntry>> = unsupported()
    override suspend fun open(path: String): Result<InputStream> = unsupported()
    override suspend fun upload(path: String, source: InputStream): Result<Unit> = unsupported()
    override suspend fun delete(path: String): Result<Unit> = unsupported()
    override suspend fun mkdir(path: String): Result<Unit> = unsupported()
}
