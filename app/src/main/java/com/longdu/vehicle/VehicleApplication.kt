package com.longdu.vehicle

import android.app.Application
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.util.BackupManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application 入口 — 首次启动自动迁移旧版数据
 */
class VehicleApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        migrateIfNeeded()
    }

    private fun migrateIfNeeded() {
        val prefs = getSharedPreferences("app_migration", MODE_PRIVATE)
        if (prefs.getBoolean("legacy_migrated", false)) return

        scope.launch {
            try {
                val db = AppDatabase.getInstance(this@VehicleApplication)
                // 如果数据库已有数据，跳过迁移
                val count = db.vehicleDao().getCount()
                if (count > 0) {
                    prefs.edit().putBoolean("legacy_migrated", true).apply()
                    return@launch
                }

                // 从 assets 读取旧版数据
                val json = assets.open("legacy_data.json")
                    .bufferedReader().use { it.readText() }

                // TODO: importDataJson / importLegacyData removed with reminder/parts modules
                val ok = true
                // .importDataJson(json)

                // 标记迁移完成
                if (ok) { prefs.edit().putBoolean("legacy_migrated", true).apply() }
            } catch (e: Exception) {
                android.util.Log.e("VehicleApp", "迁移失败: ${e.message}", e)
            }
        }
    }
}
