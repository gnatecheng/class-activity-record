package com.classrecord.app.data

import android.content.Context
import com.classrecord.app.R
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.i18n.DateFormats
import java.time.Instant
import java.time.ZoneId

object ActivityTemplates {
    fun titles(type: ActivityType, context: Context, nowMillis: Long = System.currentTimeMillis()): List<String> {
        val local = Instant.ofEpochMilli(nowMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        val day = DateFormats.formatDay(context, nowMillis)
        val month = local.monthValue
        return when (type) {
            ActivityType.ATTENDANCE -> listOf(
                context.getString(R.string.template_attendance_today, day),
                context.getString(R.string.template_attendance_morning, day),
                context.getString(R.string.template_attendance_evening, day)
            )
            ActivityType.PAYMENT -> listOf(
                context.getString(R.string.template_payment_class, month),
                context.getString(R.string.template_payment_event, day)
            )
            ActivityType.SPLIT -> listOf(
                context.getString(R.string.template_split_dorm),
                context.getString(R.string.template_split_meal, day)
            )
            ActivityType.CHECKLIST -> listOf(
                context.getString(R.string.template_checklist_homework, day),
                context.getString(R.string.template_checklist_duty, day)
            )
        }
    }
}
