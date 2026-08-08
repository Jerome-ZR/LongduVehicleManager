package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 车辆列表 ViewModel
 * 管理首页车辆列表的加载、搜索、添加、删除
 */
class VehicleListViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repo = VehicleRepository(
        db.vehicleDao(), db.maintenanceRecordDao()
    )

    /** 车辆列表（StateFlow 供 UI 订阅） */
    private val _vehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val vehicles: StateFlow<List<Vehicle>> = _vehicles.asStateFlow()

    /** 加载状态 */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /** 错误消息（SnackBar 显示） */
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    init {
        loadVehicles()
    }

    /** 加载所有车辆 */
    private fun loadVehicles() {
        viewModelScope.launch {
            repo.getAllVehicles().collect { _vehicles.value = it }
        }
    }

    /** 搜索车辆 */
    fun search(query: String) {
        viewModelScope.launch {
            if (query.isBlank()) loadVehicles()
            else repo.searchVehicles(query).collect { _vehicles.value = it }
        }
    }

    /** 添加车辆 */
    fun addVehicle(
        plateNumber: String, brand: String, model: String, year: Int,
        vinCode: String, mileage: Double, purchaseDate: LocalDate?,
        maintainIntervalKm: Double, maintainRule: String,
        bodyNumber: Int?, color: String, ownerName: String, remark: String
    ) {
        viewModelScope.launch {
            try {
                val vehicle = Vehicle(
                    plateNumber = plateNumber, brand = brand, model = model,
                    year = year, vinCode = vinCode, currentMileage = mileage,
                    purchaseDate = purchaseDate,
                    nextMaintainMileage = mileage + maintainIntervalKm,
                    nextMaintainDate = purchaseDate?.plusYears(1),
                    maintainIntervalKm = maintainIntervalKm,
                    maintainRule = maintainRule,
                    bodyNumber = bodyNumber, color = color,
                    ownerName = ownerName, remark = remark
                )
                repo.insertVehicle(vehicle)
            } catch (e: Exception) {
                _errorMsg.value = "添加失败：${e.message}"
            }
        }
    }

    /** 更新车辆 */
    fun updateVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try { repo.updateVehicle(vehicle) }
            catch (e: Exception) { _errorMsg.value = "更新失败：${e.message}" }
        }
    }

    /** 删除车辆 */
    fun deleteVehicle(vehicle: Vehicle) {
        viewModelScope.launch {
            try { repo.deleteVehicle(vehicle) }
            catch (e: Exception) { _errorMsg.value = "删除失败：${e.message}" }
        }
    }

    fun clearError() { _errorMsg.value = null }
}
