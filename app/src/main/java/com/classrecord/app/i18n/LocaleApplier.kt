package com.classrecord.app.i18n

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

object LocaleApplier {
    fun applicationLocalesFor(language: AppLanguage): LocaleListCompat =
        when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.EN -> LocaleListCompat.forLanguageTags("en")
            AppLanguage.ZH -> LocaleListCompat.forLanguageTags("zh")
        }

    fun readApplicationLocales(context: Context): LocaleListCompat =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            LocaleListCompat.wrap(manager.applicationLocales)
        } else {
            AppCompatDelegate.getApplicationLocales()
        }

    /** Maps application locales to the in-app language setting. Empty list means Follow system. */
    fun readAppliedLanguage(context: Context): AppLanguage {
        val locales = readApplicationLocales(context)
        if (locales.isEmpty) return AppLanguage.SYSTEM
        return when (locales[0]?.language) {
            "en" -> AppLanguage.EN
            "zh" -> AppLanguage.ZH
            else -> AppLanguage.SYSTEM
        }
    }

    /**
     * @param allowClearToSystem When false, SYSTEM is a no-op (preserves OS per-app language).
     * Set true only when the user explicitly chooses Follow system in app settings.
     */
    fun apply(context: Context, language: AppLanguage, allowClearToSystem: Boolean): Boolean {
        if (language == AppLanguage.SYSTEM && !allowClearToSystem) {
            return true
        }
        val compat = applicationLocalesFor(language)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(android.app.LocaleManager::class.java)
            manager.setApplicationLocales(compat.toPlatformLocaleList())
        } else {
            AppCompatDelegate.setApplicationLocales(compat)
        }
        return verifyApplied(context, language)
    }

    fun verifyApplied(context: Context, expected: AppLanguage): Boolean =
        when (expected) {
            AppLanguage.SYSTEM -> readApplicationLocales(context).isEmpty
            else -> readAppliedLanguage(context) == expected
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

    private fun LocaleListCompat.toPlatformLocaleList(): LocaleList {
        if (isEmpty) {
            return LocaleList.getEmptyLocaleList()
        }
        val tags = mutableListOf<String>()
        for (index in 0 until size()) {
            val locale = get(index)
            if (locale != null) {
                tags.add(locale.toLanguageTag())
            }
        }
        return LocaleList.forLanguageTags(tags.joinToString(","))
    }

    const val PREFS_BOOT = "locale_boot"
    private const val KEY_LANGUAGE = "app_language"
}
