package com.classrecord.app

import android.content.Context
import com.classrecord.app.i18n.AppStrings
import org.robolectric.RuntimeEnvironment

object TestContext {
    val context: Context get() = RuntimeEnvironment.getApplication()
    val strings: AppStrings get() = AppStrings(context)
}
