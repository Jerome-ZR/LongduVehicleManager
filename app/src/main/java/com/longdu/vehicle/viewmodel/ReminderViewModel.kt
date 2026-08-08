package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import com.longdu.vehicle.service.ReminderWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 提醒规则 ViewModel
 * 管理提醒规则列表，触发 WorkManager 定时任务
 */
class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )

    /** 当前选中车辆的提醒规则列表 */
    private val _rules = MutableStateFlow<List<ReminderRule>>(emptyList())
    val rules: StateFlow<List<ReminderRule>> = _rules.asStateFlow()

    /** 所有车辆列表（供规则关联选择） */
    private val _allVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val allVehicles: StateFlow<List<Vehicle>> = _allVehicles.asStateFlow()

    /** 当前查看的车牌号 */
    private val _currentPlate = MutableStateFlow("")
    val currentPlate: StateFlow<String> = _currentPlate.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllVehicles().collect { _allVehicles.value = it }
        }
        // 首次启动时调度 WorkManager 定时任务
        scheduleReminderWorker()
    }

    /** 加载某车辆的提醒规则 */
    fun loadRules(plate: String) {
        _currentPlate.value = plate
        viewModelScope.launch {
            repo.getRulesByPlate(plate).collect { _rules.value = it }
        }
    }

    /** 添加新规则 */
    fun addRule(plate: String, type: ReminderType, threshold: Double, content: String) {
        viewModelScope.launch {
            try {
                val rule = ReminderRule(
                    plateNumber = plate, type = type,
                    threshold = threshold, content = content, isEnabled = true
                )
                repo.insertRule(rule)
            } catch (e: Exception) { /* ignored */ }
        }
    }

    /** 切换规则启用/禁用 */
    fun toggleRule(rule: ReminderRule) {
        viewModelScope.launch {
            repo.toggleRule(rule.id, !rule.isEnabled)
        }
    }

    /** 删除规则 */
    fun deleteRule(rule: ReminderRule) {
        viewModelScope.launch {
            repo.deleteRule(rule)
        }
    }

    /**
     * 调度 WorkManager 定时提醒任务
     * 每 12 小时检查一次所有启用的提醒规则
     */
    private fun scheduleReminderWorker() {
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(
            12, TimeUnit.HOURS
        ).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            "reminder_check",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /** 加载所有提醒规则（提醒页面使用） */
    fun loadAllRules() {
        viewModelScope.launch {
            repo.getAllReminderRules().collect { _rules.value = it }
        }
    }

}
