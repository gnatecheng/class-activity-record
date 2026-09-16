package com.classrecord.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "class_profile")
data class ClassProfile(
    @PrimaryKey val id: Long = 1L,
    val name: String,
    val updatedAt: Long
)

@Entity(
    tableName = "members",
    indices = [Index("archived"), Index("name")]
)
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val studentNo: String? = null,
    val note: String? = null,
    val archived: Boolean = false
)

@Entity(
    tableName = "subgroups",
    indices = [Index("archived")]
)
data class SubGroup(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val note: String? = null,
    val archived: Boolean = false,
    val createdAt: Long
)

@Entity(
    tableName = "subgroup_members",
    primaryKeys = ["subGroupId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = SubGroup::class,
            parentColumns = ["id"],
            childColumns = ["subGroupId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["id"],
            childColumns = ["memberId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("memberId"), Index("subGroupId")]
)
data class SubGroupMember(
    val subGroupId: Long,
    val memberId: Long
)

enum class ScopeType { CLASS, SUBGROUP }

enum class ActivityType { ATTENDANCE, PAYMENT, SPLIT, CHECKLIST }

enum class MemberStatus { PENDING, DONE, EXCUSED }

@Entity(
    tableName = "activities",
    indices = [Index("createdAt"), Index("archived"), Index("subGroupId")]
)
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val scopeType: ScopeType,
    val subGroupId: Long? = null,
    val type: ActivityType,
    val title: String,
    val note: String? = null,
    val totalAmount: Long? = null,
    val deadline: Long? = null,
    val archived: Boolean = false,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "activity_members",
    primaryKeys = ["activityId", "memberId"],
    foreignKeys = [
        ForeignKey(
            entity = ActivityEntity::class,
            parentColumns = ["id"],
            childColumns = ["activityId"],
            onDelete = ForeignKey.CASCADE
        )
        // Member is not cascaded: historical rows must survive archive-only lifecycle.
    ],
    indices = [Index("memberId"), Index("activityId")]
)
data class ActivityMember(
    val activityId: Long,
    val memberId: Long,
    val status: MemberStatus,
    val amountDue: Long? = null,
    val amountPaid: Long? = null,
    val note: String? = null,
    val included: Boolean = true,
    val weight: Int = 1,
    val attachmentPath: String? = null,
    val attachmentMime: String? = null,
    val updatedAt: Long
)

enum class LedgerType { INCOME, EXPENSE }

@Entity(
    tableName = "ledger_entries",
    indices = [Index("createdAt"), Index("relatedActivityId")]
)
data class LedgerEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: LedgerType,
    val amountFen: Long,
    val title: String,
    val note: String? = null,
    val relatedActivityId: Long? = null,
    val createdAt: Long
)
