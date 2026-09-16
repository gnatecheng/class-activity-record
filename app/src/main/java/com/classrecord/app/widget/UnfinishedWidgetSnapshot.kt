package com.classrecord.app.widget

import com.classrecord.app.data.repo.ActivityListItem

data class UnfinishedWidgetSnapshot(
    val count: Int,
    val caption: String,
    val detail: String
) {
    companion object {
        fun from(items: List<ActivityListItem>): UnfinishedWidgetSnapshot {
            val active = items.filter { !it.activity.archived }
            val recent = active.firstOrNull()
            val total = active.sumOf { it.unfinishedCount }
            return if (recent != null) {
                UnfinishedWidgetSnapshot(
                    count = recent.unfinishedCount,
                    caption = recent.activity.title,
                    detail = if (active.size > 1) {
                        "全部进行中未完成 $total"
                    } else {
                        "${recent.scopeLabel} · ${recent.doneCount}/${recent.totalCount}"
                    }
                )
            } else {
                UnfinishedWidgetSnapshot(
                    count = total,
                    caption = "暂无进行中事务",
                    detail = "点按打开应用"
                )
            }
        }
    }
}
