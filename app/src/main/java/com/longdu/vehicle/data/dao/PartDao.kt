package com.longdu.vehicle.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.longdu.vehicle.data.entity.Part
import kotlinx.coroutines.flow.Flow

/**
 * 配件价格数据访问对象
 */
@Dao
interface PartDao {

    /** 查询所有配件 */
    @Query("SELECT * FROM parts ORDER BY category, partName")
    fun getAllParts(): Flow<List<Part>>

    /** 按车牌号查询配件（含通用配件） */
    @Query("SELECT * FROM parts WHERE plateNumber = :plate OR plateNumber = '通用' ORDER BY category, partName")
    fun getPartsByPlate(plate: String): Flow<List<Part>>

    /** 按分类查询 */
    @Query("SELECT * FROM parts WHERE category = :category ORDER BY partName")
    fun getPartsByCategory(category: String): Flow<List<Part>>

    /** 搜索：配件名称或品牌模糊匹配 */
    @Query("SELECT * FROM parts WHERE partName LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY partName")
    fun searchParts(query: String): Flow<List<Part>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(part: Part)

    @Update
    suspend fun update(part: Part)

    @Delete
    suspend fun delete(part: Part)

@Query("SELECT COUNT(*) FROM parts")
    suspend fun getCount(): Int

    @Query("DELETE FROM parts")
    suspend fun deleteAll()

}