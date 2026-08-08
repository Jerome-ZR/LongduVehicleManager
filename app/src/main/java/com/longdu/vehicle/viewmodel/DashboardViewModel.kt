package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 主页仪表盘 ViewModel
 * 统计卡片数据 + 待办提醒 + 最近记录
 */
class DashboardViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )

    /** 逾期保养数 */ private val _overdue = MutableStateFlow(0)
    val overdue: StateFlow<Int> = _overdue.asStateFlow()
    /** 即将保养数 */ private val _upcoming = MutableStateFlow(0)
    val upcoming: StateFlow<Int> = _upcoming.asStateFlow()
    /** 车辆总数 */ private val _vehicleCount = MutableStateFlow(0)
    val vehicleCount: StateFlow<Int> = _vehicleCount.asStateFlow()
    /** 维修记录总数 */ private val _recordCount = MutableStateFlow(0)
    val recordCount: StateFlow<Int> = _recordCount.asStateFlow()
    /** 所有车辆(列表) */ private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()
    /** 待办提醒 */ private val _reminders = MutableStateFlow<List<ReminderRule>>(emptyList())
    val reminders: StateFlow<List<ReminderRule>> = _reminders.asStateFlow()
    /** 最近记录 */ private val _recentRecords = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val recentRecords: StateFlow<List<MaintenanceRecord>> = _recentRecords.asStateFlow()

    init { loadDashboard() }

    fun loadDashboard() {
        viewModelScope.launch {
            _overdue.value = repo.getOverdueMaintainCount()
            _upcoming.value = repo.getUpcomingMaintainCount()
            _vehicleCount.value = repo.getVehicleCount()
            _recordCount.value = repo.getRecordCount()
        }
        viewModelScope.launch {
            repo.getAllVehicles().collect { _vehicles.value = it }
        }
        viewModelScope.launch {
            repo.getAllEnabledRules().collect { _reminders.value = it.take(5) }
        }
        viewModelScope.launch {
            repo.getRecentRecords(5).collect { _recentRecords.value = it }
        }
    }
}
