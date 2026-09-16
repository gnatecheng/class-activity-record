package com.classrecord.app.data

import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType

object LedgerMath {
    fun balanceFen(entries: List<LedgerEntry>): Long {
        var income = 0L
        var expense = 0L
        entries.forEach { entry ->
            when (entry.type) {
                LedgerType.INCOME -> income += entry.amountFen
                LedgerType.EXPENSE -> expense += entry.amountFen
            }
        }
        return income - expense
    }

    fun paidTotalFen(amountPaid: List<Long?>): Long =
        amountPaid.sumOf { it ?: 0L }
}
