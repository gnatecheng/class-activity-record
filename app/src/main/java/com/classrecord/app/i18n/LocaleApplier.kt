package com.classrecord.app.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleApplier {
    fun applicationLocalesFor(language: AppLanguage): LocaleListCompat =
        when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.ZH -> LocaleListCompat.forLanguageTags("zh")
        }

    fun apply(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(applicationLocalesFor(language))
    }

    /** Maps AppCompat application locales back to the in-app language setting. */
    fun readAppliedLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0]?.language) {
            "en" -> AppLanguage.EN
            "zh" -> AppLanguage.ZH
            else -> AppLanguage.SYSTEM
        }
    }

    fun persistForBoot(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    fun readBootLanguage(context: Context): AppLanguage? {
        val stored = context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE, null)
        return stored?.let { AppLanguage.fromStorage(it) }
    }

    const val PREFS_BOOT = "locale_boot"
    private const val KEY_LANGUAGE = "app_language"
}
