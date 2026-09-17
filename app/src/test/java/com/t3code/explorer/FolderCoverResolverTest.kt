package com.t3code.explorer

import com.t3code.explorer.domain.util.FolderCoverResolver
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class FolderCoverResolverTest {
    @Test fun `cover wins over poster and folder`() {
        val dir = Files.createTempDirectory("covers").toFile()
        dir.resolve("poster.png").writeText("")
        dir.resolve("folder.jpg").writeText("")
        dir.resolve("cover.webp").writeText("")
        assertEquals("cover.webp", FolderCoverResolver().resolve(dir)?.name)
        dir.deleteRecursively()
    }

    @Test fun `fallback chooses an image alphabetically`() {
        val dir = Files.createTempDirectory("covers").toFile()
        dir.resolve("z.png").writeText("")
        dir.resolve("a.jpg").writeText("")
        assertEquals("a.jpg", FolderCoverResolver().resolve(dir)?.name)
        dir.deleteRecursively()
    }

    @Test fun `unsupported image extension is not used as folder cover`() {
        val dir = Files.createTempDirectory("covers").toFile()
        dir.resolve("animated.gif").writeText("")
        assertEquals(null, FolderCoverResolver().resolve(dir))
        dir.deleteRecursively()
    }
}
