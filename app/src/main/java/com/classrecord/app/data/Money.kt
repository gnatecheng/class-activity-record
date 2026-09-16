package com.classrecord.app.data

import java.util.Locale
import kotlin.math.abs

object Money {
    /** Parse a yuan string such as "12.5" or "￥10" into fen (cents). */
    fun parseYuanToFen(input: String): Long? {
        val s = input.trim()
            .replace("￥", "")
            .replace("¥", "")
            .replace("元", "")
            .replace(",", "")
            .replace(" ", "")
        if (s.isEmpty()) return null
        if (!Regex("""^-?\d+(\.\d{1,2})?$""").matches(s)) return null
        val negative = s.startsWith("-")
        val unsigned = s.removePrefix("-")
        val parts = unsigned.split('.')
        val yuan = parts[0].toLong()
        val fenPart = when {
            parts.size == 1 -> 0L
            parts[1].length == 1 -> parts[1].toLong() * 10
            else -> parts[1].padEnd(2, '0').take(2).toLong()
        }
        val total = yuan * 100 + fenPart
        return if (negative) -total else total
    }

    fun formatFen(fen: Long): String {
        val sign = if (fen < 0) "-" else ""
        val absFen = abs(fen)
        val yuan = absFen / 100
        val cents = absFen % 100
        return sign + String.format(Locale.CHINA, "%d.%02d", yuan, cents)
    }

    fun formatYuan(fen: Long): String = "${formatFen(fen)} 元"

    /**
     * Split [totalFen] across [count] people. The first [remainder] people
     * receive +1 fen so the parts sum exactly to the total.
     */
    fun split(totalFen: Long, count: Int): List<Long> {
        require(count > 0) { "count must be > 0" }
        require(totalFen >= 0) { "totalFen must be >= 0" }
        val base = totalFen / count
        val remainder = (totalFen % count).toInt()
        return List(count) { index -> base + if (index < remainder) 1L else 0L }
    }

    /**
     * Split [totalFen] by integer weights. Leftover fen (from integer
     * division) go to the first people, matching [split].
     */
    fun splitByWeight(totalFen: Long, weights: List<Int>): List<Long> {
        require(weights.isNotEmpty()) { "weights must not be empty" }
        require(weights.all { it > 0 }) { "weights must be > 0" }
        require(totalFen >= 0) { "totalFen must be >= 0" }
        val totalWeight = weights.sum()
        val raw = weights.map { weight -> totalFen * weight / totalWeight }
        var leftover = totalFen - raw.sum()
        return raw.map { value ->
            val extra = if (leftover > 0) 1L else 0L
            leftover -= extra
            value + extra
        }
    }
}

data class SplitShare(
    val memberId: Long,
    val included: Boolean = true,
    val weight: Int = 1,
    val customDueFen: Long? = null
)

object SplitPlan {
    /**
     * Assign exact fen amounts. Excluded people get 0. Custom overrides
     * are taken first; the remainder is split by weight among everyone
     * else who is included.
     */
    fun amounts(totalFen: Long, shares: List<SplitShare>): List<Long> {
        require(totalFen >= 0) { "totalFen must be >= 0" }
        val result = MutableList(shares.size) { 0L }
        if (shares.isEmpty()) return result
        var reserved = 0L
        shares.forEachIndexed { index, share ->
            if (share.included && share.customDueFen != null) {
                val due = share.customDueFen.coerceAtLeast(0L)
                result[index] = due
                reserved += due
            }
        }
        val remaining = (totalFen - reserved).coerceAtLeast(0L)
        val auto = shares.mapIndexedNotNull { index, share ->
            if (share.included && share.customDueFen == null) index else null
        }
        if (auto.isEmpty()) return result
        val weights = auto.map { shares[it].weight.coerceAtLeast(1) }
        val parts = Money.splitByWeight(remaining, weights)
        auto.forEachIndexed { j, index -> result[index] = parts[j] }
        return result
    }
}
