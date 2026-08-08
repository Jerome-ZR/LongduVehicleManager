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

class MaintenanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao(),
        AppDatabase.getInstance(application).partDao(),
        AppDatabase.getInstance(application).reminderRuleDao()
    )

    private val _allVehicles = MutableStateFlow<List<Vehicle>>(emptyList())
    val allVehicles: StateFlow<List<Vehicle>> = _allVehicles.asStateFlow()

    private val _allRecords = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val allRecords: StateFlow<List<MaintenanceRecord>> = _allRecords.asStateFlow()

    private val _vehiclePlateMap = MutableStateFlow<Map<String, String>>(emptyMap())
    val vehiclePlateMap: StateFlow<Map<String, String>> = _vehiclePlateMap.asStateFlow()

    // 表单状态
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
    private val _saved = MutableStateFlow(false)
    val saved: StateFlow<Boolean> = _saved.asStateFlow()
    private val _errorMsg = MutableStateFlow<String?>(null)
    val errorMsg: StateFlow<String?> = _errorMsg.asStateFlow()

    /** 正在编辑的记录 ID（null = 新增模式） */
    private var editingId: Long? = null

    init {
        viewModelScope.launch { repo.getAllVehicles().collect { _allVehicles.value = it } }
        viewModelScope.launch { repo.getAllRecords().collect { _allRecords.value = it } }
        viewModelScope.launch {
            repo.getAllVehicles().collect { _vehiclePlateMap.value = it.associate { v -> v.plateNumber to v.plateNumber } }
        }
    }

    /** 加载已有记录进行编辑 */
    fun loadForEdit(record: MaintenanceRecord) {
        editingId = record.id
        _selectedPlate.value = record.plateNumber
        _selectedDate.value = record.date
        _selectedType.value = record.type
        _mileage.value = record.mileage.toString()
        _cost.value = record.cost.toString()
        _description.value = record.description
        _shopName.value = record.shopName
        _location.value = record.location
    }

    fun setPlate(p: String) { _selectedPlate.value = p }
    fun setDate(d: LocalDate) { _selectedDate.value = d }
    fun setType(t: RecordType) { _selectedType.value = t }
    fun setMileage(m: String) { _mileage.value = m }
    fun setCost(c: String) { _cost.value = c }
    fun setDescription(d: String) { _description.value = d }
    fun setShopName(s: String) { _shopName.value = s }
    fun setLocation(l: String) { _location.value = l }

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repo.deleteRecord(record) }
    }

    /** 保存（新增/编辑通用） */
    fun saveRecord() {
        val plate = _selectedPlate.value
        if (plate.isEmpty()) { _errorMsg.value = "请选择车辆"; return }
        viewModelScope.launch {
            try {
                val record = MaintenanceRecord(
                    id = editingId ?: 0,
                    plateNumber = plate, date = _selectedDate.value,
                    mileage = _mileage.value.toDoubleOrNull() ?: 0.0,
                    type = _selectedType.value, cost = _cost.value.toDoubleOrNull() ?: 0.0,
                    description = _description.value, shopName = _shopName.value, location = _location.value
                )
                if (editingId != null) repo.updateRecord(record)
                else repo.insertRecord(record)

                if (_selectedType.value == RecordType.MAINTENANCE) {
                    val vehicle = repo.getVehicleByPlate(plate)
                    vehicle?.let { v ->
                        val newKm = _mileage.value.toDoubleOrNull() ?: v.currentMileage
                        repo.updateVehicle(v.copy(currentMileage = newKm, nextMaintainMileage = newKm + v.maintainIntervalKm, nextMaintainDate = _selectedDate.value.plusYears(1)))
                    }
                }
                _saved.value = true
            } catch (e: Exception) { _errorMsg.value = "保存失败：${e.message}" }
        }
    }

    fun resetSaveFlag() { _saved.value = false; editingId = null }
    fun clearError() { _errorMsg.value = null }
}
