package com.t3code.explorer.ui

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3code.explorer.ExplorerApplication
import com.t3code.explorer.R
import com.t3code.explorer.data.analyzer.StorageAnalyzer
import com.t3code.explorer.data.preferences.UserPreferences
import com.t3code.explorer.domain.model.FileCategory
import com.t3code.explorer.domain.model.FileItem
import com.t3code.explorer.domain.model.RecycleEntry
import com.t3code.explorer.domain.model.SearchFilters
import com.t3code.explorer.domain.model.SortDirection
import com.t3code.explorer.domain.model.SortField
import com.t3code.explorer.domain.model.SortSpec
import com.t3code.explorer.domain.model.StorageAnalysis
import com.t3code.explorer.domain.model.StorageLocation
import com.t3code.explorer.domain.model.ViewMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ExplorerApplication
    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()
    private val safParents = mutableMapOf<String, String>()
    private var directoryJob: Job? = null
    private var searchJob: Job? = null
    private var analysisJob: Job? = null

    init {
        val primary = app.storage.primary()?.path ?: application.filesDir.absolutePath
        _uiState.update { it.copy(currentPath = primary, locations = app.storage.locations()) }
        viewModelScope.launch {
            app.preferences.preferences.collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
                if (preferences.language != "system" && AppCompatDelegate.getApplicationLocales().toLanguageTags() != preferences.language) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(preferences.language))
                loadDirectory(_uiState.value.currentPath)
            }
        }
        viewModelScope.launch { app.operations.progress.collect { value -> _uiState.update { it.copy(operation = value) } } }
    }

    fun loadDirectory(path: String = _uiState.value.currentPath) {
        val previous = _uiState.value.currentPath
        if (path.startsWith("content:") && previous.startsWith("content:") && path != previous) safParents[path] = previous
        _uiState.update { it.copy(currentPath = path, isLoading = true, error = null, selected = emptySet()) }
        directoryJob?.cancel()
        directoryJob = viewModelScope.launch {
            val result = if (path.startsWith("content:")) app.files.listSafTree(Uri.parse(path), _uiState.value.preferences.showHidden) else app.files.list(path, _uiState.value.preferences.toSortSpec(), _uiState.value.preferences.showHidden)
            result.fold(
                onSuccess = { items -> _uiState.update { it.copy(items = items, isLoading = false) } },
                onFailure = { error -> _uiState.update { it.copy(items = emptyList(), isLoading = false, error = error.message ?: app.getString(R.string.operation_failed)) } }
            )
        }
    }

    fun openSafTree(uri: Uri) {
        val persisted = runCatching {
            app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.isSuccess
        if (!persisted) showMessage(R.string.saf_permission_not_persisted)
        loadDirectory(uri.toString())
    }

    fun loadCategory(category: FileCategory) {
        val root = app.storage.primary()?.path ?: return
        _uiState.update { it.copy(currentPath = root, isLoading = true, error = null, selected = emptySet()) }
        directoryJob?.cancel()
        directoryJob = viewModelScope.launch {
            val result = app.files.search(root, SearchFilters("", category = category), _uiState.value.preferences.showHidden)
            result.fold(
                onSuccess = { items -> _uiState.update { it.copy(items = items, isLoading = false) } },
                onFailure = { error -> _uiState.update { it.copy(items = emptyList(), isLoading = false, error = error.message ?: app.getString(R.string.operation_failed)) } }
            )
        }
    }

    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.startsWith("content:")) {
            safParents[currentPath]?.let { loadDirectory(it) }
        } else {
            java.io.File(currentPath).parentFile?.let { loadDirectory(it.absolutePath) }
        }
    }

    fun toggleSelection(item: FileItem) = _uiState.update {
        it.copy(selected = if (item.path in it.selected) it.selected - item.path else it.selected + item.path)
    }

    fun selectAll() = _uiState.update { state -> state.copy(selected = state.items.map { it.path }.toSet()) }
    fun clearSelection() = _uiState.update { it.copy(selected = emptySet()) }

    fun setViewMode(value: ViewMode) { viewModelScope.launch { app.preferences.setViewMode(value) } }
    fun setSort(field: SortField, direction: SortDirection) { viewModelScope.launch { app.preferences.setSort(field, direction); loadDirectory() } }
    fun setShowHidden(value: Boolean) { viewModelScope.launch { app.preferences.setShowHidden(value) } }
    fun setFolderCovers(value: Boolean) { viewModelScope.launch { app.preferences.setFolderCovers(value) } }
    fun setResumePlayback(value: Boolean) { viewModelScope.launch { app.preferences.setResumePlayback(value) } }
    fun setBackgroundPlayback(value: Boolean) { viewModelScope.launch { app.preferences.setBackgroundPlayback(value) } }
    fun setTheme(value: String) { viewModelScope.launch { app.preferences.setTheme(value) } }
    fun setLanguage(value: String) {
        viewModelScope.launch { app.preferences.setLanguage(value) }
        AppCompatDelegate.setApplicationLocales(if (value == "system") LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(value))
    }

    fun createFolder(name: String) = viewModelScope.launch {
        if (isSafPath()) { showMessage(R.string.saf_operation_unavailable); return@launch }
        app.operations.createFolder(java.io.File(_uiState.value.currentPath), name).fold({ showMessage(R.string.folder_created) }, ::showError)
        loadDirectory()
    }

    fun createFile(name: String) = viewModelScope.launch {
        if (isSafPath()) { showMessage(R.string.saf_operation_unavailable); return@launch }
        app.operations.createFile(java.io.File(_uiState.value.currentPath), name).fold({ showMessage(R.string.file_created) }, ::showError)
        loadDirectory()
    }

    fun rename(item: FileItem, name: String) = viewModelScope.launch {
        if (item.path.startsWith("content:")) { showMessage(R.string.saf_operation_unavailable); return@launch }
        app.operations.rename(item, name).fold({ showMessage(R.string.renamed) }, ::showError)
        loadDirectory()
    }

    fun transferSelected(destination: String, move: Boolean) = viewModelScope.launch {
        if (isSafPath()) { showMessage(R.string.saf_operation_unavailable); return@launch }
        val items = _uiState.value.items.filter { it.path in _uiState.value.selected }
        if (items.any { it.path.startsWith("content:") }) { showMessage(R.string.saf_operation_unavailable); return@launch }
        val result = if (move) app.operations.move(items, java.io.File(destination)) else app.operations.copy(items, java.io.File(destination))
        result.fold({ showMessage(if (move) R.string.moved else R.string.copied) }, ::showError)
        clearSelection()
        loadDirectory()
    }

    fun deleteSelected() = viewModelScope.launch {
        if (isSafPath()) { showMessage(R.string.saf_operation_unavailable); return@launch }
        val items = _uiState.value.items.filter { it.path in _uiState.value.selected }
        if (items.any { it.path.startsWith("content:") }) { showMessage(R.string.saf_operation_unavailable); return@launch }
        app.operations.deleteToRecycleBin(items).fold({ showMessage(R.string.moved_to_recycle_bin) }, ::showError)
        clearSelection()
        loadDirectory()
    }

    fun share(item: FileItem) { _uiState.update { it.copy(shareIntent = app.operations.shareIntent(item)) } }
    fun consumeShareIntent() { _uiState.update { it.copy(shareIntent = null) } }

    fun search(query: String) {
        searchJob?.cancel()
        if (_uiState.value.currentPath.startsWith("content:")) {
            _uiState.update { it.copy(searchQuery = query, searchResults = emptyList(), isSearching = false, error = app.getString(R.string.saf_search_unavailable)) }
            return
        }
        _uiState.update { it.copy(searchQuery = query, isSearching = true, error = null) }
        searchJob = viewModelScope.launch {
            val result = app.files.search(_uiState.value.currentPath, SearchFilters(query), _uiState.value.preferences.showHidden)
            _uiState.update { it.copy(searchResults = result.getOrDefault(emptyList()), isSearching = false, error = result.exceptionOrNull()?.message) }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _uiState.update { it.copy(searchQuery = "", searchResults = emptyList(), isSearching = false) }
    }

    fun analyzeStorage(location: StorageLocation? = null) {
        val target = location ?: _uiState.value.locations.firstOrNull() ?: return
        analysisJob?.cancel()
        _uiState.update { it.copy(isAnalyzing = true, analysis = null) }
        analysisJob = viewModelScope.launch {
            val result = StorageAnalyzer().analyze(java.io.File(target.path))
            _uiState.update { it.copy(isAnalyzing = false, analysis = result.getOrNull(), error = result.exceptionOrNull()?.message) }
        }
    }

    fun loadRecycleBin() {
        viewModelScope.launch { _uiState.update { it.copy(recycleEntries = app.recycleBin.list()) } }
    }

    fun restore(entry: RecycleEntry) = viewModelScope.launch {
        app.operations.restoreRecycleEntry(com.t3code.explorer.data.files.RecycleEntryRecord(entry.originalPath, entry.deletedPath)).fold({ showMessage(R.string.restored) }, ::showError)
        loadRecycleBin()
    }

    fun emptyRecycleBin() = viewModelScope.launch {
        app.recycleBin.empty().fold({ showMessage(R.string.recycle_bin_emptied) }, ::showError)
        loadRecycleBin()
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null, error = null) }
    private fun isSafPath() = _uiState.value.currentPath.startsWith("content:")
    private fun showMessage(messageRes: Int) { _uiState.update { it.copy(message = app.getString(messageRes)) } }
    private fun showError(error: Throwable) { _uiState.update { it.copy(error = error.message ?: app.getString(R.string.operation_failed)) } }
}

data class ExplorerUiState(
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    val selected: Set<String> = emptySet(),
    val locations: List<StorageLocation> = emptyList(),
    val preferences: UserPreferences = UserPreferences(),
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<FileItem> = emptyList(),
    val isAnalyzing: Boolean = false,
    val analysis: StorageAnalysis? = null,
    val recycleEntries: List<RecycleEntry> = emptyList(),
    val operation: com.t3code.explorer.domain.model.OperationProgress? = null,
    val message: String? = null,
    val error: String? = null,
    val shareIntent: android.content.Intent? = null
)

private fun UserPreferences.toSortSpec() = SortSpec(sortField, sortDirection)
