package com.classrecord.app

import com.classrecord.app.data.Money
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyTest {
    @Test
    fun parseYuan() {
        assertEquals(1000L, Money.parseYuanToFen("10"))
        assertEquals(1050L, Money.parseYuanToFen("10.5"))
        assertEquals(1250L, Money.parseYuanToFen("12.50"))
        assertEquals(1L, Money.parseYuanToFen("0.01"))
        assertEquals(1000L, Money.parseYuanToFen("￥10"))
        assertNull(Money.parseYuanToFen("abc"))
        assertNull(Money.parseYuanToFen("1.234"))
    }

    @Test
    fun formatFen() {
        assertEquals("10.00", Money.formatFen(1000))
        assertEquals("0.01", Money.formatFen(1))
        assertEquals("-1.25", Money.formatFen(-125))
    }

    @Test
    fun splitRemainderGoesToFirstPeople() {
        val parts = Money.split(1000, 3)
        assertEquals(listOf(334L, 333L, 333L), parts)
        assertEquals(1000L, parts.sum())
    }

    @Test
    fun splitExact() {
        val parts = Money.split(900, 3)
        assertEquals(listOf(300L, 300L, 300L), parts)
    }
}
