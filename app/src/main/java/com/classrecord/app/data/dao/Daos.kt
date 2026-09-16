package com.classrecord.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ClassProfile
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.SubGroup
import com.classrecord.app.data.entity.SubGroupMember
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassProfileDao {
    @Query("SELECT * FROM class_profile WHERE id = 1")
    fun observe(): Flow<ClassProfile?>

    @Query("SELECT * FROM class_profile WHERE id = 1")
    suspend fun get(): ClassProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(profile: ClassProfile)

    @Query("DELETE FROM class_profile")
    suspend fun deleteAll()
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY archived ASC, name COLLATE LOCALIZED ASC")
    fun observeAll(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE archived = 0 ORDER BY name COLLATE LOCALIZED ASC")
    fun observeActive(): Flow<List<Member>>

    @Query("SELECT * FROM members WHERE archived = 0 ORDER BY name COLLATE LOCALIZED ASC")
    suspend fun getActive(): List<Member>

    @Query("SELECT * FROM members ORDER BY name COLLATE LOCALIZED ASC")
    suspend fun getAll(): List<Member>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreserveId(member: Member)

    @Query("DELETE FROM members")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM members WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM members WHERE id = :id")
    suspend fun getById(id: Long): Member?

    @Insert
    suspend fun insert(member: Member): Long

    @Insert
    suspend fun insertAll(members: List<Member>): List<Long>

    @Update
    suspend fun update(member: Member)
}

@Dao
interface SubGroupDao {
    @Query("SELECT * FROM subgroups ORDER BY archived ASC, createdAt DESC")
    fun observeAll(): Flow<List<SubGroup>>

    @Query("SELECT * FROM subgroups WHERE archived = 0 ORDER BY name COLLATE LOCALIZED ASC")
    fun observeActive(): Flow<List<SubGroup>>

    @Query("SELECT * FROM subgroups WHERE id = :id")
    suspend fun getById(id: Long): SubGroup?

    @Query("SELECT COUNT(*) FROM subgroups WHERE archived = 0")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT * FROM subgroups ORDER BY createdAt DESC")
    suspend fun getAll(): List<SubGroup>

    @Insert
    suspend fun insert(group: SubGroup): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreserveId(group: SubGroup)

    @Update
    suspend fun update(group: SubGroup)

    @Query("DELETE FROM subgroups")
    suspend fun deleteAll()
}

@Dao
interface SubGroupMemberDao {
    @Query("SELECT memberId FROM subgroup_members WHERE subGroupId = :groupId")
    suspend fun getMemberIds(groupId: Long): List<Long>

    @Query("SELECT memberId FROM subgroup_members WHERE subGroupId = :groupId")
    fun observeMemberIds(groupId: Long): Flow<List<Long>>

    @Query("SELECT * FROM subgroup_members")
    fun observeAll(): Flow<List<SubGroupMember>>

    @Query("SELECT * FROM subgroup_members")
    suspend fun getAll(): List<SubGroupMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<SubGroupMember>)

    @Query("DELETE FROM subgroup_members WHERE subGroupId = :groupId")
    suspend fun deleteForGroup(groupId: Long)

    @Query("DELETE FROM subgroup_members")
    suspend fun deleteAll()
}

@Dao
interface ActivityDao {
    @Query("SELECT * FROM activities WHERE archived = 0 ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities ORDER BY createdAt DESC")
    fun observeAllIncludingArchived(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE id = :id")
    fun observeById(id: Long): Flow<ActivityEntity?>

    @Query("SELECT * FROM activities WHERE id = :id")
    suspend fun getById(id: Long): ActivityEntity?

    @Query("SELECT * FROM activities ORDER BY createdAt DESC")
    suspend fun getAll(): List<ActivityEntity>

    @Insert
    suspend fun insert(activity: ActivityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreserveId(activity: ActivityEntity)

    @Update
    suspend fun update(activity: ActivityEntity)

    @Query("DELETE FROM activities")
    suspend fun deleteAll()
}

@Dao
interface ActivityMemberDao {
    @Query("SELECT * FROM activity_members WHERE activityId = :activityId")
    fun observeForActivity(activityId: Long): Flow<List<ActivityMember>>

    @Query("SELECT * FROM activity_members WHERE activityId = :activityId")
    suspend fun getForActivity(activityId: Long): List<ActivityMember>

    @Query("SELECT * FROM activity_members")
    fun observeAll(): Flow<List<ActivityMember>>

    @Query("SELECT * FROM activity_members")
    suspend fun getAll(): List<ActivityMember>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<ActivityMember>)

    @Update
    suspend fun update(row: ActivityMember)

    @Query("DELETE FROM activity_members")
    suspend fun deleteAll()
}

@Dao
interface LedgerDao {
    @Query("SELECT * FROM ledger_entries ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<LedgerEntry>>

    @Query("SELECT * FROM ledger_entries ORDER BY createdAt DESC")
    suspend fun getAll(): List<LedgerEntry>

    @Query("SELECT COUNT(*) FROM ledger_entries WHERE relatedActivityId = :activityId AND type = 'INCOME'")
    fun observeIncomeCount(activityId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM ledger_entries WHERE relatedActivityId = :activityId AND type = 'INCOME'")
    suspend fun incomeCount(activityId: Long): Int

    @Insert
    suspend fun insert(entry: LedgerEntry): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreserveId(entry: LedgerEntry)

    @Query("DELETE FROM ledger_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM ledger_entries")
    suspend fun deleteAll()
}
