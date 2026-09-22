package com.classrecord.app

import com.classrecord.app.data.ActivityTemplates
import com.classrecord.app.data.csv.Csv
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.data.repo.ActivityMemberRow
import org.junit.Assert.assertTrue
import org.junit.Test

class UnfinishedExportTest {
    @Test
    fun unfinishedCsvListsPendingAmounts() {
        val detail = ActivityDetail(
            activity = ActivityEntity(
                id = 1,
                scopeType = ScopeType.CLASS,
                type = ActivityType.SPLIT,
                title = "寝室水电",
                createdAt = 0,
                updatedAt = 0
            ),
            scopeLabel = "全班",
            rows = listOf(
                ActivityMemberRow(
                    member = ActivityMember(1, 1, MemberStatus.PENDING, 3300, 0, null, updatedAt = 0),
                    name = "甲",
                    studentNo = "1"
                ),
                ActivityMemberRow(
                    member = ActivityMember(1, 2, MemberStatus.DONE, 3300, 3300, null, updatedAt = 0),
                    name = "乙",
                    studentNo = "2"
                )
            )
        )
        val csv = Csv.unfinished(detail)
        assertTrue(csv.contains("甲"))
        assertTrue(csv.contains("33.00"))
        assertTrue(csv.contains("合计"))
        assertTrue(!csv.contains("乙"))
    }

    @Test
    fun attendancePresetsIncludeTodayRollCall() {
        val titles = ActivityTemplates.titles(ActivityType.ATTENDANCE, 1_726_272_000_000L)
        assertTrue(titles.any { it.startsWith("今日点名") })
        assertTrue(ActivityTemplates.titles(ActivityType.SPLIT).any { it.contains("寝室") })
    }
}
