package com.longdu.vehicle.service

import android.Manifest.permission.POST_NOTIFICATIONS
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.repository.VehicleRepository
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 提醒 Worker — 由 WorkManager 定时调度
 * 查询所有启用的提醒规则，检查是否达到触发阈值，发送系统通知
 */
class ReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val db = AppDatabase.getInstance(appContext)
    private val repo = VehicleRepository(
        db.vehicleDao(), db.maintenanceRecordDao(), db.partDao(), db.reminderRuleDao()
    )

    override suspend fun doWork(): Result {
        try {
            // 1. 查询所有启用的提醒规则
            val rules = mutableListOf<com.longdu.vehicle.data.entity.ReminderRule>()
            repo.getAllEnabledRules().collect { rules.addAll(it); return@collect }

            // 2. 对每条规则检查是否满足触发条件
            rules.forEach { rule ->
                val vehicle = repo.getVehicleByPlate(rule.plateNumber) ?: return@forEach
                var shouldNotify = false

                when (rule.type) {
                    ReminderType.MILEAGE -> {
                        // 距下次保养的剩余里程 <= 阈值时触发
                        val nextMileage = vehicle.nextMaintainMileage ?: return@forEach
                        val remaining = nextMileage - vehicle.currentMileage
                        if (remaining > 0 && remaining <= rule.threshold) {
                            shouldNotify = true
                        }
                    }
                    ReminderType.DATE -> {
                        // 距下次保养的剩余天数 <= 阈值时触发
                        val nextDate = vehicle.nextMaintainDate ?: return@forEach
                        val remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), nextDate)
                        if (remainingDays in 1..rule.threshold.toLong()) {
                            shouldNotify = true
                        }
                    }
                }

                if (shouldNotify) {
                    sendNotification(vehicle.plateNumber, rule.content)
                }
            }
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    /**
     * 发送系统通知
     */
    private fun sendNotification(plateNumber: String, content: String) {
        // 创建通知渠道（Android 8.0+ 必需）
        val channelId = "vehicle_reminder"
        val channel = NotificationChannel(
            channelId, "车辆提醒", NotificationManager.IMPORTANCE_DEFAULT
        )
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)

        // 构建通知
        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("车辆提醒 - $plateNumber")
            .setContentText(content.ifEmpty { "请检查车辆保养状态" })
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // 检查通知权限后发送
        if (ContextCompat.checkSelfPermission(
                applicationContext, POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(applicationContext)
                .notify(plateNumber.hashCode(), notification)
        }
    }
}
