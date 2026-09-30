package com.classrecord.app

import android.content.Context
import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.di.AppContainer

/** Robolectric application with an in-memory database for deterministic screenshots. */
class ScreenshotTestApp : ClassRecordApp() {
    override fun createAppContainer(context: Context): AppContainer =
        AppContainer(context, AppDatabase.createInMemory(context))
}
