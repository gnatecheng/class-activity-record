package com.classrecord.app.data.csv

import android.content.Context
import com.classrecord.app.R
import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityDetail
import com.classrecord.app.i18n.AppStrings
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object Csv {
    fun escape(cell: String): String {
        return if (cell.contains(',') || cell.contains('"') || cell.contains('\n') || cell.contains('\r')) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
    }

    fun row(vararg cells: String): String = cells.joinToString(",") { escape(it) }

    fun withBom(body: String): ByteArray {
        return ("\uFEFF$body").toByteArray(Charsets.UTF_8)
    }

    fun members(list: List<Member>, context: Context, strings: AppStrings): String {
        val lines = mutableListOf(
            row(
                context.getString(R.string.csv_col_name),
                context.getString(R.string.csv_col_student_no),
                context.getString(R.string.csv_col_note),
                context.getString(R.string.csv_col_archived)
            )
        )
        list.forEach { member ->
            lines += row(
                member.name,
                member.studentNo.orEmpty(),
                member.note.orEmpty(),
                strings.yesNo(member.archived)
            )
        }
        return lines.joinToString("\n")
    }

    fun activityProgress(detail: ActivityDetail, context: Context, strings: AppStrings): String {
        val type = detail.activity.type
        val lines = mutableListOf(
            row(
                context.getString(R.string.csv_col_name),
                context.getString(R.string.csv_col_student_no),
                context.getString(R.string.csv_col_status),
                context.getString(R.string.csv_col_in_split),
                context.getString(R.string.csv_col_due),
                context.getString(R.string.csv_col_paid),
                context.getString(R.string.csv_col_note)
            )
        )
        detail.rows.forEach { row ->
            lines += row(
                row.name,
                row.studentNo.orEmpty(),
                strings.memberStatus(type, row.member.status),
                strings.yesNo(row.member.included),
                row.member.amountDue?.let { Money.formatFen(it) }.orEmpty(),
                row.member.amountPaid?.let { Money.formatFen(it) }.orEmpty(),
                row.member.note.orEmpty()
            )
        }
        return lines.joinToString("\n")
    }

    fun unfinished(detail: ActivityDetail, context: Context, strings: AppStrings): String {
        val type = detail.activity.type
        val pending = detail.rows.filter {
            it.member.status == MemberStatus.PENDING && it.member.included
        }
        val lines = mutableListOf(
            row(
                context.getString(R.string.csv_col_activity),
                context.getString(R.string.csv_col_name),
                context.getString(R.string.csv_col_student_no),
                context.getString(R.string.csv_col_due),
                context.getString(R.string.csv_col_paid),
                context.getString(R.string.csv_col_owed)
            )
        )
        pending.forEach { row ->
            val due = row.member.amountDue
            val paid = row.member.amountPaid
            val left = if (due != null || paid != null) {
                ((due ?: 0L) - (paid ?: 0L)).coerceAtLeast(0L)
            } else {
                null
            }
            lines += row(
                detail.activity.title,
                row.name,
                row.studentNo.orEmpty(),
                due?.let { Money.formatFen(it) }.orEmpty(),
                paid?.let { Money.formatFen(it) }.orEmpty(),
                left?.let { Money.formatFen(it) }.orEmpty()
            )
        }
        if (type == ActivityType.PAYMENT || type == ActivityType.SPLIT) {
            val dueSum = pending.sumOf { it.member.amountDue ?: 0L }
            val paidSum = pending.sumOf { it.member.amountPaid ?: 0L }
            val leftSum = (dueSum - paidSum).coerceAtLeast(0L)
            lines += row(
                context.getString(R.string.csv_total),
                "",
                "",
                Money.formatFen(dueSum),
                Money.formatFen(paidSum),
                Money.formatFen(leftSum)
            )
        }
        return lines.joinToString("\n")
    }

    fun allActivities(details: List<ActivityDetail>, context: Context, strings: AppStrings): String {
        val lines = mutableListOf(
            row(
                context.getString(R.string.csv_col_activity),
                context.getString(R.string.csv_col_type),
                context.getString(R.string.csv_col_scope),
                context.getString(R.string.csv_col_name),
                context.getString(R.string.csv_col_student_no),
                context.getString(R.string.csv_col_status),
                context.getString(R.string.csv_col_in_split),
                context.getString(R.string.csv_col_due),
                context.getString(R.string.csv_col_paid),
                context.getString(R.string.csv_col_note)
            )
        )
        details.forEach { detail ->
            val type = detail.activity.type
            detail.rows.forEach { row ->
                lines += row(
                    detail.activity.title,
                    strings.activityType(type),
                    detail.scopeLabel,
                    row.name,
                    row.studentNo.orEmpty(),
                    strings.memberStatus(type, row.member.status),
                    strings.yesNo(row.member.included),
                    row.member.amountDue?.let { Money.formatFen(it) }.orEmpty(),
                    row.member.amountPaid?.let { Money.formatFen(it) }.orEmpty(),
                    row.member.note.orEmpty()
                )
            }
        }
        return lines.joinToString("\n")
    }

    fun ledger(entries: List<LedgerEntry>, context: Context, strings: AppStrings): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val zone = ZoneId.systemDefault()
        val lines = mutableListOf(
            row(
                context.getString(R.string.csv_col_time),
                context.getString(R.string.csv_col_type),
                context.getString(R.string.csv_col_summary),
                context.getString(R.string.csv_col_amount),
                context.getString(R.string.csv_col_note)
            )
        )
        entries.forEach { entry ->
            val whenText = Instant.ofEpochMilli(entry.createdAt).atZone(zone).format(fmt)
            val signed = if (entry.type == LedgerType.INCOME) entry.amountFen else -entry.amountFen
            lines += row(
                whenText,
                strings.ledgerType(entry.type),
                entry.title,
                Money.formatFen(signed),
                entry.note.orEmpty()
            )
        }
        return lines.joinToString("\n")
    }
}
