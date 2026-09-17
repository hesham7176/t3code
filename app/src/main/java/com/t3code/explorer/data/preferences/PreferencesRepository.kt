package com.t3code.explorer.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.t3code.explorer.domain.model.SortDirection
import com.t3code.explorer.domain.model.SortField
import com.t3code.explorer.domain.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("explorer_settings")

data class UserPreferences(
    val viewMode: ViewMode = ViewMode.MEDIUM_GRID,
    val sortField: SortField = SortField.NAME,
    val sortDirection: SortDirection = SortDirection.ASCENDING,
    val showHidden: Boolean = false,
    val folderCovers: Boolean = true,
    val resumePlayback: Boolean = true,
    val backgroundPlayback: Boolean = true,
    val theme: String = "system",
    val language: String = "system"
)

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val viewMode = stringPreferencesKey("view_mode")
        val sortField = stringPreferencesKey("sort_field")
        val sortDirection = stringPreferencesKey("sort_direction")
        val showHidden = booleanPreferencesKey("show_hidden")
        val folderCovers = booleanPreferencesKey("folder_covers")
        val resumePlayback = booleanPreferencesKey("resume_playback")
        val backgroundPlayback = booleanPreferencesKey("background_playback")
        val theme = stringPreferencesKey("theme")
        val language = stringPreferencesKey("language")
    }

    val preferences: Flow<UserPreferences> = context.settingsDataStore.data.map { values ->
        UserPreferences(
            viewMode = values[Keys.viewMode]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.MEDIUM_GRID,
            sortField = values[Keys.sortField]?.let { runCatching { SortField.valueOf(it) }.getOrNull() } ?: SortField.NAME,
            sortDirection = values[Keys.sortDirection]?.let { runCatching { SortDirection.valueOf(it) }.getOrNull() } ?: SortDirection.ASCENDING,
            showHidden = values[Keys.showHidden] ?: false,
            folderCovers = values[Keys.folderCovers] ?: true,
            resumePlayback = values[Keys.resumePlayback] ?: true,
            backgroundPlayback = values[Keys.backgroundPlayback] ?: true,
            theme = values[Keys.theme] ?: "system",
            language = values[Keys.language] ?: "system"
        )
    }

    suspend fun setViewMode(value: ViewMode) = context.settingsDataStore.edit { it[Keys.viewMode] = value.name }
    suspend fun setSort(field: SortField, direction: SortDirection) = context.settingsDataStore.edit {
        it[Keys.sortField] = field.name
        it[Keys.sortDirection] = direction.name
    }
    suspend fun setShowHidden(value: Boolean) = context.settingsDataStore.edit { it[Keys.showHidden] = value }
    suspend fun setFolderCovers(value: Boolean) = context.settingsDataStore.edit { it[Keys.folderCovers] = value }
    suspend fun setResumePlayback(value: Boolean) = context.settingsDataStore.edit { it[Keys.resumePlayback] = value }
    suspend fun setBackgroundPlayback(value: Boolean) = context.settingsDataStore.edit { it[Keys.backgroundPlayback] = value }
    suspend fun setTheme(value: String) = context.settingsDataStore.edit { it[Keys.theme] = value }
    suspend fun setLanguage(value: String) = context.settingsDataStore.edit { it[Keys.language] = value }
}
