package com.classrecord.app.data

import android.content.Context
import com.classrecord.app.R
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.data.repo.ActivityMemberRow
import com.classrecord.app.i18n.AppStrings

object ActivityCopyText {
    fun summary(detail: ActivityDetail, strings: AppStrings, context: Context): String {
        val activity = detail.activity
        val lines = mutableListOf<String>()
        lines += "【${activity.title}】${detail.scopeLabel}"
        lines += context.getString(R.string.copy_type_line, strings.activityType(activity.type))
        lines += context.getString(
            R.string.copy_progress_line,
            detail.doneCount,
            detail.totalCount
        )
        if (isMoney(activity.type)) {
            val due = detail.rows.sumOf { it.member.amountDue ?: 0L }
            val paid = detail.rows.sumOf { it.member.amountPaid ?: 0L }
            val remaining = (due - paid).coerceAtLeast(0L)
            lines += context.getString(R.string.copy_due_total, formatMoney(context, due))
            lines += context.getString(R.string.copy_paid_total, formatMoney(context, paid))
            lines += context.getString(R.string.copy_remaining_total, formatMoney(context, remaining))
        }
        val pending = pendingRows(detail)
        if (pending.isEmpty()) {
            lines += context.getString(R.string.copy_unfinished_none)
        } else {
            lines += context.getString(R.string.copy_unfinished_colon)
            pending.forEach { row ->
                lines += "- ${rowLine(context, strings, detail.activity.type, row, numbered = false)}"
            }
        }
        return lines.joinToString("\n")
    }

    fun reminder(detail: ActivityDetail, strings: AppStrings, context: Context): String {
        val activity = detail.activity
        val pending = pendingRows(detail)
        if (pending.isEmpty()) {
            return context.getString(
                R.string.copy_all_done,
                activity.title,
                detail.scopeLabel
            )
        }
        val remainingFen = pending.sumOf { remainingFen(it) }
        return buildString {
            append(context.getString(R.string.copy_reminder_header, activity.title))
            append('\n')
            append("${detail.scopeLabel} · ${strings.activityType(activity.type)}\n")
            append(context.getString(R.string.copy_please_verb, strings.copyVerb(activity.type)))
            append("\n\n")
            append(context.getString(R.string.copy_unfinished_count, pending.size))
            if (isMoney(activity.type)) {
                append(context.getString(R.string.copy_unfinished_with_total, formatMoney(context, remainingFen)))
            }
            append('\n')
            pending.forEachIndexed { index, row ->
                append("${index + 1}. ${rowLine(context, strings, activity.type, row, numbered = true)}\n")
            }
        }.trimEnd()
    }

    fun unfinishedPlain(detail: ActivityDetail, context: Context): String {
        val pending = pendingRows(detail)
        val header = context.getString(R.string.copy_unfinished_list_title, detail.activity.title)
        if (pending.isEmpty()) {
            return "$header\n${context.getString(R.string.copy_none_paren)}"
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
            lines += "${context.getString(R.string.csv_total)}\t${Money.formatFen(total)}"
        }
        return lines.joinToString("\n")
    }

    fun nextPeriodTitle(currentTitle: String, dateLabel: String, english: Boolean): String {
        val stripped = if (english) {
            currentTitle.replace(Regex("""\([A-Za-z]{3,9} \d{1,2}\)$"""), "").trim()
        } else {
            currentTitle.replace(Regex("""（\d{1,2}月\d{1,2}日）$"""), "").trim()
        }
        val wrapper = if (english) "($dateLabel)" else "（$dateLabel）"
        return "$stripped$wrapper"
    }

    private fun pendingRows(detail: ActivityDetail): List<ActivityMemberRow> {
        return detail.rows.filter { it.member.status == MemberStatus.PENDING && it.member.included }
    }

    private fun rowLine(
        context: Context,
        strings: AppStrings,
        type: ActivityType,
        row: ActivityMemberRow,
        numbered: Boolean
    ): String {
        val name = displayName(row)
        return if (isMoney(type)) {
            val due = row.member.amountDue ?: 0L
            val paid = row.member.amountPaid ?: 0L
            val left = (due - paid).coerceAtLeast(0L)
            if (numbered) {
                context.getString(R.string.copy_owed, formatMoney(context, left))
                    .let { "$name  $it" }
            } else {
                context.getString(
                    R.string.copy_row_money,
                    name,
                    formatMoney(context, due),
                    formatMoney(context, paid),
                    formatMoney(context, left)
                )
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

    private fun isMoney(type: ActivityType): Boolean =
        type == ActivityType.PAYMENT || type == ActivityType.SPLIT

    private fun formatMoney(context: Context, fen: Long): String =
        context.getString(R.string.money_yuan, Money.formatFen(fen))
}
