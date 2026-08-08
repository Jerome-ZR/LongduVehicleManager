package com.longdu.vehicle.util

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 设置管理器 — 持久化提醒阈值等配置项
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    // 保养提醒阈值（公里数）
    var maintainMileageThreshold: Double
        get() = prefs.getFloat("maintain_mileage_threshold", 500f).toDouble()
        set(value) = prefs.edit().putFloat("maintain_mileage_threshold", value.toFloat()).apply()

    // 保养提醒阈值（天数）
    var maintainDaysThreshold: Int
        get() = prefs.getInt("maintain_days_threshold", 30)
        set(value) = prefs.edit().putInt("maintain_days_threshold", value).apply()

    // 年审提醒阈值（天数）
    var inspectionDaysThreshold: Int
        get() = prefs.getInt("inspection_days_threshold", 30)
        set(value) = prefs.edit().putInt("inspection_days_threshold", value).apply()

    // WorkManager 提醒周期（小时）
    var reminderPeriodHours: Int
        get() = prefs.getInt("reminder_period_hours", 12)
        set(value) = prefs.edit().putInt("reminder_period_hours", value).apply()

    /** 导出所有设置为 JSON */
    fun exportSettings(): String {
        val map = prefs.all.entries.joinToString(",", "{", "}") { "\"${it.key}\": \"${it.value}\"" }
        return """{"settings": $map}"""
    }

    /** 从 JSON 导入设置 */
    fun importSettings(json: String) {
        try {
            val cleaned = json.trim().removePrefix("{").removeSuffix("}")
            cleaned.split(",").forEach { pair ->
                val (key, value) = pair.split(":").map { it.trim().removeSurrounding("\"") }
                prefs.edit().putString(key, value).apply()
            }
        } catch (_: Exception) {}
    }
}
