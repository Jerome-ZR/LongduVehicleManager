package com.longdu.vehicle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VehicleDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = VehicleRepository(
        AppDatabase.getInstance(application).vehicleDao(),
        AppDatabase.getInstance(application).maintenanceRecordDao()
    )

    private val _vehicle = MutableStateFlow<Vehicle?>(null)
    val vehicle: StateFlow<Vehicle?> = _vehicle.asStateFlow()

    private val _records = MutableStateFlow<List<MaintenanceRecord>>(emptyList())
    val records: StateFlow<List<MaintenanceRecord>> = _records.asStateFlow()

    // TODO: 配件列表（后续重做）

    fun loadVehicle(plate: String) {
        viewModelScope.launch {
            _vehicle.value = repo.getVehicleByPlate(plate)
            repo.getRecordsByPlate(plate).collect { _records.value = it }
        }
    }

    // TODO: loadAllParts() 后续重做

    fun deleteRecord(record: MaintenanceRecord) {
        viewModelScope.launch { repo.deleteRecord(record) }
    }

    // TODO: deletePart() / addPart() 后续重做
}
