package com.classrecord.app

import com.classrecord.app.data.MemberSort
import com.classrecord.app.data.Money
import com.classrecord.app.data.SplitPlan
import com.classrecord.app.data.SplitShare
import com.classrecord.app.data.csv.Csv
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.repo.ActivityMemberRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SplitPlanTest {
    @Test
    fun splitByWeightRemainderToFirst() {
        val parts = Money.splitByWeight(1000, listOf(1, 1, 2))
        assertEquals(1000L, parts.sum())
        assertEquals(listOf(250L, 250L, 500L), parts)
    }

    @Test
    fun splitByWeightUneven() {
        val parts = Money.splitByWeight(100, listOf(1, 1, 1))
        assertEquals(100L, parts.sum())
        assertEquals(listOf(34L, 33L, 33L), parts)
    }

    @Test
    fun excludeAndOverride() {
        val shares = listOf(
            SplitShare(1, included = false, weight = 1),
            SplitShare(2, included = true, weight = 1, customDueFen = 200),
            SplitShare(3, included = true, weight = 1),
            SplitShare(4, included = true, weight = 1)
        )
        val amounts = SplitPlan.amounts(1000, shares)
        assertEquals(0L, amounts[0])
        assertEquals(200L, amounts[1])
        assertEquals(800L, amounts[2] + amounts[3])
        assertEquals(1000L, amounts.sum())
    }
}

class MemberSortTest {
    @Test
    fun emptyStudentNoLast() {
        val a = Member(1, "张三", studentNo = null)
        val b = Member(2, "李四", studentNo = "02")
        val c = Member(3, "王五", studentNo = "01")
        val sorted = MemberSort.members(listOf(a, b, c), byStudentNo = true)
        assertEquals(listOf("王五", "李四", "张三"), sorted.map { it.name })
    }

    @Test
    fun activityRowsPendingFirstThenStudentNo() {
        fun row(name: String, no: String?, status: MemberStatus) = ActivityMemberRow(
            member = ActivityMember(
                activityId = 1,
                memberId = name.hashCode().toLong(),
                status = status,
                updatedAt = 0
            ),
            name = name,
            studentNo = no
        )
        val list = listOf(
            row("张三", null, MemberStatus.DONE),
            row("李四", "02", MemberStatus.PENDING),
            row("王五", "01", MemberStatus.PENDING)
        )
        val sorted = MemberSort.activityRows(list, byStudentNo = true)
        assertEquals(listOf("王五", "李四", "张三"), sorted.map { it.name })
    }
}

class CsvTest {
    @Test
    fun escapeQuotesAndComma() {
        assertEquals("a", Csv.escape("a"))
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        val line = Csv.row("张三", "01", "备注,含逗号")
        assertTrue(line.contains("\"备注,含逗号\""))
    }
}
