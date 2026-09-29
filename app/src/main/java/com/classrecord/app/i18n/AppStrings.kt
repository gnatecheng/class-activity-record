package com.classrecord.app.i18n

import android.content.Context
import com.classrecord.app.R
import com.classrecord.app.data.Money
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.entity.ActivityEntity

class AppStrings(private val context: Context) {
    private fun s(id: Int, vararg args: Any): String = context.getString(id, *args)

    fun scopeLabel(activity: ActivityEntity, groupNames: Map<Long, String>): String =
        if (activity.scopeType == ScopeType.CLASS) {
            s(R.string.scope_whole_class)
        } else {
            groupNames[activity.subGroupId] ?: s(R.string.scope_subgroup_fallback)
        }

    fun deletedMember(): String = s(R.string.deleted_member)

    fun activityType(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> s(R.string.type_attendance)
        ActivityType.PAYMENT -> s(R.string.type_payment)
        ActivityType.SPLIT -> s(R.string.type_split)
        ActivityType.CHECKLIST -> s(R.string.type_checklist)
    }

    fun memberStatus(type: ActivityType, status: MemberStatus): String = when (type) {
        ActivityType.ATTENDANCE -> when (status) {
            MemberStatus.PENDING -> s(R.string.status_pending_attendance)
            MemberStatus.DONE -> s(R.string.status_done_attendance)
            MemberStatus.EXCUSED -> s(R.string.status_excused)
        }
        ActivityType.PAYMENT, ActivityType.SPLIT -> when (status) {
            MemberStatus.DONE -> s(R.string.status_paid)
            else -> s(R.string.status_unpaid)
        }
        ActivityType.CHECKLIST -> when (status) {
            MemberStatus.DONE -> s(R.string.status_done_checklist)
            else -> s(R.string.status_pending_checklist)
        }
    }

    fun ledgerType(type: LedgerType): String = when (type) {
        LedgerType.INCOME -> s(R.string.ledger_income)
        LedgerType.EXPENSE -> s(R.string.ledger_expense)
    }

    fun yesNo(yes: Boolean): String = if (yes) s(R.string.yes) else s(R.string.no)

    fun unfinishedHint(type: ActivityType, count: Int): String {
        if (count <= 0) return s(R.string.hint_all_done)
        return when (type) {
            ActivityType.ATTENDANCE -> s(R.string.hint_attendance_unfinished, count)
            ActivityType.PAYMENT, ActivityType.SPLIT -> s(R.string.hint_payment_unfinished, count)
            ActivityType.CHECKLIST -> s(R.string.hint_checklist_unfinished, count)
        }
    }

    fun copyVerb(type: ActivityType): String = when (type) {
        ActivityType.ATTENDANCE -> s(R.string.copy_verb_attend)
        ActivityType.PAYMENT, ActivityType.SPLIT -> s(R.string.copy_verb_pay)
        ActivityType.CHECKLIST -> s(R.string.copy_verb_complete)
    }

    fun errTitleRequired(): String = s(R.string.err_title_required)
    fun errNoScopeMembers(): String = s(R.string.err_no_scope_members)
    fun errInvalidDue(): String = s(R.string.err_invalid_due)
    fun errInvalidSplit(): String = s(R.string.err_invalid_split)
    fun errSplitNeedOne(): String = s(R.string.err_split_need_one)
    fun errActivityMissing(): String = s(R.string.err_activity_missing)
    fun errSplitOnly(): String = s(R.string.err_split_only)
    fun errNoMemberRows(): String = s(R.string.err_no_member_rows)
    fun errSourceMissing(): String = s(R.string.err_source_missing)
    fun errLedgerAmount(): String = s(R.string.err_ledger_amount)
    fun errLedgerSummary(): String = s(R.string.err_ledger_summary)
    fun errAlreadyInLedger(): String = s(R.string.err_already_in_ledger)
    fun errNoPaidYet(): String = s(R.string.err_no_paid_yet)
    fun errWriteFile(): String = s(R.string.err_write_file)
    fun errReadBackup(): String = s(R.string.err_read_backup)
    fun errNotBackup(): String = s(R.string.err_not_backup)
    fun errNoDataInBackup(): String = s(R.string.err_no_data_in_backup)
    fun errWriteBackup(): String = s(R.string.err_write_backup)
    fun errReadImage(): String = s(R.string.err_read_image)
    fun errParseImage(): String = s(R.string.err_parse_image)
    fun errAmountDueFormat(): String = s(R.string.err_amount_due_format)
    fun errAmountPaidFormat(): String = s(R.string.err_amount_paid_format)
    fun errInvalidTotal(): String = s(R.string.err_invalid_amount)

    fun ledgerFromActivity(scopeLabel: String): String = s(R.string.ledger_from_activity, scopeLabel)

    fun ledgerPaidTotalTitle(title: String): String = s(R.string.ledger_paid_total_title, title)

    fun backupFileName(): String = s(R.string.settings_backup_file)

    fun rosterDup(label: String): String = s(R.string.roster_dup, label)

    fun rosterStudentTaken(studentNo: String, otherName: String): String =
        s(R.string.roster_student_taken, studentNo, otherName)

    fun rosterAlready(name: String, extra: String): String = s(R.string.roster_already, name, extra)

    fun rosterStudentNoParen(studentNo: String): String = s(R.string.roster_student_no_paren, studentNo)

    fun rosterDisplayLabel(name: String, studentNo: String?): String =
        if (studentNo != null) s(R.string.roster_label_with_no, name, studentNo) else name

    fun importResultMessage(added: Int, restored: Int): String = when {
        added == 0 && restored == 0 -> s(R.string.import_none)
        restored == 0 -> s(R.string.import_added, added)
        added == 0 -> s(R.string.import_restored_only, restored)
        else -> s(R.string.import_both, added, restored)
    }

    fun exportFilenameProgress(title: String): String = s(R.string.export_filename_progress, title)

    fun exportFilenameUnfinishedCsv(title: String): String = s(R.string.export_filename_unfinished_csv, title)

    fun exportFilenameUnfinishedTxt(title: String): String = s(R.string.export_filename_unfinished_txt, title)

    fun errInvalidPerPerson(): String = s(R.string.err_invalid_per_person)

    fun errInvalidSplitTotal(): String = s(R.string.err_invalid_split_total)

    fun errLedgerInvalidAmount(): String = s(R.string.err_invalid_amount)

    fun formatMoney(fen: Long): String = s(R.string.money_yuan, Money.formatFen(fen))
}
