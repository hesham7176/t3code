package com.t3code.explorer

import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SortDirection
import com.t3code.explorer.domain.model.SortField
import com.t3code.explorer.domain.model.SortSpec
import com.t3code.explorer.domain.util.sortedBySpec
import kotlin.test.Test
import kotlin.test.assertEquals

class FileSortTest {
    private val folder = FileItem("folder", "/folder", isDirectory = true, size = 0, modifiedAt = 1, mimeType = "inode/directory", extension = "")
    private val b = FileItem("b.mp4", "/b.mp4", isDirectory = false, size = 200, modifiedAt = 3, mimeType = "video/mp4", extension = "mp4")
    private val a = FileItem("a.txt", "/a.txt", isDirectory = false, size = 100, modifiedAt = 2, mimeType = "text/plain", extension = "txt")

    @Test fun `directories precede files and name sorting is case insensitive`() {
        assertEquals(listOf(folder, a, b), listOf(b, a, folder).sortedBySpec(SortSpec()))
    }

    @Test fun `descending size is supported`() {
        assertEquals(listOf(folder, b, a), listOf(folder, a, b).sortedBySpec(SortSpec(SortField.SIZE, SortDirection.DESCENDING)))
    }
}
