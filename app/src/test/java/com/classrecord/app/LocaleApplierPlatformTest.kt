package com.classrecord.app

import androidx.test.core.app.ApplicationProvider
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class LocaleApplierPlatformTest {
    @Test
    fun applyEnglishAndVerifyOnApi33Shadow() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assertTrue(LocaleApplier.apply(context, AppLanguage.EN, allowClearToSystem = true))
        assertEquals(AppLanguage.EN, LocaleApplier.readAppliedLanguage(context))
    }

    @Test
    fun systemNoOpPreservesExistingLocalesWhenNotAllowed() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.apply(context, AppLanguage.ZH, allowClearToSystem = true)
        LocaleApplier.apply(context, AppLanguage.SYSTEM, allowClearToSystem = false)
        assertEquals(AppLanguage.ZH, LocaleApplier.readAppliedLanguage(context))
    }
}
