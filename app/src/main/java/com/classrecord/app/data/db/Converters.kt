package com.classrecord.app.data.db

import androidx.room.TypeConverter
import com.classrecord.app.data.entity.ActivityType
import com.classrecord.app.data.entity.LedgerType
import com.classrecord.app.data.entity.MemberStatus
import com.classrecord.app.data.entity.ScopeType

class Converters {
    @TypeConverter
    fun fromScopeType(value: ScopeType): String = value.name

    @TypeConverter
    fun toScopeType(value: String): ScopeType = ScopeType.valueOf(value)

    @TypeConverter
    fun fromActivityType(value: ActivityType): String = value.name

    @TypeConverter
    fun toActivityType(value: String): ActivityType = ActivityType.valueOf(value)

    @TypeConverter
    fun fromMemberStatus(value: MemberStatus): String = value.name

    @TypeConverter
    fun toMemberStatus(value: String): MemberStatus = MemberStatus.valueOf(value)

    @TypeConverter
    fun fromLedgerType(value: LedgerType): String = value.name

    @TypeConverter
    fun toLedgerType(value: String): LedgerType = LedgerType.valueOf(value)
}
