package com.longdu.vehicle.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * 车辆实体类
 * 以车牌号为主键，存储车辆基本信息和保养相关数据
 *
 * @param plateNumber 车牌号（主键，如"豫J0298警"）
 * @param brand 车辆品牌（如"春风"）
 * @param model 车辆型号（如"650TR-G"）
 * @param year 出厂年份
 * @param vinCode 车辆识别码（VIN）
 * @param currentMileage 当前总里程（公里，Double 双精度）
 * @param purchaseDate 购买日期
 * @param nextMaintainMileage 下次保养里程（当前里程+保养间隔自动计算）
 * @param nextMaintainDate 下次保养日期
 * @param bodyNumber 车身编号（警用车辆编号，可选）
 * @param color 颜色
 * @param ownerName 使用人/责任人
 * @param maintainRule 保养规则描述（如"1年或3000公里"）
 * @param maintainIntervalKm 保养里程间隔（公里）
 * @param remark 备注
 */
@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey
    val plateNumber: String,

    val brand: String = "", val model: String = "", val year: Int = 2020,
    val vinCode: String = "",                   // VIN码
    val currentMileage: Double = 0.0,           // 当前总里程(km)
    val purchaseDate: LocalDate? = null,        // 购买日期
    val nextMaintainMileage: Double? = null,    // 下次保养里程
    val nextMaintainDate: LocalDate? = null,    // 下次保养日期

    val lastMaintainDate: LocalDate? = null,    // 上次保养时间
    val lastMaintainKm: Double? = null,         // 上次保养公里数
    val inspectionDate: LocalDate? = null,      // 审车日期

    val bodyNumber: Int? = null,                // 车身编号
    val color: String = "",                     // 颜色
    val ownerName: String = "",                 // 使用人
    val maintainRule: String = "",              // 保养规则描述
    val maintainIntervalKm: Double = 3000.0,    // 保养里程间隔(km)
    val remark: String = ""                     // 备注
)
