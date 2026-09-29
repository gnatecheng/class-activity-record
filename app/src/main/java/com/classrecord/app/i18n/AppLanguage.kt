package com.classrecord.app.i18n

import java.util.Locale

enum class AppLanguage {
    SYSTEM,
    ZH,
    EN;

    companion object {
        fun fromStorage(value: String?): AppLanguage =
            entries.find { it.name == value } ?: SYSTEM
    }
}

fun resolveAppLocale(mode: AppLanguage, systemLocale: Locale = Locale.getDefault()): Locale =
    when (mode) {
        AppLanguage.ZH -> Locale.forLanguageTag("zh")
        AppLanguage.EN -> Locale.forLanguageTag("en")
        AppLanguage.SYSTEM ->
            if (systemLocale.language.startsWith("en")) {
                Locale.forLanguageTag("en")
            } else {
                Locale.forLanguageTag("zh")
            }
    }
