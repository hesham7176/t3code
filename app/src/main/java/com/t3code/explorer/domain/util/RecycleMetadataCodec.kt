package com.t3code.explorer.domain.util

import java.util.Base64

object RecycleMetadataCodec {
    fun encode(originalPath: String): String = Base64.getEncoder().encodeToString(originalPath.toByteArray(Charsets.UTF_8))

    fun decode(value: String): String = runCatching {
        String(Base64.getDecoder().decode(value), Charsets.UTF_8)
    }.getOrDefault(value)
}
