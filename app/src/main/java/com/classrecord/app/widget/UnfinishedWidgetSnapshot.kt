package com.classrecord.app.widget

import android.content.Context
import com.classrecord.app.R
import com.classrecord.app.data.repo.ActivityListItem

data class UnfinishedWidgetSnapshot(
    val count: Int,
    val caption: String,
    val detail: String
) {
    companion object {
        fun from(context: Context, items: List<ActivityListItem>): UnfinishedWidgetSnapshot {
            val active = items.filter { !it.activity.archived }
            val recent = active.firstOrNull()
            val total = active.sumOf { it.unfinishedCount }
            return if (recent != null) {
                UnfinishedWidgetSnapshot(
                    count = recent.unfinishedCount,
                    caption = recent.activity.title,
                    detail = if (active.size > 1) {
                        context.getString(R.string.widget_all_unfinished, total)
                    } else {
                        "${recent.scopeLabel} · ${recent.doneCount}/${recent.totalCount}"
                    }
                )
            } else {
                UnfinishedWidgetSnapshot(
                    count = total,
                    caption = context.getString(R.string.widget_no_active),
                    detail = context.getString(R.string.widget_tap_open)
                )
            }
        }
    }
}
