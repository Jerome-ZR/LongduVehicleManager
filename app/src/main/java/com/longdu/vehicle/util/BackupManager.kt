package com.longdu.vehicle.util

import android.content.Context
import android.net.Uri
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.data.entity.PartCategory
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate

/**
 * 数据备份/恢复/导入/导出管理器
 * 使用 JSON 格式序列化所有 Room 数据 + 设置
 */
class BackupManager(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val repo = VehicleRepository(db.vehicleDao(), db.maintenanceRecordDao(), db.partDao(), db.reminderRuleDao())
    private val settingsMgr = SettingsManager(context)

    /** 导出所有数据为 JSON 字符串 */
    suspend fun exportAllData(): String = withContext(Dispatchers.IO) {
        val vehicles = repo.getAllVehicles().first()
        val records = repo.getAllRecords().first()
        val parts = repo.getPartsByPlate("通用").first()
        val reminders = repo.getAllReminderRules().first()

        JSONObject().apply {
            // 版本标记
            put("version", 1)
            put("exportTime", System.currentTimeMillis())
            put("appName", "龙都车辆管理平台")

            // 车辆
            put("vehicles", JSONArray().apply {
                vehicles.forEach { v ->
                    put(JSONObject().apply {
                        put("plateNumber", v.plateNumber); put("brand", v.brand)
                        put("model", v.model); put("year", v.year)
                        put("vinCode", v.vinCode); put("currentMileage", v.currentMileage)
                        if (v.purchaseDate != null) put("purchaseDate", v.purchaseDate.toEpochDay())
                        if (v.nextMaintainMileage != null) put("nextMaintainMileage", v.nextMaintainMileage)
                        if (v.nextMaintainDate != null) put("nextMaintainDate", v.nextMaintainDate.toEpochDay())
                        if (v.bodyNumber != null) put("bodyNumber", v.bodyNumber)
                        put("color", v.color); put("ownerName", v.ownerName)
                        put("maintainRule", v.maintainRule); put("maintainIntervalKm", v.maintainIntervalKm)
                        put("remark", v.remark)
                    })
                }
            })

            // 保养记录
            put("records", JSONArray().apply {
                records.forEach { r ->
                    put(JSONObject().apply {
                        put("plateNumber", r.plateNumber)
                        put("date", r.date.toEpochDay())
                        put("mileage", r.mileage); put("type", r.type.name)
                        put("cost", r.cost); put("description", r.description)
                        put("shopName", r.shopName); put("location", r.location)
                    })
                }
            })

            // 配件
            put("parts", JSONArray().apply {
                parts.forEach { p ->
                    put(JSONObject().apply {
                        put("plateNumber", p.plateNumber); put("partName", p.partName)
                        put("brand", p.brand); put("category", p.category.name)
                        put("price", p.price); put("supplier", p.supplier)
                        if (p.replaceDate != null) put("replaceDate", p.replaceDate.toEpochDay())
                        put("remark", p.remark)
                    })
                }
            })

            // 提醒规则
            put("reminders", JSONArray().apply {
                reminders.forEach { r ->
                    put(JSONObject().apply {
                        put("plateNumber", r.plateNumber); put("type", r.type.name)
                        put("threshold", r.threshold); put("content", r.content)
                        put("isEnabled", r.isEnabled)
                    })
                }
            })
        }.toString(2)
    }

    /** 从 JSON 导入数据（会先清空旧数据） */
    suspend fun importFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            val version = root.optInt("version", 0)
            if (version != 1) return@withContext false

            // 清空旧数据
            repo.clearAllData()

            // 导入车辆
            val vehiclesArr = root.optJSONArray("vehicles") ?: JSONArray()
            for (i in 0 until vehiclesArr.length()) {
                val v = vehiclesArr.getJSONObject(i)
                repo.insertVehicle(Vehicle(
                    plateNumber = v.getString("plateNumber"),
                    brand = v.optString("brand"), model = v.optString("model"),
                    year = v.optInt("year", 2020), vinCode = v.optString("vinCode"),
                    currentMileage = v.optDouble("currentMileage", 0.0),
                    purchaseDate = if (v.has("purchaseDate")) LocalDate.ofEpochDay(v.getLong("purchaseDate")) else null,
                    nextMaintainMileage = if (v.has("nextMaintainMileage")) v.getDouble("nextMaintainMileage") else null,
                    nextMaintainDate = if (v.has("nextMaintainDate")) LocalDate.ofEpochDay(v.getLong("nextMaintainDate")) else null,
                    bodyNumber = if (v.has("bodyNumber")) v.getInt("bodyNumber") else null,
                    color = v.optString("color"), ownerName = v.optString("ownerName"),
                    maintainRule = v.optString("maintainRule"),
                    maintainIntervalKm = v.optDouble("maintainIntervalKm", 3000.0),
                    remark = v.optString("remark")
                ))
            }

            // 导入记录
            val recordsArr = root.optJSONArray("records") ?: JSONArray()
            for (i in 0 until recordsArr.length()) {
                val r = recordsArr.getJSONObject(i)
                repo.insertRecord(MaintenanceRecord(
                    plateNumber = r.getString("plateNumber"),
                    date = LocalDate.ofEpochDay(r.getLong("date")),
                    mileage = r.optDouble("mileage", 0.0),
                    type = try { RecordType.valueOf(r.getString("type")) } catch (_: Exception) { RecordType.MAINTENANCE },
                    cost = r.optDouble("cost", 0.0),
                    description = r.optString("description"),
                    shopName = r.optString("shopName"),
                    location = r.optString("location")
                ))
            }

            // 导入配件
            val partsArr = root.optJSONArray("parts") ?: JSONArray()
            for (i in 0 until partsArr.length()) {
                val p = partsArr.getJSONObject(i)
                repo.insertPart(Part(
                    plateNumber = p.optString("plateNumber", "通用"),
                    partName = p.getString("partName"),
                    brand = p.optString("brand"),
                    category = try { PartCategory.valueOf(p.getString("category")) } catch (_: Exception) { PartCategory.OTHER },
                    price = p.optDouble("price", 0.0),
                    supplier = p.optString("supplier"),
                    replaceDate = if (p.has("replaceDate")) LocalDate.ofEpochDay(p.getLong("replaceDate")) else null,
                    remark = p.optString("remark")
                ))
            }

            // 导入提醒
            val remindersArr = root.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until remindersArr.length()) {
                val r = remindersArr.getJSONObject(i)
                repo.insertRule(ReminderRule(
                    plateNumber = r.getString("plateNumber"),
                    type = try { ReminderType.valueOf(r.getString("type")) } catch (_: Exception) { ReminderType.MILEAGE },
                    threshold = r.optDouble("threshold", 0.0),
                    content = r.optString("content"),
                    isEnabled = r.optBoolean("isEnabled", true)
                ))
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 保存文本到 Uri（导出使用） */
    suspend fun saveToUri(context: Context, uri: Uri, content: String) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
        }
    }

    /** 从 Uri 读取文本（导入使用） */
    suspend fun readFromUri(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        context.contentResolver.openInputStream(uri)?.use { stream ->
            BufferedReader(InputStreamReader(stream)).use { reader ->
                var line: String? = reader.readLine()
                while (line != null) { sb.appendLine(line); line = reader.readLine() }
            }
        }
        sb.toString()
    }
}
