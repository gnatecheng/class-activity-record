package com.classrecord.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.classrecord.app.data.dao.ActivityDao
import com.classrecord.app.data.dao.ActivityMemberDao
import com.classrecord.app.data.dao.ClassProfileDao
import com.classrecord.app.data.dao.LedgerDao
import com.classrecord.app.data.dao.MemberDao
import com.classrecord.app.data.dao.SubGroupDao
import com.classrecord.app.data.dao.SubGroupMemberDao
import com.classrecord.app.data.entity.ActivityEntity
import com.classrecord.app.data.entity.ActivityMember
import com.classrecord.app.data.entity.ClassProfile
import com.classrecord.app.data.entity.LedgerEntry
import com.classrecord.app.data.entity.Member
import com.classrecord.app.data.entity.SubGroup
import com.classrecord.app.data.entity.SubGroupMember

@Database(
    entities = [
        ClassProfile::class,
        Member::class,
        SubGroup::class,
        SubGroupMember::class,
        ActivityEntity::class,
        ActivityMember::class,
        LedgerEntry::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun classProfileDao(): ClassProfileDao
    abstract fun memberDao(): MemberDao
    abstract fun subGroupDao(): SubGroupDao
    abstract fun subGroupMemberDao(): SubGroupMemberDao
    abstract fun activityDao(): ActivityDao
    abstract fun activityMemberDao(): ActivityMemberDao
    abstract fun ledgerDao(): LedgerDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ledger_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `amountFen` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `note` TEXT,
                        `relatedActivityId` INTEGER,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ledger_entries_createdAt` ON `ledger_entries` (`createdAt`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_ledger_entries_relatedActivityId` ON `ledger_entries` (`relatedActivityId`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `activity_members` ADD COLUMN `included` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE `activity_members` ADD COLUMN `weight` INTEGER NOT NULL DEFAULT 1"
                )
                db.execSQL(
                    "ALTER TABLE `activity_members` ADD COLUMN `attachmentPath` TEXT"
                )
                db.execSQL(
                    "ALTER TABLE `activity_members` ADD COLUMN `attachmentMime` TEXT"
                )
            }
        }

        fun create(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "class_record.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
        }
    }
}
