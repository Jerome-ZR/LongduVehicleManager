package com.longdu.vehicle.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.longdu.vehicle.data.entity.MaintenanceRecord
import kotlinx.coroutines.flow.Flow

/**
 * 维修保养记录数据访问对象
 */
@Dao
interface MaintenanceRecordDao {

    /** 查询所有记录，按日期倒序 */
    @Query("SELECT * FROM maintenance_records ORDER BY date DESC")
    fun getAllRecords(): Flow<List<MaintenanceRecord>>

    /** 查询某车辆的全部记录 */
    @Query("SELECT * FROM maintenance_records WHERE plateNumber = :plate ORDER BY date DESC")
    fun getRecordsByPlate(plate: String): Flow<List<MaintenanceRecord>>

    /** 按类型筛选某车辆的记录 */
    @Query("SELECT * FROM maintenance_records WHERE plateNumber = :plate AND type = :type ORDER BY date DESC")
    fun getRecordsByPlateAndType(plate: String, type: String): Flow<List<MaintenanceRecord>>

    /** 查询最近N条记录 */
    @Query("SELECT * FROM maintenance_records ORDER BY date DESC LIMIT :limit")
    fun getRecentRecords(limit: Int = 5): Flow<List<MaintenanceRecord>>

    /** 按年份统计费用 */
    @Query("""
        SELECT SUM(cost) FROM maintenance_records 
        WHERE date >= :yearStart AND date < :yearEnd
    """)
    suspend fun getYearlyCost(yearStart: Long, yearEnd: Long): Double?

    /** 按车辆统计年度费用 */
    @Query("""
        SELECT plateNumber, SUM(cost) as totalCost 
        FROM maintenance_records 
        WHERE date >= :yearStart AND date < :yearEnd
        GROUP BY plateNumber ORDER BY totalCost DESC
    """)
    suspend fun getYearlyCostPerVehicle(yearStart: Long, yearEnd: Long): List<PlateCost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: MaintenanceRecord)

    @Update
    suspend fun update(record: MaintenanceRecord)

    @Delete
    suspend fun delete(record: MaintenanceRecord)
}

/** 车辆费用统计 DTO */
data class PlateCost(
    val plateNumber: String,
    val totalCost: Double
)
