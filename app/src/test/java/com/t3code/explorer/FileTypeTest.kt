package com.t3code.explorer

import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.util.FileType
import kotlin.test.Test
import kotlin.test.assertEquals

class FileTypeTest {
    @Test fun `common media types are classified`() {
        assertEquals(FileCategory.VIDEO, FileType.category("movie.MKV"))
        assertEquals(FileCategory.IMAGE, FileType.category("cover.webp"))
        assertEquals(FileCategory.AUDIO, FileType.category("song.flac"))
        assertEquals(FileCategory.ARCHIVE, FileType.category("backup.zip"))
    }
}
