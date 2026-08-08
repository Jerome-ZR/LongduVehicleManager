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

/** 车辆数据仓库 — 封装所有数据库操作 */
class VehicleRepository(
    private val vehicleDao: VehicleDao,
    private val recordDao: MaintenanceRecordDao,
    private val partDao: PartDao,
    private val reminderDao: ReminderRuleDao
) {
    // ===== 车辆 =====
    fun getAllVehicles(): Flow<List<Vehicle>> = vehicleDao.getAllVehicles().flowOn(Dispatchers.IO)
    fun searchVehicles(q: String): Flow<List<Vehicle>> = vehicleDao.searchVehicles(q).flowOn(Dispatchers.IO)
    fun getOverdueMaintainVehicles(): Flow<List<Vehicle>> = vehicleDao.getOverdueMaintainVehicles().flowOn(Dispatchers.IO)
    suspend fun getVehicleByPlate(plate: String): Vehicle? = vehicleDao.getVehicleByPlate(plate)
    suspend fun getVehicleCount(): Int = withContext(Dispatchers.IO) { vehicleDao.getCount() }
    suspend fun getOverdueMaintainCount(): Int = withContext(Dispatchers.IO) {
        vehicleDao.getOverdueCount()
    }
    suspend fun getUpcomingMaintainCount(): Int = withContext(Dispatchers.IO) {
        vehicleDao.getUpcomingCount()
    }
    suspend fun insertVehicle(v: Vehicle) = withContext(Dispatchers.IO) { vehicleDao.insert(v) }
    suspend fun updateVehicle(v: Vehicle) = withContext(Dispatchers.IO) { vehicleDao.update(v) }
    suspend fun deleteVehicle(v: Vehicle) = withContext(Dispatchers.IO) { vehicleDao.delete(v) }

    // ===== 保养记录 =====
    fun getRecordsByPlate(plate: String): Flow<List<MaintenanceRecord>> = recordDao.getRecordsByPlate(plate).flowOn(Dispatchers.IO)
    fun getRecentRecords(limit: Int = 5): Flow<List<MaintenanceRecord>> = recordDao.getRecentRecords(limit).flowOn(Dispatchers.IO)
    fun getAllRecords(): Flow<List<MaintenanceRecord>> = recordDao.getAllRecords().flowOn(Dispatchers.IO)
    suspend fun getRecordCount(): Int = withContext(Dispatchers.IO) { recordDao.getCount() }
    suspend fun insertRecord(r: MaintenanceRecord) = withContext(Dispatchers.IO) { recordDao.insert(r) }
    suspend fun deleteRecord(r: MaintenanceRecord) = withContext(Dispatchers.IO) { recordDao.delete(r) }
    suspend fun getYearlyCost(yStart: Long, yEnd: Long): Double? = recordDao.getYearlyCost(yStart, yEnd)

    // ===== 配件 =====
    fun getPartsByPlate(plate: String): Flow<List<Part>> = partDao.getPartsByPlate(plate).flowOn(Dispatchers.IO)
    suspend fun insertPart(p: Part) = withContext(Dispatchers.IO) { partDao.insert(p) }
    suspend fun deletePart(p: Part) = withContext(Dispatchers.IO) { partDao.delete(p) }
    suspend fun getPartCount(): Int = withContext(Dispatchers.IO) { partDao.getCount() }

    // ===== 提醒规则 =====
    fun getRulesByPlate(plate: String): Flow<List<ReminderRule>> = reminderDao.getRulesByPlate(plate).flowOn(Dispatchers.IO)
    fun getAllEnabledRules(): Flow<List<ReminderRule>> = reminderDao.getEnabledRules().flowOn(Dispatchers.IO)
    fun getAllReminderRules(): Flow<List<ReminderRule>> = reminderDao.getAllRules().flowOn(Dispatchers.IO)
    suspend fun insertRule(r: ReminderRule) = withContext(Dispatchers.IO) { reminderDao.insert(r) }
    suspend fun toggleRule(id: Long, enabled: Boolean) = withContext(Dispatchers.IO) { reminderDao.toggleEnabled(id, enabled) }
    suspend fun deleteRule(r: ReminderRule) = withContext(Dispatchers.IO) { reminderDao.delete(r) }

    // ===== 清空数据库 =====
    suspend fun clearAllData() = withContext(Dispatchers.IO) {
        reminderDao.deleteAll()
        recordDao.deleteAll()
        partDao.deleteAll()
        vehicleDao.deleteAll()
    }
}
