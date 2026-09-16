package com.classrecord.app

import com.classrecord.app.data.LedgerMath
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import org.junit.Assert.assertEquals
import org.junit.Test

class LedgerMathTest {
    @Test
    fun balanceIncomeMinusExpense() {
        val entries = listOf(
            LedgerEntry(1, LedgerType.INCOME, 10000, "收", null, null, 0),
            LedgerEntry(2, LedgerType.EXPENSE, 2500, "支", null, null, 0),
            LedgerEntry(3, LedgerType.INCOME, 300, "零", null, null, 0)
        )
        assertEquals(7800L, LedgerMath.balanceFen(entries))
    }

    @Test
    fun paidTotalIgnoresNull() {
        assertEquals(1500L, LedgerMath.paidTotalFen(listOf(1000L, null, 500L, 0L)))
    }
}
