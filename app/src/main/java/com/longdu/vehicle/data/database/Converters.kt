package com.longdu.vehicle.data.database

import androidx.room.TypeConverter
import com.longdu.vehicle.data.entity.PartCategory
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.data.entity.ReminderType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Room 类型转换器
 * 将 Kotlin 特殊类型（LocalDate、枚举）与数据库支持的类型（Long、String）互转
 */
class Converters {

    // ===== LocalDate ↔ Long (以 Epoch Day 存储) =====
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun toLocalDate(epochDay: Long?): LocalDate? =
        epochDay?.let { LocalDate.ofEpochDay(it) }

    // ===== RecordType ↔ String =====
    @TypeConverter
    fun fromRecordType(type: RecordType?): String? = type?.name

    @TypeConverter
    fun toRecordType(value: String?): RecordType? =
        value?.let { try { RecordType.valueOf(it) } catch (_: Exception) { null } }

    // ===== PartCategory ↔ String =====
    @TypeConverter
    fun fromPartCategory(cat: PartCategory?): String? = cat?.name

    @TypeConverter
    fun toPartCategory(value: String?): PartCategory? =
        value?.let { try { PartCategory.valueOf(it) } catch (_: Exception) { null } }

    // ===== ReminderType ↔ String =====
    @TypeConverter
    fun fromReminderType(type: ReminderType?): String? = type?.name

    @TypeConverter
    fun toReminderType(value: String?): ReminderType? =
        value?.let { try { ReminderType.valueOf(it) } catch (_: Exception) { null } }
}
