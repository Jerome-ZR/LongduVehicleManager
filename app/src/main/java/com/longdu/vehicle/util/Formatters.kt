package com.longdu.vehicle.util

import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * 工具类 — 日期/里程/金额格式化
 * 所有方法均为纯函数，不依赖外部状态
 */
object Formatters {

    /** 日期格式：yyyy-MM-dd */
    val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.CHINA)

    /** 金额格式：保留两位小数 */
    val moneyFormat: DecimalFormat = DecimalFormat("#,##0.00")

    /** 里程格式：保留两位小数，加"km"后缀 */
    val mileageFormat: DecimalFormat = DecimalFormat("#,##0.00")

    /** 格式化 LocalDate 为显示用字符串 */
    fun formatDate(date: LocalDate?): String = date?.format(dateFormat) ?: "未设置"

    /** 格式化金额 */
    fun formatMoney(value: Double): String = "¥${moneyFormat.format(value)}"

    /** 格式化里程 */
    fun formatMileage(value: Double): String = "${mileageFormat.format(value)} km"

    /**
     * 计算距下次保养的剩余里程
     * 返回 null 表示未设置下次保养里程
     */
    fun getRemainingMileage(current: Double, nextMaintain: Double?): Double? {
        return nextMaintain?.let { it - current }
    }

    /**
     * 计算距下次保养的剩余天数
     * 返回 null 表示未设置下次保养日期
     */
    fun getRemainingDays(nextMaintainDate: LocalDate?): Long? {
        return nextMaintainDate?.let {
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), it)
        }
    }
}
