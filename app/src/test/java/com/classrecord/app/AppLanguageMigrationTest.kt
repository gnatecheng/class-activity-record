package com.classrecord.app

import androidx.test.core.app.ApplicationProvider
import com.classrecord.app.data.prefs.UserPrefs
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(application = android.app.Application::class)
class AppLanguageMigrationTest {
    @Before
    fun clearPrefs() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        File(context.filesDir, "datastore").deleteRecursively()
        context.getSharedPreferences(LocaleApplier.PREFS_BOOT, android.content.Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun migrateCopiesBootEnglishIntoDataStoreAndApplies() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        LocaleApplier.persistForBoot(context, AppLanguage.EN)
        val prefs = UserPrefs(context)
        prefs.migrateAndApplyStoredLanguage()
        assertEquals(AppLanguage.EN, prefs.appLanguage.first())
        assertEquals("en", LocaleApplier.applicationLocalesFor(AppLanguage.EN)[0]?.language)
    }
}
