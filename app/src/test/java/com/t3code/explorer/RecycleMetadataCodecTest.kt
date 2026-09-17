package com.t3code.explorer

import com.t3code.explorer.domain.util.RecycleMetadataCodec
import kotlin.test.Test
import kotlin.test.assertEquals

class RecycleMetadataCodecTest {
    @Test fun `paths with separators and unicode round trip`() {
        val path = "/storage/emulated/0/كتب/episode=01.mp4"
        assertEquals(path, RecycleMetadataCodec.decode(RecycleMetadataCodec.encode(path)))
    }

    @Test fun `legacy plain metadata remains readable`() {
        assertEquals("/tmp/file.txt", RecycleMetadataCodec.decode("/tmp/file.txt"))
    }
}
