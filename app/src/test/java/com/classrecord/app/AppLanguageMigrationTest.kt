package com.classrecord.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.classrecord.app.data.prefs.UserPrefs
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ClassRecordApp::class, sdk = [34])
class AppLanguageMigrationTest {
    @Test
    fun pendingChineseAppliedWhenMainActivityStarts() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val prefs = (context.applicationContext as ClassRecordApp).container.userPrefs
        prefs.configureLocaleStateForTests(
            language = AppLanguage.ZH,
            migrated = false,
            pending = AppLanguage.ZH,
        )
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
        assertEquals(AppLanguage.ZH, LocaleApplier.readAppliedLanguage(context))
    }

    @Test
    fun frameworkChineseSyncsStoredFromFollowSystemWithoutApply() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.apply(context, AppLanguage.ZH, allowClearToSystem = true)
        val prefs = (context.applicationContext as ClassRecordApp).container.userPrefs
        prefs.configureLocaleStateForTests(
            language = AppLanguage.SYSTEM,
            migrated = true,
            pending = null,
        )
        assertEquals(AppLanguage.ZH, prefs.syncStoredLanguageFromFramework())
        assertEquals(AppLanguage.ZH, prefs.appLanguage.first())
        assertEquals(AppLanguage.ZH, LocaleApplier.readAppliedLanguage(context))
    }

    @Test
    fun frameworkEnglishSyncsOverStoredChineseWithoutApply() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.apply(context, AppLanguage.EN, allowClearToSystem = true)
        val prefs = (context.applicationContext as ClassRecordApp).container.userPrefs
        prefs.configureLocaleStateForTests(
            language = AppLanguage.ZH,
            migrated = true,
            pending = null,
        )
        assertEquals(AppLanguage.EN, prefs.syncStoredLanguageFromFramework())
        assertEquals(AppLanguage.EN, prefs.appLanguage.first())
        assertEquals(AppLanguage.EN, LocaleApplier.readAppliedLanguage(context))
    }

    @Test
    fun followSystemDoesNotClearFrameworkLocalesOnSettingsRefresh() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.apply(context, AppLanguage.ZH, allowClearToSystem = true)
        val prefs = (context.applicationContext as ClassRecordApp).container.userPrefs
        prefs.configureLocaleStateForTests(
            language = AppLanguage.SYSTEM,
            migrated = true,
            pending = null,
        )
        LocaleApplier.apply(context, AppLanguage.SYSTEM, allowClearToSystem = false)
        assertEquals(AppLanguage.ZH, LocaleApplier.readAppliedLanguage(context))
    }
}
