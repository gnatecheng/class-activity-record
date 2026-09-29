package com.classrecord.app.i18n

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleApplier {
    fun apply(language: AppLanguage) {
        val tag = resolveAppLocale(language).toLanguageTag()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
    }

    fun syncFromBlocking(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_LANGUAGE, null)
        val mode = AppLanguage.fromStorage(stored)
        apply(mode)
        return mode
    }

    fun persistForBoot(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFS_BOOT, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE, language.name)
            .apply()
    }

    private const val PREFS_BOOT = "locale_boot"
    private const val KEY_LANGUAGE = "app_language"
}
