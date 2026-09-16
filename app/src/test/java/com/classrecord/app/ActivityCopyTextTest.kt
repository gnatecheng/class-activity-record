package com.classrecord.app

import com.classrecord.app.data.ActivityCopyText
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.data.repo.ActivityMemberRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActivityCopyTextTest {
    private fun paymentDetail(): ActivityDetail {
        val activity = ActivityEntity(
            id = 1,
            scopeType = ScopeType.CLASS,
            type = ActivityType.PAYMENT,
            title = "秋游费用",
            createdAt = 0,
            updatedAt = 0
        )
        return ActivityDetail(
            activity = activity,
            scopeLabel = "全班",
            rows = listOf(
                ActivityMemberRow(
                    member = ActivityMember(1, 1, MemberStatus.DONE, 5000, 5000, null, updatedAt = 0),
                    name = "张三",
                    studentNo = null
                ),
                ActivityMemberRow(
                    member = ActivityMember(1, 2, MemberStatus.PENDING, 5000, 0, null, updatedAt = 0),
                    name = "李四",
                    studentNo = null
                )
            )
        )
    }

    @Test
    fun summaryIncludesTotalsAndUnfinished() {
        val text = ActivityCopyText.summary(paymentDetail())
        assertTrue(text.contains("【秋游费用】全班"))
        assertTrue(text.contains("进度：1/2"))
        assertTrue(text.contains("应缴合计：100.00 元"))
        assertTrue(text.contains("已缴合计：50.00 元"))
        assertTrue(text.contains("李四"))
    }

    @Test
    fun reminderListsPendingWithAmount() {
        val text = ActivityCopyText.reminder(paymentDetail())
        assertTrue(text.startsWith("【催缴】秋游费用（全班）"))
        assertTrue(text.contains("李四（50.00 元）"))
        assertTrue(!text.contains("张三"))
    }

    @Test
    fun nextPeriodTitleReplacesDateSuffix() {
        assertEquals(
            "秋游费用（9月11日）",
            ActivityCopyText.nextPeriodTitle("秋游费用", "9月11日")
        )
        assertEquals(
            "秋游费用（9月12日）",
            ActivityCopyText.nextPeriodTitle("秋游费用（9月11日）", "9月12日")
        )
    }
}
