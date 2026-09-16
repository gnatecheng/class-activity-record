package com.classrecord.app.data.repo

import androidx.room.withTransaction
import com.classrecord.app.data.LedgerMath
import com.classrecord.app.data.Money
import com.classrecord.app.data.ParsedMember
import com.classrecord.app.data.SplitPlan
import com.classrecord.app.data.SplitShare
import com.classrecord.app.data.db.AppDatabase
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.ClassProfile
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType
import com.classrecord.app.data.entity.SubGroup
import com.classrecord.app.data.entity.SubGroupMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class ActivityListItem(
    val activity: ActivityEntity,
    val scopeLabel: String,
    val doneCount: Int,
    val totalCount: Int,
    val unfinishedCount: Int
)

data class SubGroupWithMembers(
    val group: SubGroup,
    val memberIds: Set<Long>,
    val memberNames: List<String>
)

data class ActivityDetail(
    val activity: ActivityEntity,
    val scopeLabel: String,
    val rows: List<ActivityMemberRow>
) {
    val participating: List<ActivityMemberRow> get() = rows.filter { it.member.included }
    val totalCount: Int get() = participating.size
    val doneCount: Int get() = participating.count { it.member.status != MemberStatus.PENDING }
    val unfinishedCount: Int get() = participating.count { it.member.status == MemberStatus.PENDING }
}

data class ActivityMemberRow(
    val member: ActivityMember,
    val name: String,
    val studentNo: String?
)

class ClassRepository(private val db: AppDatabase) {
    private val dao = db.classProfileDao()

    fun observe(): Flow<ClassProfile?> = dao.observe()

    suspend fun get(): ClassProfile? = dao.get()

