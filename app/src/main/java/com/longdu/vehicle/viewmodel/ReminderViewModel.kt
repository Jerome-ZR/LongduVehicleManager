package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import com.longdu.vehicle.service.ReminderWorker
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.util.SettingsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * 提醒 ViewModel — 基于车辆保养数据实时计算提醒状态
 * 不再使用独立的 ReminderRule 实体
 */
class ReminderViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )
    private val settings = SettingsManager(application)

    /** 逾期保养车辆（剩余里程 < 0） */
    private val _overdueVehicles = MutableStateFlow<List<ReminderVehicle>>(emptyList())
    val overdueVehicles: StateFlow<List<ReminderVehicle>> = _overdueVehicles.asStateFlow()

    /** 即将保养车辆（0 < 剩余 ≤ 阈值） */
    private val _upcomingVehicles = MutableStateFlow<List<ReminderVehicle>>(emptyList())
    val upcomingVehicles: StateFlow<List<ReminderVehicle>> = _upcomingVehicles.asStateFlow()

    /** 保养正常车辆 */
    private val _normalVehicles = MutableStateFlow<List<ReminderVehicle>>(emptyList())
    val normalVehicles: StateFlow<List<ReminderVehicle>> = _normalVehicles.asStateFlow()

    /** 逾期数量 */
    private val _overdueCount = MutableStateFlow(0)
    val overdueCount: StateFlow<Int> = _overdueCount.asStateFlow()

    /** 即将保养数量 */
    private val _upcomingCount = MutableStateFlow(0)
    val upcomingCount: StateFlow<Int> = _upcomingCount.asStateFlow()

    init { scheduleReminderWorker() }

    /** 加载并计算所有车辆的提醒状态 */
    fun loadAll() {
        viewModelScope.launch {
            val vehicles = repo.getAllVehicles().first()
            val mileageThreshold = settings.maintainMileageThreshold
            val daysThreshold = settings.maintainDaysThreshold
            val today = LocalDate.now()

            val result = vehicles.map { v -> computeReminder(v, mileageThreshold, daysThreshold, today) }
                .sortedBy { it.remainingKm ?: Double.MAX_VALUE }

            _overdueVehicles.value = result.filter { it.status == ReminderStatus.OVERDUE }
            _upcomingVehicles.value = result.filter { it.status == ReminderStatus.UPCOMING }
            _normalVehicles.value = result.filter { it.status == ReminderStatus.NORMAL }
            _overdueCount.value = _overdueVehicles.value.size
            _upcomingCount.value = _upcomingVehicles.value.size
        }
    }

    /** 计算单辆车的提醒状态 */
    private fun computeReminder(v: Vehicle, kmThreshold: Double, daysThreshold: Int, today: LocalDate): ReminderVehicle {
        val remainingKm = v.nextMaintainMileage?.let { it - v.currentMileage }
        val remainingDays = v.nextMaintainDate?.let { ChronoUnit.DAYS.between(today, it) }

        val status = when {
            remainingKm != null && remainingKm < 0 -> ReminderStatus.OVERDUE
            remainingDays != null && remainingDays < 0 -> ReminderStatus.OVERDUE
            remainingKm != null && remainingKm > 0 && remainingKm <= kmThreshold -> ReminderStatus.UPCOMING
            remainingDays != null && remainingDays in 1..daysThreshold.toLong() -> ReminderStatus.UPCOMING
            else -> ReminderStatus.NORMAL
        }

        return ReminderVehicle(
            plateNumber = v.plateNumber,
            bodyNumber = v.bodyNumber,
            ownerName = v.ownerName,
            brand = v.brand, model = v.model,
            currentMileage = v.currentMileage,
            nextMaintainMileage = v.nextMaintainMileage,
            nextMaintainDate = v.nextMaintainDate,
            remainingKm = remainingKm,
            remainingDays = remainingDays,
            status = status
        )
    }

    private fun scheduleReminderWorker() {
        val hours = settings.reminderPeriodHours.toLong().takeIf { it > 0 } ?: 12L
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(hours, TimeUnit.HOURS).build()
        WorkManager.getInstance(app).enqueueUniquePeriodicWork(
            "reminder_check", ExistingPeriodicWorkPolicy.KEEP, request
        )
    }
}

/** 提醒状态 */
enum class ReminderStatus { OVERDUE, UPCOMING, NORMAL }

/** 带提醒信息的车辆数据 */
data class ReminderVehicle(
    val plateNumber: String,
    val bodyNumber: Int?,
    val ownerName: String,
    val brand: String, val model: String,
    val currentMileage: Double,
    val nextMaintainMileage: Double?,
    val nextMaintainDate: LocalDate?,
    val remainingKm: Double?,     // 剩余公里数（负值=已超）
    val remainingDays: Long?,     // 剩余天数（负值=已超）
    val status: ReminderStatus
)
