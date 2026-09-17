package com.t3code.explorer

import com.t3code.explorer.data.downloads.DownloadPolicy
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DownloadPolicyTest {
    @Test fun `only HTTP and HTTPS are accepted`() {
        DownloadPolicy.requireSupported(URL("https://example.test/file"))
        DownloadPolicy.requireSupported(URL("http://example.test/file"))
        assertFailsWith<IllegalArgumentException> { DownloadPolicy.requireSupported(URL("ftp://example.test/file")) }
    }

    @Test fun `resume appends only after a partial response`() {
        assertTrue(DownloadPolicy.shouldAppend(10, HttpURLConnection.HTTP_PARTIAL))
        assertFalse(DownloadPolicy.shouldAppend(10, HttpURLConnection.HTTP_OK))
        assertEquals(110L, DownloadPolicy.totalBytes(10, 100, true))
        assertEquals(100L, DownloadPolicy.totalBytes(0, 100, false))
    }
}
