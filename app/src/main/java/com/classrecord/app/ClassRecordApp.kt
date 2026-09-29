package com.classrecord.app

import android.app.Application
import com.classrecord.app.di.AppContainer
import com.classrecord.app.i18n.LocaleApplier
import com.classrecord.app.widget.UnfinishedWidgetUpdater
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ClassRecordApp : Application() {
    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        LocaleApplier.syncFromBlocking(this)
        container = AppContainer(this)
        runBlocking {
            val language = container.userPrefs.appLanguage.first()
            LocaleApplier.persistForBoot(this@ClassRecordApp, language)
            LocaleApplier.apply(language)
        }
        UnfinishedWidgetUpdater.observe(this, appScope, container.activityRepository, container.appStrings)
    }
}
