package com.t3code.explorer.data.cloud

import java.io.InputStream

interface CloudProvider {
    val id: String
    val name: String
    suspend fun list(parentId: String?): Result<List<CloudEntry>>
    suspend fun download(id: String): Result<InputStream>
    suspend fun upload(parentId: String?, name: String, source: InputStream): Result<CloudEntry>
    suspend fun delete(id: String): Result<Unit>
}

data class CloudEntry(val id: String, val name: String, val isDirectory: Boolean, val size: Long, val modifiedAt: Long)

/** OAuth provider implementations can be added without changing the local browser. */
class NotConfiguredCloudProvider(override val id: String, override val name: String) : CloudProvider {
    private fun <T> unsupported(): Result<T> = Result.failure(UnsupportedOperationException("$name is not configured"))
    override suspend fun list(parentId: String?) = unsupported()
    override suspend fun download(id: String) = unsupported()
    override suspend fun upload(parentId: String?, name: String, source: InputStream) = unsupported()
    override suspend fun delete(id: String) = unsupported()
}
