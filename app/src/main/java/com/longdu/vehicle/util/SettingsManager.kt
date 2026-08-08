package com.longdu.vehicle.util

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray

/**
 * 设置管理器 — 持久化提醒阈值、维修地点等配置项
 */
class SettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    var maintainMileageThreshold: Double
        get() = prefs.getFloat("maintain_mileage_threshold", 500f).toDouble()
        set(value) = prefs.edit().putFloat("maintain_mileage_threshold", value.toFloat()).apply()

    var maintainDaysThreshold: Int
        get() = prefs.getInt("maintain_days_threshold", 30)
        set(value) = prefs.edit().putInt("maintain_days_threshold", value).apply()

    var inspectionDaysThreshold: Int
        get() = prefs.getInt("inspection_days_threshold", 30)
        set(value) = prefs.edit().putInt("inspection_days_threshold", value).apply()

    var reminderPeriodHours: Int
        get() = prefs.getInt("reminder_period_hours", 12)
        set(value) = prefs.edit().putInt("reminder_period_hours", value).apply()

    // ===== 维修地点管理 =====
    fun getLocations(): List<String> {
        val json = prefs.getString("maintenance_locations", null) ?: return DEFAULT_LOCATIONS
        return try { (0 until JSONArray(json).length()).map { JSONArray(json).getString(it) } } catch (_: Exception) { DEFAULT_LOCATIONS }
    }
    fun addLocation(name: String) {
        val list = getLocations().toMutableList()
        if (name.isNotBlank() && name !in list) { list.add(name); saveLocations(list) }
    }
    fun deleteLocation(name: String) { saveLocations(getLocations().filter { it != name }) }
    fun updateLocation(old: String, new: String) {
        val list = getLocations().toMutableList()
        val idx = list.indexOf(old)
        if (idx >= 0 && new.isNotBlank()) { list[idx] = new; saveLocations(list) }
    }
    private fun saveLocations(list: List<String>) {
        prefs.edit().putString("maintenance_locations", JSONArray(list).toString()).apply()
    }

    companion object {
        val DEFAULT_LOCATIONS = listOf("铁马机车生活馆", "洪亮机车", "现奔宝", "其他")
    }
}
