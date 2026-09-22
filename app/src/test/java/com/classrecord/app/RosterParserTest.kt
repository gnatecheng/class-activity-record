package com.classrecord.app

import com.classrecord.app.data.ExistingRosterPerson
import com.classrecord.app.data.RosterParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RosterParserTest {
    @Test
    fun parseMultilineAndDunhao() {
        val result = RosterParser.parse("张三\n李四、王五\n\n", emptySet())
        assertEquals(listOf("张三", "李四", "王五"), result.toInsert.map { it.name })
        assertEquals(2, result.skippedBlank)
    }

    @Test
    fun parseCommaList() {
        val result = RosterParser.parse("甲,乙，丙", emptySet())
        assertEquals(listOf("甲", "乙", "丙"), result.toInsert.map { it.name })
    }

    @Test
    fun parseNameAndStudentNo() {
        val space = RosterParser.parse("李四 2023002", emptySet()).toInsert.single()
        assertEquals("李四", space.name)
        assertEquals("2023002", space.studentNo)

        val comma = RosterParser.parse("王五,2023003", emptySet()).toInsert.single()
        assertEquals("王五", comma.name)
        assertEquals("2023003", comma.studentNo)
    }

    @Test
    fun parseStudentNoFirstAndTab() {
        val flipped = RosterParser.parse("2023003 王五", emptySet()).toInsert.single()
        assertEquals("王五", flipped.name)
        assertEquals("2023003", flipped.studentNo)

        val tab = RosterParser.parse("赵六\t2023004", emptySet()).toInsert.single()
        assertEquals("赵六", tab.name)
        assertEquals("2023004", tab.studentNo)
    }

    @Test
    fun skipExistingAndInternalDupes() {
        val result = RosterParser.parse("张三\n李四\n张三", setOf("张三"))
        assertEquals(listOf("李四"), result.toInsert.map { it.name })
        assertEquals(2, result.skippedDuplicate)
        assertTrue(result.conflicts.any { it.hint.contains("已在班") })
    }

    @Test
    fun duplicateStudentNoHint() {
        val existing = listOf(
            ExistingRosterPerson(1, "张三", "2023001", archived = false)
        )
        val result = RosterParser.parse("李四 2023001", existing)
        assertEquals(0, result.insertCount)
        assertEquals(1, result.skippedDuplicate)
        assertTrue(result.conflicts.single().hint.contains("学号 2023001"))
    }

    @Test
    fun archivedNameBecomesRestore() {
        val existing = listOf(
            ExistingRosterPerson(9, "王五", "2023003", archived = true)
        )
        val result = RosterParser.parse("王五 2023999", existing)
        assertEquals(0, result.insertCount)
        assertEquals(1, result.restoreCount)
        assertEquals(9L, result.toRestore.single().memberId)
        assertEquals("2023999", result.toRestore.single().studentNo)
    }

    @Test
    fun nameOnlyWhenSecondTokenNotStudentNo() {
        val result = RosterParser.parse("高三二班 值日组", emptySet()).toInsert.single()
        assertEquals("高三二班 值日组", result.name)
        assertNull(result.studentNo)
    }
}
