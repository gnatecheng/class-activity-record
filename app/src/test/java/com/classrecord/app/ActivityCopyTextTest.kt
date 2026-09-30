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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], qualifiers = "zh-rCN")
class ActivityCopyTextTest {
    private val context = TestContext.context
    private val strings = TestContext.strings

    private fun paymentDetail(): ActivityDetail {
        val activity = ActivityEntity(
            id = 1,
            scopeType = ScopeType.CLASS,
            type = ActivityType.PAYMENT,
            title = "秋季出游费用",
            createdAt = 0,
            updatedAt = 0
        )
        return ActivityDetail(
            activity = activity,
            scopeLabel = "全体",
            rows = listOf(
                ActivityMemberRow(
                    member = ActivityMember(1, 1, MemberStatus.DONE, 5000, 5000, null, updatedAt = 0),
                    name = "张三",
                    studentNo = null
                ),
                ActivityMemberRow(
                    member = ActivityMember(1, 2, MemberStatus.PENDING, 5000, 0, null, updatedAt = 0),
                    name = "李四",
                    studentNo = "2023002"
                )
            )
        )
    }

    @Test
    fun summaryIncludesTotalsAndUnfinished() {
        val text = ActivityCopyText.summary(paymentDetail(), strings, context)
        assertTrue(text.contains("【秋季出游费用】全体"))
        assertTrue(text.contains("进度：1/2"))
        assertTrue(text.contains("应缴合计：100.00 元"))
        assertTrue(text.contains("已缴合计：50.00 元"))
        assertTrue(text.contains("李四"))
    }

    @Test
    fun reminderIsWeChatReadyNumberedList() {
        val text = ActivityCopyText.reminder(paymentDetail(), strings, context)
        assertTrue(text.startsWith("【催缴】秋季出游费用"))
        assertTrue(text.contains("全体 · 缴费"))
        assertTrue(text.contains("未完成 1 人，合计 50.00 元"))
        assertTrue(text.contains("1. 李四（2023002）  尚欠 50.00 元"))
        assertTrue(!text.contains("张三"))
        assertTrue(!text.contains("、"))
    }

    @Test
    fun unfinishedPlainIsTabSeparated() {
        val text = ActivityCopyText.unfinishedPlain(paymentDetail(), context)
        assertTrue(text.startsWith("秋季出游费用 未完成名单"))
        assertTrue(text.contains("李四（2023002）\t50.00"))
        assertTrue(text.contains("合计\t50.00"))
        assertTrue(!text.contains("张三"))
    }

    @Test
    fun nextPeriodTitleReplacesDateSuffix() {
        assertEquals(
            "秋季出游费用（9月11日）",
            ActivityCopyText.nextPeriodTitle("秋季出游费用", "9月11日", english = false)
        )
        assertEquals(
            "秋季出游费用（9月12日）",
            ActivityCopyText.nextPeriodTitle("秋季出游费用（9月11日）", "9月12日", english = false)
        )
    }
}
