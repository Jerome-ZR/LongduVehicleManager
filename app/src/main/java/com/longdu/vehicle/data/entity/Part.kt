package com.longdu.vehicle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 配件价格实体类
 *
 * @param id 自增主键
 * @param plateNumber 适用车牌号（"通用" 表示适配所有车型）
 * @param partName 配件名称
 * @param brand 配件品牌
 * @param category 配件分类（机油/滤芯/轮胎/刹车/灯具/车身/电器/其他）
 * @param price 参考价格（元，Double 双精度）
 * @param supplier 供应商（如"铁马机车生活馆"）
 * @param replaceDate 更换日期
 * @param remark 备注
 */
@Entity(tableName = "parts")
data class Part(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val plateNumber: String = "通用",       // 适用车牌号（"通用"或具体车牌号）
    val partName: String,                   // 配件名称
    val brand: String = "",                 // 配件品牌
    val category: PartCategory = PartCategory.OTHER, // 配件分类

    val price: Double = 0.0,                // 参考价格(元)
    val supplier: String = "",              // 供应商

    val replaceDate: LocalDate? = null,     // 更换日期
    val remark: String = ""                 // 备注
)

/**
 * 配件分类枚举
 */
enum class PartCategory {
    OIL,        // 机油
    FILTER,     // 滤芯（机滤/空滤）
    TIRE,       // 轮胎
    BRAKE,      // 刹车片/刹车盘
    LIGHT,      // 灯具
    BODY,       // 车身件
    ELECTRICAL, // 电器件
    OTHER       // 其他
}
