package com.classrecord.app.i18n

import android.content.Context
import com.classrecord.app.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateFormats {
    fun formatDay(context: Context, millis: Long): String {
        val locale = context.resources.configuration.locales[0]
        val pattern = context.getString(
            if (locale.language.startsWith("en")) R.string.date_day_en else R.string.date_day_zh
        )
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatDate(context: Context, millis: Long): String {
        val locale = context.resources.configuration.locales[0]
        val pattern = context.getString(
            if (locale.language.startsWith("en")) R.string.date_full_en else R.string.date_full_zh
        )
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun formatBuildTime(context: Context, iso: String): String {
        return runCatching {
            val instant = Instant.parse(iso)
            val locale = context.resources.configuration.locales[0]
            val pattern = context.getString(
                if (locale.language.startsWith("en")) R.string.date_build_en else R.string.date_build_zh
            )
            instant.atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern(pattern, locale))
        }.getOrDefault(iso)
    }
}
