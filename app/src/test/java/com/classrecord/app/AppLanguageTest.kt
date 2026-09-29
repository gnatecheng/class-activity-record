package com.classrecord.app

import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.resolveAppLocale
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppLanguageTest {
    @Test
    fun fixedModesUseExpectedLanguageTags() {
        assertEquals("zh", resolveAppLocale(AppLanguage.ZH).toLanguageTag())
        assertEquals("en", resolveAppLocale(AppLanguage.EN).toLanguageTag())
    }

    @Test
    fun systemModeFollowsEnglishOrChineseFromSystemLocale() {
        assertEquals(
            "en",
            resolveAppLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("en-US")).language
        )
        assertEquals(
            "zh",
            resolveAppLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("zh-CN")).language
        )
        assertEquals(
            "zh",
            resolveAppLocale(AppLanguage.SYSTEM, Locale.forLanguageTag("fr-FR")).language
        )
    }
}
