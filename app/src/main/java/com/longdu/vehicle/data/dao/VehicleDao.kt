package com.longdu.vehicle.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.longdu.vehicle.data.entity.Vehicle
import kotlinx.coroutines.flow.Flow

/**
 * 车辆数据访问对象
 */
@Dao
interface VehicleDao {

    /** 查询所有车辆，按车牌号排序 */
    @Query("SELECT * FROM vehicles ORDER BY plateNumber ASC")
    fun getAllVehicles(): Flow<List<Vehicle>>

    /** 根据车牌号查询单辆车 */
    @Query("SELECT * FROM vehicles WHERE plateNumber = :plate")
    suspend fun getVehicleByPlate(plate: String): Vehicle?

    /** 模糊搜索：车牌号、品牌、使用人 */
    @Query("""
        SELECT * FROM vehicles 
        WHERE plateNumber LIKE '%' || :query || '%' 
           OR brand LIKE '%' || :query || '%'
           OR ownerName LIKE '%' || :query || '%'
        ORDER BY plateNumber ASC
    """)
    fun searchVehicles(query: String): Flow<List<Vehicle>>

    /** 查询保养逾期的车辆（当前里程 >= 下次保养里程） */
    @Query("SELECT * FROM vehicles WHERE nextMaintainMileage IS NOT NULL AND currentMileage >= nextMaintainMileage")
    fun getOverdueMaintainVehicles(): Flow<List<Vehicle>>

    /** 统计车辆总数 */
    @Query("SELECT COUNT(*) FROM vehicles")
    suspend fun getCount(): Int

    /** 插入车辆，冲突时替换 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vehicle: Vehicle)

    /** 更新车辆 */
    @Update
    suspend fun update(vehicle: Vehicle)

    /** 删除车辆 */
    @Delete
    suspend fun delete(vehicle: Vehicle)

    /** 根据车牌号删除 */
    @Query("DELETE FROM vehicles WHERE plateNumber = :plate")
    suspend fun deleteByPlate(plate: String)
}
