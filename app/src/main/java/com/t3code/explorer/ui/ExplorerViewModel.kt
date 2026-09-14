package com.t3code.explorer.ui

import android.app.Application
import android.net.Uri
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.t3code.explorer.ExplorerApplication
import com.t3code.explorer.data.analyzer.StorageAnalyzer
import com.t3code.explorer.data.preferences.UserPreferences
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExplorerViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as ExplorerApplication
    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        val primary = app.storage.primary()?.path ?: application.filesDir.absolutePath
        _uiState.update { it.copy(currentPath = primary, locations = app.storage.locations()) }
        viewModelScope.launch {
            app.preferences.preferences.collect { preferences ->
                _uiState.update { it.copy(preferences = preferences) }
                if (preferences.language != "system" && AppCompatDelegate.applicationLocales.toLanguageTags() != preferences.language) AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(preferences.language))
                loadDirectory(_uiState.value.currentPath)
            }
        }
        viewModelScope.launch { app.operations.progress.collect { value -> _uiState.update { it.copy(operation = value) } } }
    }

    fun loadDirectory(path: String = _uiState.value.currentPath) {
        _uiState.update { it.copy(currentPath = path, isLoading = true, error = null, selected = emptySet()) }
        viewModelScope.launch {
            val result = if (path.startsWith("content:")) app.files.listSafTree(Uri.parse(path), _uiState.value.preferences.showHidden) else app.files.list(path, _uiState.value.preferences.toSortSpec(), _uiState.value.preferences.showHidden)
            result.fold(
                onSuccess = { items -> _uiState.update { it.copy(items = items, isLoading = false) } },
                onFailure = { error -> _uiState.update { it.copy(items = emptyList(), isLoading = false, error = error.message) } }
            )
        }
    }

    fun openSafTree(uri: Uri) {
        runCatching {
            app.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        loadDirectory(uri.toString())
    }

    fun navigateUp() {
        val current = java.io.File(_uiState.value.currentPath)
        current.parentFile?.let { loadDirectory(it.absolutePath) }
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
        app.operations.createFolder(java.io.File(_uiState.value.currentPath), name).fold({ showMessage("Folder created") }, ::showError)
        loadDirectory()
    }

    fun createFile(name: String) = viewModelScope.launch {
        app.operations.createFile(java.io.File(_uiState.value.currentPath), name).fold({ showMessage("File created") }, ::showError)
        loadDirectory()
    }

    fun rename(item: FileItem, name: String) = viewModelScope.launch {
        app.operations.rename(item, name).fold({ showMessage("Renamed") }, ::showError)
        loadDirectory()
    }

    fun transferSelected(destination: String, move: Boolean) = viewModelScope.launch {
        val items = _uiState.value.items.filter { it.path in _uiState.value.selected }
        val result = if (move) app.operations.move(items, java.io.File(destination)) else app.operations.copy(items, java.io.File(destination))
        result.fold({ showMessage(if (move) "Moved" else "Copied") }, ::showError)
        clearSelection()
        loadDirectory()
    }

    fun deleteSelected() = viewModelScope.launch {
        val items = _uiState.value.items.filter { it.path in _uiState.value.selected }
        app.operations.deleteToRecycleBin(items).fold({ showMessage("Moved to recycle bin") }, ::showError)
        clearSelection()
        loadDirectory()
    }

    fun share(item: FileItem) { _uiState.update { it.copy(shareIntent = app.operations.shareIntent(item)) } }
    fun consumeShareIntent() { _uiState.update { it.copy(shareIntent = null) } }

    fun search(query: String) {
        _uiState.update { it.copy(searchQuery = query, isSearching = true) }
        viewModelScope.launch {
            val result = app.files.search(_uiState.value.currentPath, SearchFilters(query), _uiState.value.preferences.showHidden)
            _uiState.update { it.copy(searchResults = result.getOrDefault(emptyList()), isSearching = false) }
        }
    }

    fun clearSearch() = _uiState.update { it.copy(searchQuery = "", searchResults = emptyList()) }

    fun analyzeStorage(location: StorageLocation? = null) {
        val target = location ?: _uiState.value.locations.firstOrNull() ?: return
        _uiState.update { it.copy(isAnalyzing = true, analysis = null) }
        viewModelScope.launch {
            val result = StorageAnalyzer().analyze(java.io.File(target.path))
            _uiState.update { it.copy(isAnalyzing = false, analysis = result.getOrNull(), error = result.exceptionOrNull()?.message) }
        }
    }

    fun loadRecycleBin() {
        viewModelScope.launch { _uiState.update { it.copy(recycleEntries = app.recycleBin.list()) } }
    }

    fun restore(entry: RecycleEntry) = viewModelScope.launch {
        app.operations.restoreRecycleEntry(com.t3code.explorer.data.files.RecycleEntryRecord(entry.originalPath, entry.deletedPath)).fold({ showMessage("Restored") }, ::showError)
        loadRecycleBin()
    }

    fun emptyRecycleBin() = viewModelScope.launch {
        app.recycleBin.empty().fold({ showMessage("Recycle bin emptied") }, ::showError)
        loadRecycleBin()
    }

    fun consumeMessage() = _uiState.update { it.copy(message = null, error = null) }
    private fun showMessage(message: String) { _uiState.update { it.copy(message = message) } }
    private fun showError(error: Throwable) { _uiState.update { it.copy(error = error.message ?: "Operation failed") } }
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
