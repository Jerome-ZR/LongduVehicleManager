package com.longdu.vehicle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 提醒规则实体类
 * 每条规则定义一个触发条件：按里程或按日期，达到阈值时触发通知
 *
 * @param id 自增主键
 * @param plateNumber 关联车牌号
 * @param type 提醒类型：按里程(MILEAGE) / 按日期(DATE)
 * @param threshold 触发阈值（里程类：公里数，如 5000；日期类：天数，如 180）
 * @param content 提醒内容（如"距下次保养还剩1000公里"）
 * @param isEnabled 是否启用
 */
@Entity(tableName = "reminder_rules")
data class ReminderRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val plateNumber: String,                // 关联车牌号
    val type: ReminderType,                 // 提醒类型
    val threshold: Double = 0.0,            // 触发阈值
    val content: String = "",               // 提醒内容
    val isEnabled: Boolean = true           // 是否启用
)

/**
 * 提醒类型枚举
 * MILEAGE: 按里程触发（如距下次保养剩余 <= 1000km）
 * DATE: 按日期触发（如距下次保养 <= 30天）
 */
enum class ReminderType {
    MILEAGE,    // 按里程
    DATE        // 按日期
}
