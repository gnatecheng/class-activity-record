package com.classrecord.app

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.matcher.ViewMatchers.isRoot
import com.classrecord.app.data.demo.DemoDataSeeder
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.prefs.ThemeMode
import com.classrecord.app.di.AppContainer
import com.classrecord.app.di.AppViewModelFactory
import com.classrecord.app.i18n.AppLanguage
import com.classrecord.app.i18n.LocaleApplier
import com.classrecord.app.ui.activities.ActivityDetailScreen
import com.classrecord.app.ui.home.HomeScreen
import com.classrecord.app.ui.home.HomeViewModel
import com.classrecord.app.ui.ledger.LedgerScreen
import com.classrecord.app.ui.members.MembersScreen
import com.classrecord.app.ui.theme.ClassRecordTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File
import java.time.Instant
import java.util.Locale
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowLegacySystemClock

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(
    application = ScreenshotTestApp::class,
    sdk = [33],
    qualifiers = "w360dp-h781dp-port-xxhdpi",
)
class HomepageScreenshotTest {
    private val lifecycleOwner = object : LifecycleOwner {
        private val registry = LifecycleRegistry(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
        override val lifecycle: Lifecycle = registry
    }

    private val viewModelStoreOwner = object : ViewModelStoreOwner {
        override val viewModelStore = ViewModelStore()
    }

    @Test
    fun captureHomepageScreenshotMatrix() {
        listOf(
            Variant("zh", english = false, dark = false),
            Variant("zh", english = false, dark = true),
            Variant("en", english = true, dark = false),
            Variant("en", english = true, dark = true),
        ).forEach { variant ->
            captureVariant(variant)
        }
    }

    private fun captureVariant(variant: Variant) {
        val app = org.robolectric.RuntimeEnvironment.getApplication() as ScreenshotTestApp
        val container = app.container
        prepareVariant(container, variant)
        val ids = runBlocking {
            container.database.activityDao().getAll().associate { it.type to it.id }
        }
        val attendanceId = checkNotNull(ids[ActivityType.ATTENDANCE])
        val paymentId = checkNotNull(ids[ActivityType.PAYMENT])
        val themeFolder = if (variant.dark) "dark" else "light"

        captureScreen(variant.lang, themeFolder, variant.dark, "01-home") {
            HomeContent(container)
        }
        captureScreen(variant.lang, themeFolder, variant.dark, "02-attendance") {
            ActivityDetailScreen(activityId = attendanceId, onBack = {}, onOpenedNew = {})
        }
        captureScreen(variant.lang, themeFolder, variant.dark, "03-payment") {
            ActivityDetailScreen(activityId = paymentId, onBack = {}, onOpenedNew = {})
        }
        captureScreen(variant.lang, themeFolder, variant.dark, "04-ledger") {
            LedgerScreen(onBack = {})
        }
        captureScreen(variant.lang, themeFolder, variant.dark, "05-members") {
            MembersScreen(onBack = {}, onEdit = {})
        }
    }

    private fun captureScreen(
        lang: String,
        themeFolder: String,
        dark: Boolean,
        fileStem: String,
        content: @Composable () -> Unit,
    ) {
        viewModelStoreOwner.viewModelStore.clear()
        val activity = Robolectric.buildActivity(ComponentActivity::class.java)
            .create()
            .start()
            .resume()
            .visible()
            .get()
        activity.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides lifecycleOwner,
                LocalViewModelStoreOwner provides viewModelStoreOwner,
                LocalContext provides activity,
            ) {
                ClassRecordTheme(darkTheme = dark) {
                    content()
                }
            }
        }
        onView(isRoot()).captureRoboImage(screenshotFile(lang, themeFolder, fileStem))
    }

    @Composable
    private fun HomeContent(container: AppContainer) {
        val vm: HomeViewModel = viewModel(factory = AppViewModelFactory(container))
        HomeScreen(
            viewModel = vm,
            onEditClass = {},
            onMembers = {},
            onSubGroups = {},
            onNewActivity = {},
            onOpenActivity = {},
            onLedger = {},
            onSettings = {},
        )
    }

    private data class Variant(val lang: String, val english: Boolean, val dark: Boolean)

    companion object {
        private val FIXED_MILLIS: Long =
            Instant.parse("2026-09-28T04:00:00Z").toEpochMilli()

        private fun prepareVariant(container: AppContainer, variant: Variant) {
            setRobolectricTime(FIXED_MILLIS)
            val localeTag = if (variant.english) "en-US" else "zh-CN"
            val roboLocale = if (variant.english) "en-rUS" else "zh-rCN"
            org.robolectric.RuntimeEnvironment.setQualifiers(
                "$roboLocale-w360dp-h781dp-port-xxhdpi",
            )
            val context = org.robolectric.RuntimeEnvironment.getApplication()
            val config = Configuration(context.resources.configuration)
            config.setLocale(Locale.forLanguageTag(localeTag))
            config.uiMode = (config.uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (variant.dark) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            LocaleApplier.apply(
                context,
                if (variant.english) AppLanguage.EN else AppLanguage.ZH,
                allowClearToSystem = true,
            )
            runBlocking {
                container.database.clearAllTables()
                DemoDataSeeder(
                    container.database,
                    container.classRepository,
                    container.memberRepository,
                    container.subGroupRepository,
                    container.activityRepository,
                    container.ledgerRepository,
                ).seedIfEmpty(english = variant.english)
                container.userPrefs.setThemeMode(
                    if (variant.dark) ThemeMode.DARK else ThemeMode.LIGHT,
                )
            }
        }

        private fun setRobolectricTime(millis: Long) {
            val method = ShadowLegacySystemClock::class.java.getDeclaredMethod(
                "setCurrentTimeMillis",
                Long::class.javaPrimitiveType,
            )
            method.isAccessible = true
            method.invoke(null, millis)
        }

        private fun screenshotFile(lang: String, themeFolder: String, fileStem: String): File {
            val cwd = File(System.getProperty("user.dir"))
            val appModule = if (cwd.name == "app") cwd else File(cwd, "app")
            val dir = File(appModule, "build/homepage-screenshots-raw/$lang/$themeFolder")
            dir.mkdirs()
            return File(dir, "$fileStem.png")
        }
    }
}
