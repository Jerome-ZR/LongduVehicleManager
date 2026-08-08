package com.longdu.vehicle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 维修保养记录实体类
 *
 * @param id 自增主键
 * @param plateNumber 关联车牌号（外键，与 Vehicle.plateNumber 对应）
 * @param date 记录日期
 * @param mileage 当前里程(km)
 * @param type 记录类型：保养 / 维修 / 年检 / 保险
 * @param cost 费用（元，Double 双精度）
 * @param description 描述（维修/保养项目内容）
 * @param shopName 维修厂名称
 * @param location 维修地点
 */
@Entity(tableName = "maintenance_records")
data class MaintenanceRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val plateNumber: String,                // 关联车牌号
    val date: LocalDate,                    // 记录日期
    val mileage: Double = 0.0,              // 当前里程(km)

    val type: RecordType,                   // 记录类型枚举
    val cost: Double = 0.0,                 // 费用(元)
    val description: String = "",           // 描述
    val shopName: String = "",              // 维修厂名称
    val location: String = ""               // 维修地点
)

/**
 * 记录类型枚举
 */
enum class RecordType {
    MAINTENANCE,    // 保养
    REPAIR,         // 维修
    INSPECTION,     // 年检
    INSURANCE       // 保险
}
