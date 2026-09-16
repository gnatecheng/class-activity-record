package com.classrecord.app.data.csv

import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.repo.ActivityDetail
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

    fun members(list: List<Member>): String {
        val lines = mutableListOf(row("姓名", "学号", "备注", "已归档"))
        list.forEach { member ->
            lines += row(
                member.name,
                member.studentNo.orEmpty(),
                member.note.orEmpty(),
                if (member.archived) "是" else "否"
            )
        }
        return lines.joinToString("\n")
    }

    fun activityProgress(detail: ActivityDetail): String {
        val type = detail.activity.type
        val lines = mutableListOf(
            row("姓名", "学号", "状态", "参与分摊", "应缴（元）", "已缴（元）", "备注")
        )
        detail.rows.forEach { row ->
            lines += row(
                row.name,
                row.studentNo.orEmpty(),
                row.member.status.label(type),
                if (row.member.included) "是" else "否",
                row.member.amountDue?.let { Money.formatFen(it) }.orEmpty(),
                row.member.amountPaid?.let { Money.formatFen(it) }.orEmpty(),
                row.member.note.orEmpty()
            )
        }
        return lines.joinToString("\n")
    }

    fun allActivities(details: List<ActivityDetail>): String {
        val lines = mutableListOf(
            row("事务", "类型", "范围", "姓名", "学号", "状态", "参与分摊", "应缴（元）", "已缴（元）", "备注")
        )
        details.forEach { detail ->
            val type = detail.activity.type
            detail.rows.forEach { row ->
                lines += row(
                    detail.activity.title,
                    typeLabel(type),
                    detail.scopeLabel,
                    row.name,
                    row.studentNo.orEmpty(),
                    row.member.status.label(type),
                    if (row.member.included) "是" else "否",
                    row.member.amountDue?.let { Money.formatFen(it) }.orEmpty(),
                    row.member.amountPaid?.let { Money.formatFen(it) }.orEmpty(),
                    row.member.note.orEmpty()
                )
            }
        }
        return lines.joinToString("\n")
    }

    fun ledger(entries: List<LedgerEntry>): String {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val zone = ZoneId.systemDefault()
        val lines = mutableListOf(row("时间", "类型", "摘要", "金额（元）", "备注"))
        entries.forEach { entry ->
            val whenText = Instant.ofEpochMilli(entry.createdAt).atZone(zone).format(fmt)
            val signed = if (entry.type == LedgerType.INCOME) entry.amountFen else -entry.amountFen
            lines += row(
                whenText,
                if (entry.type == LedgerType.INCOME) "收入" else "支出",
                entry.title,
                Money.formatFen(signed),
                entry.note.orEmpty()
            )
        }
        return lines.joinToString("\n")
    }

    private fun typeLabel(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> "出勤"
        ActivityType.PAYMENT -> "缴费"
        ActivityType.SPLIT -> "费用分摊"
        ActivityType.CHECKLIST -> "清单"
    }

    private fun MemberStatus.label(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> when (this) {
            MemberStatus.PENDING -> "未到"
            MemberStatus.DONE -> "已到"
            MemberStatus.EXCUSED -> "请假"
        }
        ActivityType.PAYMENT, ActivityType.SPLIT -> when (this) {
            MemberStatus.DONE -> "已缴"
            else -> "未缴"
        }
        ActivityType.CHECKLIST -> when (this) {
            MemberStatus.DONE -> "已完成"
            else -> "未完成"
        }
    }
}
