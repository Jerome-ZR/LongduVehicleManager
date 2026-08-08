package com.longdu.vehicle.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.longdu.vehicle.data.dao.MaintenanceRecordDao
import com.longdu.vehicle.data.dao.VehicleDao
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Vehicle

/** Room 数据库单例（v3：移除 Part / ReminderRule 表，后续重做） */
@Database(
    entities = [Vehicle::class, MaintenanceRecord::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun maintenanceRecordDao(): MaintenanceRecordDao

    companion object {
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS parts")
                db.execSQL("DROP TABLE IF EXISTS reminder_rules")
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, AppDatabase::class.java, "longdu_vehicle.db"
                ).addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
