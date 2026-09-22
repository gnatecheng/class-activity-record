package com.classrecord.app.data

import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.data.repo.ActivityMemberRow

object ActivityCopyText {
    fun summary(detail: ActivityDetail): String {
        val activity = detail.activity
        val lines = mutableListOf<String>()
        lines += "【${activity.title}】${detail.scopeLabel}"
        lines += "类型：${typeLabel(activity.type)}"
        lines += "进度：${detail.doneCount}/${detail.totalCount}"
        if (isMoney(activity.type)) {
            val due = detail.rows.sumOf { it.member.amountDue ?: 0L }
            val paid = detail.rows.sumOf { it.member.amountPaid ?: 0L }
            val remaining = (due - paid).coerceAtLeast(0L)
            lines += "应缴合计：${Money.formatYuan(due)}"
            lines += "已缴合计：${Money.formatYuan(paid)}"
            lines += "未缴合计：${Money.formatYuan(remaining)}"
        }
        val pending = pendingRows(detail)
        if (pending.isEmpty()) {
            lines += "未完成：无"
        } else {
            lines += "未完成："
            pending.forEach { row ->
                lines += "- ${rowLine(detail.activity.type, row, numbered = false)}"
            }
        }
        return lines.joinToString("\n")
    }

    /**
     * WeChat-ready reminder: one person per line, numbered,
     * amounts on the same line so a group chat paste stays readable.
     */
    fun reminder(detail: ActivityDetail): String {
        val activity = detail.activity
        val pending = pendingRows(detail)
        val verb = verb(activity.type)
        if (pending.isEmpty()) {
            return "【${activity.title}】${detail.scopeLabel}已全部完成，谢谢。"
        }
        val remainingFen = pending.sumOf { remainingFen(it) }
        return buildString {
            append("【催缴】${activity.title}\n")
            append("${detail.scopeLabel} · ${typeLabel(activity.type)}\n")
            append("还请尽快${verb}，谢谢。\n\n")
            append("未完成 ${pending.size} 人")
            if (isMoney(activity.type)) {
                append("，合计 ${Money.formatYuan(remainingFen)}")
            }
            append('\n')
            pending.forEachIndexed { index, row ->
                append("${index + 1}. ${rowLine(activity.type, row, numbered = true)}\n")
            }
        }.trimEnd()
    }

    fun unfinishedPlain(detail: ActivityDetail): String {
        val pending = pendingRows(detail)
        val header = "${detail.activity.title} 未完成名单"
        if (pending.isEmpty()) {
            return "$header\n（无）"
        }
        val lines = mutableListOf(header)
        pending.forEach { row ->
            val name = displayName(row)
            if (isMoney(detail.activity.type)) {
                lines += "$name\t${Money.formatFen(remainingFen(row))}"
            } else {
                lines += name
            }
        }
        if (isMoney(detail.activity.type)) {
            val total = pending.sumOf { remainingFen(it) }
            lines += "合计\t${Money.formatFen(total)}"
        }
        return lines.joinToString("\n")
    }

    fun nextPeriodTitle(currentTitle: String, dateLabel: String): String {
        val stripped = currentTitle.replace(Regex("""（\d{1,2}月\d{1,2}日）$"""), "").trim()
        return "$stripped（$dateLabel）"
    }

    private fun pendingRows(detail: ActivityDetail): List<ActivityMemberRow> {
        return detail.rows.filter { it.member.status == MemberStatus.PENDING && it.member.included }
    }

    private fun rowLine(type: ActivityType, row: ActivityMemberRow, numbered: Boolean): String {
        val name = displayName(row)
        return if (isMoney(type)) {
            val due = row.member.amountDue ?: 0L
            val paid = row.member.amountPaid ?: 0L
            val left = (due - paid).coerceAtLeast(0L)
            if (numbered) {
                "$name  尚欠 ${Money.formatYuan(left)}"
            } else {
                "$name 应缴 ${Money.formatYuan(due)}，已缴 ${Money.formatYuan(paid)}，尚欠 ${Money.formatYuan(left)}"
            }
        } else {
            name
        }
    }

    private fun displayName(row: ActivityMemberRow): String {
        val no = row.studentNo?.trim().orEmpty()
        return if (no.isNotEmpty()) "${row.name}（$no）" else row.name
    }

    private fun remainingFen(row: ActivityMemberRow): Long {
        val due = row.member.amountDue ?: 0L
        val paid = row.member.amountPaid ?: 0L
        return (due - paid).coerceAtLeast(0L)
    }

    private fun verb(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> "到场"
        ActivityType.PAYMENT, ActivityType.SPLIT -> "缴费"
        ActivityType.CHECKLIST -> "完成"
    }

    private fun isMoney(type: ActivityType): Boolean =
        type == ActivityType.PAYMENT || type == ActivityType.SPLIT

    private fun typeLabel(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> "出勤"
        ActivityType.PAYMENT -> "缴费"
        ActivityType.SPLIT -> "费用分摊"
        ActivityType.CHECKLIST -> "清单"
    }
}
