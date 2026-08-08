package com.longdu.vehicle.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.longdu.vehicle.data.dao.MaintenanceRecordDao
import com.longdu.vehicle.data.dao.PartDao
import com.longdu.vehicle.data.dao.ReminderRuleDao
import com.longdu.vehicle.data.dao.VehicleDao
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.Vehicle

/**
 * Room 数据库单例
 * 包含 4 张表：vehicles / maintenance_records / parts / reminder_rules
 */
@Database(
    entities = [
        Vehicle::class,
        MaintenanceRecord::class,
        Part::class,
        ReminderRule::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao
    abstract fun partDao(): PartDao
    abstract fun reminderRuleDao(): ReminderRuleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * 获取数据库单例（双重检查锁定，线程安全）
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "longdu_vehicle.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
