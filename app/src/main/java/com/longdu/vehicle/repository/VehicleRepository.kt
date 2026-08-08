package com.longdu.vehicle.repository

import com.longdu.vehicle.data.dao.MaintenanceRecordDao
import com.longdu.vehicle.data.dao.PartDao
import com.longdu.vehicle.data.dao.ReminderRuleDao
import com.longdu.vehicle.data.dao.VehicleDao
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.Vehicle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * 车辆数据仓库 — 封装所有数据库操作
 * 确保所有 Flow 在 IO 线程执行，suspend 函数显式切换到 IO
 */
class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val recordDao: MaintenanceRecordDao,
    private val partDao: PartDao,
    private val reminderDao: ReminderRuleDao
) {
    // ===== 车辆 =====
    fun getAllVehicles(): Flow<List<Vehicle>> =
        vehicleDao.getAllVehicles().flowOn(Dispatchers.IO)

    fun searchVehicles(query: String): Flow<List<Vehicle>> =
        vehicleDao.searchVehicles(query).flowOn(Dispatchers.IO)

    fun getOverdueMaintainVehicles(): Flow<List<Vehicle>> =
        vehicleDao.getOverdueMaintainVehicles().flowOn(Dispatchers.IO)

    suspend fun getVehicleByPlate(plate: String): Vehicle? =
        vehicleDao.getVehicleByPlate(plate)

    suspend fun getVehicleCount(): Int = withContext(Dispatchers.IO) { vehicleDao.getCount() }

    suspend fun insertVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.insert(vehicle)
    }

    suspend fun updateVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.update(vehicle)
    }

    suspend fun deleteVehicle(vehicle: Vehicle) = withContext(Dispatchers.IO) {
        vehicleDao.delete(vehicle)
    }

    // ===== 保养记录 =====
    fun getRecordsByPlate(plate: String): Flow<List<MaintenanceRecord>> =
        recordDao.getRecordsByPlate(plate).flowOn(Dispatchers.IO)

    fun getRecentRecords(limit: Int = 5): Flow<List<MaintenanceRecord>> =
        recordDao.getRecentRecords(limit).flowOn(Dispatchers.IO)

    suspend fun insertRecord(record: MaintenanceRecord) = withContext(Dispatchers.IO) {
        recordDao.insert(record)
    }

    suspend fun deleteRecord(record: MaintenanceRecord) = withContext(Dispatchers.IO) {
        recordDao.delete(record)
    }

    suspend fun getYearlyCost(yearStart: Long, yearEnd: Long): Double? =
        recordDao.getYearlyCost(yearStart, yearEnd)

    // ===== 配件 =====
    fun getPartsByPlate(plate: String): Flow<List<Part>> =
        partDao.getPartsByPlate(plate).flowOn(Dispatchers.IO)

    suspend fun insertPart(part: Part) = withContext(Dispatchers.IO) {
        partDao.insert(part)
    }

    suspend fun deletePart(part: Part) = withContext(Dispatchers.IO) {
        partDao.delete(part)
    }

    // ===== 提醒规则 =====
    fun getRulesByPlate(plate: String): Flow<List<ReminderRule>> =
        reminderDao.getRulesByPlate(plate).flowOn(Dispatchers.IO)

    fun getAllEnabledRules(): Flow<List<ReminderRule>> =
        reminderDao.getEnabledRules().flowOn(Dispatchers.IO)

    suspend fun insertRule(rule: ReminderRule) = withContext(Dispatchers.IO) {
        reminderDao.insert(rule)
    }

    suspend fun toggleRule(ruleId: Long, enabled: Boolean) = withContext(Dispatchers.IO) {
        reminderDao.toggleEnabled(ruleId, enabled)
    }

    suspend fun deleteRule(rule: ReminderRule) = withContext(Dispatchers.IO) {
        reminderDao.delete(rule)
    }
}
