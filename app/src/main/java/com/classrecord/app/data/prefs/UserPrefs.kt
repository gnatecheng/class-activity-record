package com.classrecord.app.data.prefs

import android.content.Context
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
        dataStore.edit {
            it[KEY_APP_LANGUAGE] = language.name
            it.remove(KEY_PENDING_LOCALE_APPLY)
        }
        LocaleApplier.persistForBoot(appContext, language)
        LocaleApplier.apply(
            appContext,
            language,
            allowClearToSystem = language == AppLanguage.SYSTEM,
        )
    }

    /**
     * Resolves stored language from DataStore / legacy boot prefs. On API 33+ deferred
     * locale apply runs in [applyPendingLocaleIfNeeded] when an Activity exists.
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
            dataStore.edit { it[KEY_APP_LANGUAGE] = language.name }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!migrated && language != AppLanguage.SYSTEM) {
                dataStore.edit { it[KEY_PENDING_LOCALE_APPLY] = language.name }
            } else if (!migrated && language == AppLanguage.SYSTEM) {
                // Do not clear OS per-app locales on first launch / upgrade to Follow system.
                dataStore.edit { it[KEY_APP_LOCALE_MIGRATED] = true }
            }
        } else {
            val ok = LocaleApplier.apply(
                appContext,
                language,
                allowClearToSystem = language == AppLanguage.SYSTEM,
            )
            if (ok && !migrated) {
                dataStore.edit { it[KEY_APP_LOCALE_MIGRATED] = true }
            }
            LocaleApplier.persistForBoot(appContext, language)
        }
    }

    /** Call from MainActivity before setContent on API 33+. */
    suspend fun applyPendingLocaleIfNeeded(activityContext: Context): Boolean {
        val snapshot = dataStore.data.first()
        val pending = snapshot[KEY_PENDING_LOCALE_APPLY]?.let { AppLanguage.fromStorage(it) }
            ?: return finishMigrationIfNeeded(snapshot)

        val applied = LocaleApplier.apply(
            activityContext,
            pending,
            allowClearToSystem = true,
        )
        if (applied) {
            dataStore.edit {
                it.remove(KEY_PENDING_LOCALE_APPLY)
                it[KEY_APP_LOCALE_MIGRATED] = true
            }
            LocaleApplier.persistForBoot(appContext, pending)
        }
        return applied
    }

    /** Test-only: reset locale migration flags without touching other settings. */
    suspend fun configureLocaleStateForTests(
        language: AppLanguage,
        migrated: Boolean,
        pending: AppLanguage? = null,
    ) {
        dataStore.edit {
            it[KEY_APP_LANGUAGE] = language.name
            it[KEY_APP_LOCALE_MIGRATED] = migrated
            if (pending != null) {
                it[KEY_PENDING_LOCALE_APPLY] = pending.name
            } else {
                it.remove(KEY_PENDING_LOCALE_APPLY)
            }
        }
    }

    private suspend fun finishMigrationIfNeeded(snapshot: Preferences): Boolean {
        if (snapshot[KEY_APP_LOCALE_MIGRATED] != true) {
            dataStore.edit { it[KEY_APP_LOCALE_MIGRATED] = true }
        }
        return true
    }

    private val appContext = context.applicationContext

    companion object {
        private val KEY_SORT_STUDENT_NO = booleanPreferencesKey("sort_by_student_no")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_APP_LANGUAGE = stringPreferencesKey("app_language")
        private val KEY_APP_LOCALE_MIGRATED = booleanPreferencesKey("app_locale_migrated_v151")
        private val KEY_PENDING_LOCALE_APPLY = stringPreferencesKey("pending_locale_apply_v151")
    }
}
