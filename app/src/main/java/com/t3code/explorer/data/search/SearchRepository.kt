package com.t3code.explorer.data.search

import com.t3code.explorer.data.files.FileRepository
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.SearchFilters
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SearchRepository(private val files: FileRepository) {
    fun search(root: String, filters: SearchFilters, showHidden: Boolean): Flow<Result<List<FileItem>>> = flow {
        emit(files.search(root, filters, showHidden))
    }
}
