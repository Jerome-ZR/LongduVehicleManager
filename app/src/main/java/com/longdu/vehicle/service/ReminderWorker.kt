package com.longdu.vehicle.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 提醒 Worker — 基于车辆保养数据实时检查
 * 遍历所有车辆，检查逾期或即将到期的保养，发送系统通知
 */
class ReminderWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

    private val db = AppDatabase.getInstance(appContext)
    private val repo = VehicleRepository(db.vehicleDao(), db.maintenanceRecordDao(), db.partDao(), db.reminderRuleDao())

    override suspend fun doWork(): Result {
        try {
            val vehicles = repo.getAllVehicles().first()
            val prefs = applicationContext.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
            val kmThreshold = prefs.getFloat("maintain_mileage_threshold", 500f).toDouble()
            val daysThreshold = prefs.getInt("maintain_days_threshold", 30)
            val today = LocalDate.now()

            vehicles.forEach { v ->
                val remainingKm = v.nextMaintainMileage?.let { it - v.currentMileage }
                val remainingDays = v.nextMaintainDate?.let { ChronoUnit.DAYS.between(today, it) }

                val shouldNotify = when {
                    // 已逾期
                    remainingKm != null && remainingKm < 0 -> true
                    remainingDays != null && remainingDays < 0 -> true
                    // 即将到期
                    remainingKm != null && remainingKm > 0 && remainingKm <= kmThreshold -> true
                    remainingDays != null && remainingDays in 1..daysThreshold.toLong() -> true
                    else -> false
                }

                if (shouldNotify) {
                    val msg = buildString {
                        append("${v.plateNumber}")
                        v.bodyNumber?.let { append(" [$it]") }
                        remainingKm?.let {
                            append(if (it < 0) " 已超${-it.toInt()}km" else " 剩余${it.toInt()}km")
                        }
                        remainingDays?.let {
                            append(if (it < 0) " 已超${-it}天" else " 剩余${it}天")
                        }
                    }
                    sendNotification(v.plateNumber, msg)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private fun sendNotification(plate: String, content: String) {
        val channelId = "vehicle_reminder"
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(NotificationChannel(channelId, "车辆保养提醒", NotificationManager.IMPORTANCE_DEFAULT))

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED) return

        NotificationManagerCompat.from(applicationContext).notify(plate.hashCode(),
            NotificationCompat.Builder(applicationContext, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⚠️ 车辆保养提醒")
                .setContentText(content)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
        )
    }
}
