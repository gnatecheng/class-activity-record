package com.classrecord.app

import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocaleApplierTest {
    @Test
    fun englishAndChineseUseFixedTags() {
        assertEquals("en", LocaleApplier.applicationLocalesFor(AppLanguage.EN)[0]?.language)
        assertEquals("zh", LocaleApplier.applicationLocalesFor(AppLanguage.ZH)[0]?.language)
    }

    @Test
    fun systemUsesEmptyLocaleListToFollowDevice() {
        assertTrue(LocaleApplier.applicationLocalesFor(AppLanguage.SYSTEM).isEmpty)
    }
}
