package com.classrecord.app

import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityListItem
import com.classrecord.app.ui.home.ActivityArchiveFilter
import com.classrecord.app.ui.home.ActivityListQuery
import com.classrecord.app.widget.UnfinishedWidgetSnapshot
import org.junit.Assert.assertEquals
import org.junit.Test

class ActivityListQueryTest {
    private val items = listOf(
        item(1, "秋游缴费", archived = false),
        item(2, "月考出勤", archived = false),
        item(3, "秋游缴费补缴", archived = true)
    )

    @Test
    fun activeFilterHidesArchived() {
        val visible = ActivityListQuery.filter(items, "", ActivityArchiveFilter.ACTIVE)
        assertEquals(listOf(1L, 2L), visible.map { it.activity.id })
    }

    @Test
    fun archivedFilterOnlyShowsArchived() {
        val visible = ActivityListQuery.filter(items, "", ActivityArchiveFilter.ARCHIVED)
        assertEquals(listOf(3L), visible.map { it.activity.id })
    }

    @Test
    fun titleSearchIsCaseInsensitiveSubstring() {
        val visible = ActivityListQuery.filter(items, "秋游", ActivityArchiveFilter.ACTIVE)
        assertEquals(listOf(1L), visible.map { it.activity.id })
    }

    @Test
    fun searchCanFindArchivedTitle() {
        val visible = ActivityListQuery.filter(items, "补缴", ActivityArchiveFilter.ARCHIVED)
        assertEquals(listOf(3L), visible.map { it.activity.id })
    }
}

class UnfinishedWidgetSnapshotTest {
    @Test
    fun usesRecentActiveActivityCount() {
        val snapshot = UnfinishedWidgetSnapshot.from(
            listOf(
                item(2, "月考出勤", archived = false, unfinished = 3),
                item(1, "旧事务", archived = false, unfinished = 9),
                item(3, "已归档", archived = true, unfinished = 5)
            )
        )
        assertEquals(3, snapshot.count)
        assertEquals("月考出勤", snapshot.caption)
        assertEquals("全部进行中未完成 12", snapshot.detail)
    }

    @Test
    fun emptyActiveFallsBackToOpenHint() {
        val snapshot = UnfinishedWidgetSnapshot.from(
            listOf(item(1, "旧事务", archived = true, unfinished = 4))
        )
        assertEquals(0, snapshot.count)
        assertEquals("暂无进行中事务", snapshot.caption)
        assertEquals("点按打开应用", snapshot.detail)
    }
}

private fun item(
    id: Long,
    title: String,
    archived: Boolean,
    unfinished: Int = 1
): ActivityListItem {
    return ActivityListItem(
        activity = ActivityEntity(
            id = id,
            scopeType = ScopeType.CLASS,
            type = ActivityType.PAYMENT,
            title = title,
            archived = archived,
            createdAt = id,
            updatedAt = id
        ),
        scopeLabel = "全班",
        doneCount = 10 - unfinished,
        totalCount = 10,
        unfinishedCount = unfinished
    )
}
