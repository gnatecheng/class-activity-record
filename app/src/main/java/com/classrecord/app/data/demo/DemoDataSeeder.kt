package com.classrecord.app.data.demo

import androidx.room.withTransaction
import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.repo.ActivityRepository
import com.classrecord.app.data.repo.ClassRepository
import com.classrecord.app.data.repo.LedgerRepository
import com.classrecord.app.data.repo.MemberRepository
import com.classrecord.app.data.repo.SubGroupRepository

/**
 * Inserts a one-time demo class when the database is still empty.
 * Does not modify existing user data.
 */
class DemoDataSeeder(
    private val db: AppDatabase,
    private val classRepository: ClassRepository,
    private val memberRepository: MemberRepository,
    private val subGroupRepository: SubGroupRepository,
    private val activityRepository: ActivityRepository,
    private val ledgerRepository: LedgerRepository,
) {
    suspend fun canSeed(): Boolean {
        if (classRepository.get() != null) return false
        if (memberRepository.getActive().isNotEmpty()) return false
        if (db.activityDao().getAll().isNotEmpty()) return false
        return true
    }

    suspend fun seedIfEmpty(english: Boolean): Boolean {
        if (!canSeed()) return false
        val pack = if (english) DemoContent.english else DemoContent.chinese
        db.withTransaction {
            classRepository.saveName(pack.className)
            val memberIds = pack.members.map { spec ->
                memberRepository.add(spec.name, spec.studentNo, null)
            }
            val subgroupIds = pack.subgroupMemberIndices.map { memberIds[it] }.toSet()
            val subgroupId = subGroupRepository.create(pack.subgroupName, null, subgroupIds)

            pack.activities.forEach { spec ->
                val scopeType = if (spec.scopeSubgroup) ScopeType.SUBGROUP else ScopeType.CLASS
                val subGroup = if (spec.scopeSubgroup) subgroupId else null
                val activityId = activityRepository.create(
                    scopeType = scopeType,
                    subGroupId = subGroup,
                    type = spec.type,
                    title = spec.title,
                    note = spec.note,
                    totalAmount = spec.totalSplitFen,
                    perPersonDue = spec.perPersonDueFen,
                    deadline = null,
                    splitPlan = null,
                )
                val rows = db.activityMemberDao().getForActivity(activityId)
                if (spec.paidCount != null && spec.type == ActivityType.PAYMENT) {
                    rows.take(spec.paidCount).forEach { row ->
                        activityRepository.updateAmounts(
                            row = row,
                            amountDue = row.amountDue,
                            amountPaid = row.amountDue,
                            note = row.note,
                            markPaid = true,
                        )
                    }
                }
                if (spec.doneCount != null &&
                    (spec.type == ActivityType.ATTENDANCE || spec.type == ActivityType.CHECKLIST)
                ) {
                    rows.take(spec.doneCount).forEach { row ->
                        activityRepository.setStatus(row, MemberStatus.DONE)
                    }
                }
            }

            ledgerRepository.add(
                type = LedgerType.INCOME,
                amountFen = 50_000L,
                title = pack.ledgerIncomeTitle,
                note = null,
            )
            ledgerRepository.add(
                type = LedgerType.EXPENSE,
                amountFen = 8_500L,
                title = pack.ledgerExpenseTitle,
                note = null,
            )
        }
        return true
    }
}
