package com.classrecord.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = ClassRecordApp::class, sdk = [34])
class MainActivitySmokeTest {
    @After
    fun resetLocales() {
        val context = RuntimeEnvironment.getApplication()
        LocaleApplier.apply(context, AppLanguage.SYSTEM, allowClearToSystem = true)
    }

    @Test
    fun mainActivityResumesInChineseLocale() {
        val context = RuntimeEnvironment.getApplication()
        LocaleApplier.apply(context, AppLanguage.ZH, allowClearToSystem = true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }

    @Test
    fun mainActivityResumesInEnglishLocale() {
        val context = RuntimeEnvironment.getApplication()
        LocaleApplier.apply(context, AppLanguage.EN, allowClearToSystem = true)
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
        }
    }
}
