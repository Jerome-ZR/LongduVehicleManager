package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * 维修保养记录 ViewModel
 * 管理新增/编辑记录的表单状态，以及当前选中车辆的信息更新
 */
class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )

    /** 所有车辆（供下拉选择） */
    private val _allVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val allVehicles: StateFlow<List<Vehicle>> = _allVehicles.asStateFlow()

    /** 表单状态 */
    private val _selectedPlate = MutableStateFlow("")
    val selectedPlate: StateFlow<String> = _selectedPlate.asStateFlow()

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _selectedType = MutableStateFlow(RecordType.MAINTENANCE)
    val selectedType: StateFlow<RecordType> = _selectedType.asStateFlow()

    private val _mileage = MutableStateFlow("")
    val mileage: StateFlow<String> = _mileage.asStateFlow()

    private val _cost = MutableStateFlow("")
    val cost: StateFlow<String> = _cost.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _shopName = MutableStateFlow("")
    val shopName: StateFlow<String> = _shopName.asStateFlow()

    private val _location = MutableStateFlow("")
    val location: StateFlow<String> = _location.asStateFlow()

    /** 保存成功信号 */
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()

    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    init {
        viewModelScope.launch {
            repo.getAllVehicles().collect { _allVehicles.value = it }
        }
    }

    /** 更新表单字段 */
    fun setPlate(plate: String) { _selectedPlate.value = plate }
    fun setDate(date: LocalDate) { _selectedDate.value = date }
    fun setType(type: RecordType) { _selectedType.value = type }
    fun setMileage(m: String) { _mileage.value = m }
    fun setCost(c: String) { _cost.value = c }
    fun setDescription(d: String) { _description.value = d }
    fun setShopName(s: String) { _shopName.value = s }
    fun setLocation(l: String) { _location.value = l }

    /** 保存记录 */
    fun saveRecord() {
        val plate = _selectedPlate.value
        if (plate.isEmpty()) { _errorMsg.value = "请选择车辆"; return }
        viewModelScope.launch {
            try {
                val record = MaintenanceRecord(
                    plateNumber = plate,
                    date = _selectedDate.value,
                    mileage = _mileage.value.toDoubleOrNull() ?: 0.0,
                    type = _selectedType.value,
                    cost = _cost.value.toDoubleOrNull() ?: 0.0,
                    description = _description.value,
                    shopName = _shopName.value,
                    location = _location.value
                )
                repo.insertRecord(record)

                // 如果是保养类型，更新车辆的当前里程和下次保养数据
                if (_selectedType.value == RecordType.MAINTENANCE) {
                    val vehicle = repo.getVehicleByPlate(plate)
                    vehicle?.let { v ->
                        val newMileage = _mileage.value.toDoubleOrNull() ?: v.currentMileage
                        val updated = v.copy(
                            currentMileage = newMileage,
                            nextMaintainMileage = newMileage + v.maintainIntervalKm,
                            nextMaintainDate = _selectedDate.value.plusYears(1)
                        )
                        repo.updateVehicle(updated)
                    }
                }

                _saved.value = true
            } catch (e: Exception) {
                _errorMsg.value = "保存失败：${e.message}"
            }
        }
    }

    fun resetSaveFlag() { _saved.value = false }
    fun clearError() { _errorMsg.value = null }
}
