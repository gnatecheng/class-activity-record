package com.classrecord.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ClassRecordApp::class, sdk = [34])
class MainActivitySmokeTest {
    @After
    fun resetLocales() {
        LocaleApplier.apply(AppLanguage.SYSTEM)
    }

    @Test
    fun mainActivityResumesInChineseLocale() {
        LocaleApplier.apply(AppLanguage.ZH)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun mainActivityResumesInEnglishLocale() {
        LocaleApplier.apply(AppLanguage.EN)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }
}
