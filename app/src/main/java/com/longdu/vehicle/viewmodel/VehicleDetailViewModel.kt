package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 车辆详情 ViewModel
 * 管理单辆车的详情信息、维修记录列表、配件列表
 */
class VehicleDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )

    /** 当前车辆信息 */
    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    /** 维修保养记录列表 */
    private val _records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val records: StateFlow<List<MaintenanceRecord>> = _records.asStateFlow()

    /** 配件列表 */
    private val _parts = MutableStateFlow<List<Part>>(emptyList())
    val parts: StateFlow<List<Part>> = _parts.asStateFlow()

    /** 加载车辆详情 */
    fun loadVehicle(plate: String) {
        viewModelScope.launch {
            _vehicle.value = repo.getVehicleByPlate(plate)
            // 同时加载记录和配件
            repo.getRecordsByPlate(plate).collect { _records.value = it }
        }
        viewModelScope.launch {
            repo.getPartsByPlate(plate).collect { _parts.value = it }
        }
    }

    /** 删除保养记录 */
    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repo.deleteRecord(record) }
    }

    /** 删除配件 */
    fun deletePart(part: Part) {
        viewModelScope.launch { repo.deletePart(part) }
    }

    /** 添加配件 */
    fun addPart(part: Part) {
        viewModelScope.launch { repo.insertPart(part) }
    }
}
