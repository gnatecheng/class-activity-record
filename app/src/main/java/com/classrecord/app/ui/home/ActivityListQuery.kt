package com.classrecord.app.ui.home

import com.classrecord.app.data.repo.ActivityListItem

enum class ActivityArchiveFilter {
    ACTIVE,
    ARCHIVED
}

object ActivityListQuery {
    fun filter(
        items: List<ActivityListItem>,
        query: String,
        filter: ActivityArchiveFilter
    ): List<ActivityListItem> {
        val needle = query.trim()
        val archivedOnly = filter == ActivityArchiveFilter.ARCHIVED
        return items.filter { item ->
            item.activity.archived == archivedOnly &&
                (needle.isEmpty() || item.activity.title.contains(needle, ignoreCase = true))
        }
    }
}
