package com.classrecord.app.data

import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityDetail

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
        val pending = detail.rows.filter { it.member.status == MemberStatus.PENDING && it.member.included }
        if (pending.isEmpty()) {
            lines += "未完成：无"
        } else {
            lines += "未完成："
            pending.forEach { row ->
                lines += if (isMoney(activity.type)) {
                    val due = row.member.amountDue ?: 0L
                    val paid = row.member.amountPaid ?: 0L
                    val left = (due - paid).coerceAtLeast(0L)
                    "- ${row.name} 应缴 ${Money.formatYuan(due)}，已缴 ${Money.formatYuan(paid)}，尚欠 ${Money.formatYuan(left)}"
                } else {
                    "- ${row.name}"
                }
            }
        }
        return lines.joinToString("\n")
    }

    fun reminder(detail: ActivityDetail): String {
        val activity = detail.activity
        val pending = detail.rows.filter { it.member.status == MemberStatus.PENDING && it.member.included }
        val verb = when (activity.type) {
            ActivityType.ATTENDANCE -> "到场"
            ActivityType.PAYMENT, ActivityType.SPLIT -> "缴费"
            ActivityType.CHECKLIST -> "完成"
        }
        if (pending.isEmpty()) {
            return "【${activity.title}】${detail.scopeLabel}已全部完成，谢谢。"
        }
        val names = pending.joinToString("、") { row ->
            if (isMoney(activity.type)) {
                val due = row.member.amountDue ?: 0L
                val paid = row.member.amountPaid ?: 0L
                val left = (due - paid).coerceAtLeast(0L)
                "${row.name}（${Money.formatYuan(left)}）"
            } else {
                row.name
            }
        }
        return buildString {
            append("【催缴】${activity.title}（${detail.scopeLabel}）\n")
            append("还请以下同学尽快${verb}：\n")
            append(names)
            append("\n谢谢！")
        }
    }

    fun nextPeriodTitle(currentTitle: String, dateLabel: String): String {
        val stripped = currentTitle.replace(Regex("""（\d{1,2}月\d{1,2}日）$"""), "").trim()
        return "$stripped（$dateLabel）"
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