    suspend fun saveName(name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        dao.upsert(
            ClassProfile(
                id = 1L,
                name = trimmed,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

class MemberRepository(private val db: AppDatabase) {
    private val dao = db.memberDao()

    fun observeAll(): Flow<List<Member>> = dao.observeAll()

    fun observeActive(): Flow<List<Member>> = dao.observeActive()

    fun observeActiveCount(): Flow<Int> = dao.observeActiveCount()

    suspend fun getActive(): List<Member> = dao.getActive()

    suspend fun getById(id: Long): Member? = dao.getById(id)

    suspend fun add(name: String, studentNo: String?, note: String?): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        return dao.insert(
            Member(
                name = trimmed,
                studentNo = studentNo?.trim()?.ifBlank { null },
                note = note?.trim()?.ifBlank { null },
                archived = false
            )
        )
    }

    suspend fun update(member: Member) {
        require(member.name.trim().isNotEmpty())
        dao.update(
            member.copy(
                name = member.name.trim(),
                studentNo = member.studentNo?.trim()?.ifBlank { null },
                note = member.note?.trim()?.ifBlank { null }
            )
        )
    }

    suspend fun setArchived(member: Member, archived: Boolean) {
        dao.update(member.copy(archived = archived))
    }

    suspend fun addAll(parsed: List<ParsedMember>): Int {
        if (parsed.isEmpty()) return 0
        val rows = parsed.map {
            Member(
                name = it.name.trim(),
                studentNo = it.studentNo?.trim()?.ifBlank { null },
                archived = false
            )
        }
        return db.withTransaction {
            dao.insertAll(rows).size
        }
    }
}

class SubGroupRepository(private val db: AppDatabase) {
    private val groupDao = db.subGroupDao()
    private val linkDao = db.subGroupMemberDao()
    private val memberDao = db.memberDao()

    fun observeAll(): Flow<List<SubGroup>> = groupDao.observeAll()

    fun observeActive(): Flow<List<SubGroup>> = groupDao.observeActive()

    fun observeActiveCount(): Flow<Int> = groupDao.observeActiveCount()

    fun observeWithMembers(): Flow<List<SubGroupWithMembers>> {
        return combine(
            groupDao.observeAll(),
            linkDao.observeAll(),
            memberDao.observeAll()
        ) { groups, links, members ->
            val names = members.associate { it.id to it.name }
            val byGroup = links.groupBy { it.subGroupId }
            groups.map { group ->
                val ids = byGroup[group.id].orEmpty().map { it.memberId }.toSet()
                SubGroupWithMembers(
                    group = group,
                    memberIds = ids,
                    memberNames = ids.mapNotNull { names[it] }
                )
            }
        }
    }

    suspend fun getById(id: Long): SubGroup? = groupDao.getById(id)

    fun observeMemberIds(groupId: Long): Flow<List<Long>> = linkDao.observeMemberIds(groupId)

    suspend fun getMemberIds(groupId: Long): List<Long> = linkDao.getMemberIds(groupId)

    suspend fun create(name: String, note: String?, memberIds: Collection<Long>): Long {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty())
        val now = System.currentTimeMillis()
        return db.withTransaction {
            val id = groupDao.insert(
                SubGroup(
                    name = trimmed,
                    note = note?.trim()?.ifBlank { null },
                    archived = false,
                    createdAt = now
                )
            )
            if (memberIds.isNotEmpty()) {
                linkDao.insertAll(memberIds.distinct().map { SubGroupMember(id, it) })
            }
            id
        }
    }

    suspend fun update(group: SubGroup, memberIds: Collection<Long>) {
        require(group.name.trim().isNotEmpty())
        db.withTransaction {
            groupDao.update(
                group.copy(
                    name = group.name.trim(),
                    note = group.note?.trim()?.ifBlank { null }
                )
            )
            linkDao.deleteForGroup(group.id)
            if (memberIds.isNotEmpty()) {
                linkDao.insertAll(memberIds.distinct().map { SubGroupMember(group.id, it) })
            }
        }
    }

    suspend fun setArchived(group: SubGroup, archived: Boolean) {
        groupDao.update(group.copy(archived = archived))
    }

    suspend fun activeMemberCount(groupId: Long): Int {
        val ids = linkDao.getMemberIds(groupId).toSet()
        return memberDao.getActive().count { it.id in ids }
    }
}

class ActivityRepository(private val db: AppDatabase) {
    private val activityDao = db.activityDao()
    private val rowDao = db.activityMemberDao()
    private val memberDao = db.memberDao()
    private val groupDao = db.subGroupDao()
    private val linkDao = db.subGroupMemberDao()

    fun observeList(): Flow<List<ActivityListItem>> {
        return observeListFrom(activityDao.observeAll())
    }

    fun observeListIncludingArchived(): Flow<List<ActivityListItem>> {
        return observeListFrom(activityDao.observeAllIncludingArchived())
    }

    private fun observeListFrom(source: Flow<List<ActivityEntity>>): Flow<List<ActivityListItem>> {
        return combine(
            source,
            rowDao.observeAll(),
            groupDao.observeAll()
        ) { activities, rows, groups ->
            toListItems(activities, rows, groups)
        }
    }

    suspend fun snapshotList(): List<ActivityListItem> {
        return toListItems(activityDao.getAll(), rowDao.getAll(), groupDao.getAll())
    }

    suspend fun setArchived(activityId: Long, archived: Boolean) {
        val current = activityDao.getById(activityId) ?: return
        activityDao.update(
            current.copy(archived = archived, updatedAt = System.currentTimeMillis())
        )
    }

    private fun toListItems(
        activities: List<ActivityEntity>,
        rows: List<ActivityMember>,
        groups: List<SubGroup>
    ): List<ActivityListItem> {
        val groupNames = groups.associate { it.id to it.name }
        val byActivity = rows.groupBy { it.activityId }
        return activities.map { activity ->
            val members = byActivity[activity.id].orEmpty()
            val participating = members.filter { it.included }
            val unfinished = participating.count { it.status == MemberStatus.PENDING }
            ActivityListItem(
                activity = activity,
                scopeLabel = scopeLabel(activity, groupNames),
                doneCount = participating.size - unfinished,
                totalCount = participating.size,
                unfinishedCount = unfinished
            )
        }
    }

    fun observeDetail(activityId: Long): Flow<ActivityDetail?> {
        return combine(
            activityDao.observeById(activityId),
            rowDao.observeForActivity(activityId),
            memberDao.observeAll(),
            groupDao.observeAll()
        ) { activity, rows, members, groups ->
            if (activity == null) return@combine null
            val memberMap = members.associateBy { it.id }
            val groupNames = groups.associate { it.id to it.name }
            ActivityDetail(
                activity = activity,
                scopeLabel = scopeLabel(activity, groupNames),
                rows = rows.map { row ->
                    val member = memberMap[row.memberId]
                    ActivityMemberRow(
                        member = row,
                        name = member?.name ?: "已删除成员",
                        studentNo = member?.studentNo
                    )
                }
            )
        }
    }

    /**
     * Snapshot current roster into ActivityMember rows. Later subgroup
     * membership edits must not change this historical set.
     */
    suspend fun create(
        scopeType: ScopeType,
        subGroupId: Long?,
        type: ActivityType,
        title: String,
        note: String?,
        totalAmount: Long?,
        perPersonDue: Long?,
        deadline: Long?,
        splitPlan: List<SplitShare>? = null
    ): Long {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "请填写标题" }
        val members = resolveScopeMembers(scopeType, subGroupId)
        require(members.isNotEmpty()) { "范围没有可用成员，无法创建事务" }

        val dues: List<Long?> = when (type) {
            ActivityType.PAYMENT -> {
                val due = perPersonDue ?: 0L
                require(due >= 0L) { "应缴金额无效" }
                members.map { due }
            }
            ActivityType.SPLIT -> {
                val total = totalAmount ?: 0L
                require(total >= 0L) { "分摊总额无效" }
                val plan = alignedPlan(members, splitPlan)
                val includedCount = plan.count { it.included }
                require(includedCount > 0) { "请至少选择一名参与分摊的同学" }
                SplitPlan.amounts(total, plan)
            }
            else -> members.map { null }
        }

        val now = System.currentTimeMillis()
        return db.withTransaction {
            val id = activityDao.insert(
                ActivityEntity(
                    scopeType = scopeType,
                    subGroupId = if (scopeType == ScopeType.SUBGROUP) subGroupId else null,
                    type = type,
                    title = trimmed,
                    note = note?.trim()?.ifBlank { null },
                    totalAmount = if (type == ActivityType.SPLIT) totalAmount else null,
                    deadline = deadline,
                    archived = false,
                    createdAt = now,
                    updatedAt = now
                )
            )
            rowDao.insertAll(
                members.mapIndexed { index, member ->
                    val share = if (type == ActivityType.SPLIT) {
                        alignedPlan(members, splitPlan)[index]
                    } else {
                        SplitShare(member.id)
                    }
                    ActivityMember(
                        activityId = id,
                        memberId = member.id,
                        status = MemberStatus.PENDING,
                        amountDue = dues[index],
                        amountPaid = null,
                        note = null,
                        included = share.included,
                        weight = share.weight.coerceAtLeast(1),
                        attachmentPath = null,
                        attachmentMime = null,
                        updatedAt = now
                    )
                }
            )
            id
        }
    }

    suspend fun toggleAttendance(row: ActivityMember) {
        val next = when (row.status) {
            MemberStatus.PENDING -> MemberStatus.DONE
            MemberStatus.DONE -> MemberStatus.PENDING
            MemberStatus.EXCUSED -> MemberStatus.PENDING
        }
        rowDao.update(row.copy(status = next, updatedAt = System.currentTimeMillis()))
    }

    suspend fun markExcused(row: ActivityMember) {
        val next = if (row.status == MemberStatus.EXCUSED) {
            MemberStatus.PENDING
        } else {
            MemberStatus.EXCUSED
        }
        rowDao.update(row.copy(status = next, updatedAt = System.currentTimeMillis()))
    }

    suspend fun setStatus(row: ActivityMember, status: MemberStatus) {
        rowDao.update(row.copy(status = status, updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleChecklist(row: ActivityMember) {
        val next = if (row.status == MemberStatus.DONE) {
            MemberStatus.PENDING
        } else {
            MemberStatus.DONE
        }
        rowDao.update(row.copy(status = next, updatedAt = System.currentTimeMillis()))
    }

    suspend fun togglePaid(row: ActivityMember) {
        val now = System.currentTimeMillis()
        if (row.status == MemberStatus.DONE) {
            rowDao.update(row.copy(status = MemberStatus.PENDING, updatedAt = now))
        } else {
            val paid = if (row.amountPaid == null || row.amountPaid == 0L) {
                row.amountDue
            } else {
                row.amountPaid
            }
            rowDao.update(
                row.copy(
                    status = MemberStatus.DONE,
                    amountPaid = paid,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun updateAmounts(
        row: ActivityMember,
        amountDue: Long?,
        amountPaid: Long?,
        note: String?,
        markPaid: Boolean?
    ) {
        val now = System.currentTimeMillis()
        val status = when (markPaid) {
            true -> MemberStatus.DONE
            false -> MemberStatus.PENDING
            null -> row.status
        }
        val paid = when {
            markPaid == true && (amountPaid == null || amountPaid == 0L) -> amountDue
            else -> amountPaid
        }
        rowDao.update(
            row.copy(
                status = status,
                amountDue = amountDue,
                amountPaid = paid,
                note = note?.trim()?.ifBlank { null },
                updatedAt = now
            )
        )
    }

    suspend fun applySplitPlan(activityId: Long, totalFen: Long, shares: List<SplitShare>) {
        val activity = activityDao.getById(activityId) ?: error("事务不存在")
        require(activity.type == ActivityType.SPLIT) { "只有分摊事务可以调整份额" }
        require(totalFen >= 0L) { "分摊总额无效" }
        val rows = rowDao.getForActivity(activityId)
        require(rows.isNotEmpty()) { "没有成员快照" }
        val byId = shares.associateBy { it.memberId }
        val plan = rows.map { row ->
            byId[row.memberId] ?: SplitShare(row.memberId, row.included, row.weight)
        }
        require(plan.any { it.included }) { "请至少选择一名参与分摊的同学" }
        val dues = SplitPlan.amounts(totalFen, plan)
        val now = System.currentTimeMillis()
        db.withTransaction {
            activityDao.update(activity.copy(totalAmount = totalFen, updatedAt = now))
            rows.forEachIndexed { index, row ->
                val share = plan[index]
                rowDao.update(
                    row.copy(
                        included = share.included,
                        weight = share.weight.coerceAtLeast(1),
                        amountDue = dues[index],
                        updatedAt = now
                    )
                )
            }
        }
    }

    suspend fun setAttachment(row: ActivityMember, path: String?, mime: String?) {
        rowDao.update(
            row.copy(
                attachmentPath = path,
                attachmentMime = mime,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun snapshot(activityId: Long): ActivityDetail? {
        val activity = activityDao.getById(activityId) ?: return null
        val rows = rowDao.getForActivity(activityId)
        val members = memberDao.getAll().associateBy { it.id }
        val groups = groupDao.getAll().associate { it.id to it.name }
        return ActivityDetail(
            activity = activity,
            scopeLabel = scopeLabel(activity, groups),
            rows = rows.map { row ->
                val member = members[row.memberId]
                ActivityMemberRow(
                    member = row,
                    name = member?.name ?: "已删除成员",
                    studentNo = member?.studentNo
                )
            }
        )
    }

    suspend fun snapshotAll(): List<ActivityDetail> {
        return activityDao.getAll().mapNotNull { snapshot(it.id) }
    }

    private fun alignedPlan(members: List<Member>, splitPlan: List<SplitShare>?): List<SplitShare> {
        if (splitPlan == null) {
            return members.map { SplitShare(it.id) }
        }
        val byId = splitPlan.associateBy { it.memberId }
        return members.map { member -> byId[member.id] ?: SplitShare(member.id) }
    }

    private suspend fun resolveScopeMembers(
        scopeType: ScopeType,
        subGroupId: Long?
    ): List<Member> {
        val active = memberDao.getActive()
        return when (scopeType) {
            ScopeType.CLASS -> active
            ScopeType.SUBGROUP -> {
                val groupId = subGroupId ?: return emptyList()
                val ids = linkDao.getMemberIds(groupId).toSet()
                active.filter { it.id in ids }
            }
        }
    }

    suspend fun reuse(
        sourceId: Long,
        title: String
    ): Long {
        val activity = activityDao.getById(sourceId) ?: error("原事务不存在")
        val oldRows = rowDao.getForActivity(sourceId)
        val perPerson = when (activity.type) {
            ActivityType.PAYMENT -> {
                oldRows.mapNotNull { it.amountDue }
                    .groupingBy { it }
                    .eachCount()
                    .maxByOrNull { it.value }
                    ?.key ?: 0L
            }
            else -> null
        }
        return create(
            scopeType = activity.scopeType,
            subGroupId = activity.subGroupId,
            type = activity.type,
            title = title,
            note = activity.note,
            totalAmount = if (activity.type == ActivityType.SPLIT) activity.totalAmount else null,
            perPersonDue = perPerson,
            deadline = null
        )
    }

    private fun scopeLabel(
        activity: ActivityEntity,
        groupNames: Map<Long, String>
    ): String {
        return if (activity.scopeType == ScopeType.CLASS) {
            "全班"
        } else {
            groupNames[activity.subGroupId] ?: "小团体"
        }
    }
}

class LedgerRepository(private val db: AppDatabase) {
    private val dao = db.ledgerDao()

    fun observeAll(): Flow<List<LedgerEntry>> = dao.observeAll()

    fun observeBalanceFen(): Flow<Long> =
        dao.observeAll().map { LedgerMath.balanceFen(it) }

    fun observeHasIncome(activityId: Long): Flow<Boolean> =
        dao.observeIncomeCount(activityId).map { it > 0 }

    suspend fun add(
        type: LedgerType,
        amountFen: Long,
        title: String,
        note: String?,
        relatedActivityId: Long? = null
    ): Long {
        require(amountFen > 0L) { "金额必须大于 0" }
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "请填写摘要" }
        return dao.insert(
            LedgerEntry(
                type = type,
                amountFen = amountFen,
                title = trimmed,
                note = note?.trim()?.ifBlank { null },
                relatedActivityId = relatedActivityId,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun recordPaidTotal(detail: ActivityDetail): Long {
        if (dao.incomeCount(detail.activity.id) > 0) {
            error("该事务已记入班费")
        }
        val paid = LedgerMath.paidTotalFen(detail.rows.map { it.member.amountPaid })
        if (paid <= 0L) error("还没有已收金额")
        return add(
            type = LedgerType.INCOME,
            amountFen = paid,
            title = "${detail.activity.title} 已收合计",
            note = "来自${detail.scopeLabel}事务",
            relatedActivityId = detail.activity.id
        )
    }

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
