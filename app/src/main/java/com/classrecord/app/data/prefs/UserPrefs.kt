package com.classrecord.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    fun isDark(systemDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemDark
        LIGHT -> false
        DARK -> true
    }

    val label: String
        get() = when (this) {
            SYSTEM -> "跟随系统"
            LIGHT -> "浅色"
            DARK -> "深色"
        }

    companion object {
        fun fromStorage(value: String?): ThemeMode =
            entries.find { it.name == value } ?: SYSTEM
    }
}

class UserPrefs(context: Context) {
    private val dataStore = context.applicationContext.settingsDataStore

    val sortByStudentNo: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_SORT_STUDENT_NO] ?: true
    }

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorage(prefs[KEY_THEME_MODE])
    }

    suspend fun setSortByStudentNo(value: Boolean) {
        dataStore.edit { it[KEY_SORT_STUDENT_NO] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    companion object {
        private val KEY_SORT_STUDENT_NO = booleanPreferencesKey("sort_by_student_no")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    }
}
