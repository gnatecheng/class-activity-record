package com.classrecord.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
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

    val appLanguage: Flow<AppLanguage> = dataStore.data.map { prefs ->
        AppLanguage.fromStorage(prefs[KEY_APP_LANGUAGE])
    }

    suspend fun setSortByStudentNo(value: Boolean) {
        dataStore.edit { it[KEY_SORT_STUDENT_NO] = value }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_THEME_MODE] = mode.name }
    }

    suspend fun setAppLanguage(language: AppLanguage) {
        dataStore.edit { it[KEY_APP_LANGUAGE] = language.name }
        LocaleApplier.persistForBoot(appContext, language)
        LocaleApplier.apply(language)
    }

    /**
     * One-time upgrade path for 1.5.0: apply the stored DataStore language (or legacy
     * boot SharedPreferences) through AppCompat application locales before any Activity.
     */
    suspend fun migrateAndApplyStoredLanguage() {
        val snapshot = dataStore.data.first()
        val migrated = snapshot[KEY_APP_LOCALE_MIGRATED] ?: false
        var language = AppLanguage.fromStorage(snapshot[KEY_APP_LANGUAGE])
        if (!migrated) {
            if (language == AppLanguage.SYSTEM) {
                LocaleApplier.readBootLanguage(appContext)?.let { boot ->
                    if (boot != AppLanguage.SYSTEM) language = boot
                }
            }
            dataStore.edit {
                it[KEY_APP_LANGUAGE] = language.name
                it[KEY_APP_LOCALE_MIGRATED] = true
            }
        }
        LocaleApplier.apply(language)
        LocaleApplier.persistForBoot(appContext, language)
    }

    private val appContext = context.applicationContext

    companion object {
        private val KEY_SORT_STUDENT_NO = booleanPreferencesKey("sort_by_student_no")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_APP_LOCALE_MIGRATED = booleanPreferencesKey("app_locale_migrated_v151")
    }
}
