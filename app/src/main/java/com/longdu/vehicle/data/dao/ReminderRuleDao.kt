package com.longdu.vehicle.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.longdu.vehicle.data.entity.ReminderRule
import kotlinx.coroutines.flow.Flow

/**
 * 提醒规则数据访问对象
 */
@Dao
interface ReminderRuleDao {

    /** 查询所有提醒规则 */
    @Query("SELECT * FROM reminder_rules ORDER BY plateNumber, type")
    fun getAllRules(): Flow<List<ReminderRule>>

    /** 查询某车辆的所有提醒规则 */
    @Query("SELECT * FROM reminder_rules WHERE plateNumber = :plate ORDER BY type")
    fun getRulesByPlate(plate: String): Flow<List<ReminderRule>>

    /** 查询所有启用的提醒规则 */
    @Query("SELECT * FROM reminder_rules WHERE isEnabled = 1 ORDER BY plateNumber")
    fun getEnabledRules(): Flow<List<ReminderRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: ReminderRule)

    @Update
    suspend fun update(rule: ReminderRule)

    /** 切换启用状态 */
    @Query("UPDATE reminder_rules SET isEnabled = :enabled WHERE id = :ruleId")
    suspend fun toggleEnabled(ruleId: Long, enabled: Boolean)

    @Delete
    suspend fun delete(rule: ReminderRule)

@Query("DELETE FROM reminder_rules")
    suspend fun deleteAll()
}
}