package com.longdu.vehicle.data.database

import androidx.room.TypeConverter
import com.longdu.vehicle.data.entity.RecordType
import java.time.LocalDate

/** Room 类型转换器（PartCategory / ReminderType 已移除） */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? =
        epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun fromRecordType(type: RecordType?): String? = type?.name

    @TypeConverter
    fun toRecordType(value: String?): RecordType? =
        value?.let { try { RecordType.valueOf(it) } catch (_: Exception) { null } }
}
