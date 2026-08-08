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
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

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

    /**
     * 导入旧版 Web 应用格式的备份数据（自动检测并转换）
     * 返回 true 表示导入成功
     */
    suspend fun importLegacyData(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            // 检测是否为旧格式（有 "data" 顶层键且没有 "version"）
            if (!root.has("data") || root.has("version")) return@withContext false
            val data = root.getJSONObject("data")
            convertAndImport(data)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** 将旧格式 data 对象转换为新实体并导入 */
    private suspend fun convertAndImport(data: JSONObject) {
        val vehiclesArr = data.optJSONArray("vehicles") ?: JSONArray()
        val recordsArr = data.optJSONArray("records") ?: JSONArray()
        val partsArr = data.optJSONArray("parts") ?: JSONArray()

        // 清空旧数据
        repo.clearAllData()

        // ===== 1. 转换车辆 =====
        // 构建车牌号短码 → 完整车牌号映射表
        val shortToFull = mutableMapOf<String, String>()
        val vehiclePlates = mutableListOf<String>()

        for (i in 0 until vehiclesArr.length()) {
            val v = vehiclesArr.getJSONObject(i)
            val plate = v.getString("plate")
            vehiclePlates.add(plate)

            // 解析日期
            val lastMaintainDate = parseOldDate(v.optString("lastMaintainDate"))
            val nextMaintainDate = parseOldDate(v.optString("nextMaintain"))
            val inspectionDate = parseOldDate(v.optString("inspectionDate"))

            // 保养间隔：摩托车 3000km，警车 10000km
            val vehicleType = v.optString("type", "")
            val intervalKm = if (vehicleType.contains("摩托")) 3000.0 else 10000.0

            repo.insertVehicle(Vehicle(
                plateNumber = plate,
                brand = if (vehicleType.contains("摩托")) "春风" else "",
                model = if (vehicleType.contains("摩托")) "650TR-G" else "",
                currentMileage = v.optDouble("currentKmNum", 0.0),
                purchaseDate = parseOldDate(v.optString("lastMaintainDate"))?.minusYears(1),
                nextMaintainMileage = v.optDouble("shouldMaintainKmNum", 0.0).takeIf { it > 0 },
                nextMaintainDate = nextMaintainDate,
                bodyNumber = if (v.has("bodyNum") && !v.isNull("bodyNum")) v.getInt("bodyNum") else null,
                ownerName = v.optString("user", ""),
                maintainRule = v.optString("maintainRule", ""),
                maintainIntervalKm = intervalKm,
                remark = v.optString("remark", "")
            ))

            // 建立短码映射
            val match = Regex("(\\d+)").find(plate)
            if (match != null) {
                shortToFull[match.groupValues[1]] = plate
            }
            v.optInt("bodyNum", -1).takeIf { it > 0 }?.let { shortToFull[it.toString()] = plate }
        }

        // 补充 Q157Q 特殊映射
        shortToFull["Q157Q"] = "豫JQ157Q"
        shortToFull["豫JQ157Q"] = "豫JQ157Q"

        // ===== 2. 转换维修记录 =====
        for (i in 0 until recordsArr.length()) {
            val r = recordsArr.getJSONObject(i)
            val plateShort = r.optString("plateShort", "")
            val fullPlate = shortToFull[plateShort] ?: "豫J${plateShort}警"

            val category = r.optString("category", "保养")
            val type = when {
                category.contains("维修") -> RecordType.REPAIR
                else -> RecordType.MAINTENANCE
            }

            repo.insertRecord(MaintenanceRecord(
                plateNumber = fullPlate,
                date = parseOldDate(r.optString("date")) ?: LocalDate.now(),
                mileage = 0.0,  // 旧数据无里程字段
                type = type,
                cost = r.optDouble("price", 0.0),
                description = r.optString("project", ""),
                shopName = r.optString("location", ""),
                location = r.optString("locationNormalized", "")
            ))
        }

        // ===== 3. 转换配件 =====
        for (i in 0 until partsArr.length()) {
            val p = partsArr.getJSONObject(i)
            val model = p.optString("model", "通用")

            // 铁马/洪亮 两个供应商分别创建配件条目
            val tiemaTotal = p.optDouble("tiemaTotal", -1.0)
            val hongliangTotal = p.optDouble("hongliangTotal", -1.0)

            fun createPart(supplier: String, qty: Int, unitPrice: Double, total: Double) {
                val displayName = if (qty > 0) "${p.getString("name")} (x$qty)" else p.getString("name")
                repo.insertPart(Part(
                    plateNumber = model,
                    partName = displayName,
                    category = guessPartCategory(p.optString("name")),
                    price = total.takeIf { it > 0 } ?: unitPrice,
                    supplier = supplier,
                    remark = p.optString("remark", "")
                ))
            }

            if (tiemaTotal > 0) createPart("铁马机车生活馆", p.optInt("tiemaQty", 0), p.optDouble("tiemaPrice", 0.0), tiemaTotal)
            if (hongliangTotal > 0) createPart("洪亮机车", p.optInt("hongliangQty", 0), p.optDouble("hongliangPrice", 0.0), hongliangTotal)
            if (tiemaTotal <= 0 && hongliangTotal <= 0) {
                repo.insertPart(Part(
                    plateNumber = model, partName = p.getString("name"),
                    category = guessPartCategory(p.getString("name")), remark = p.optString("remark", "")
                ))
            }
        }
    }

    /** 解析旧版日期格式（支持 "2026.3.10" / "2026-04-09" / "2027年2月" / ""） */
    private fun parseOldDate(input: String): LocalDate? {
        if (input.isBlank() || input.contains("无")) return null
        return try {
            // 2026.3.10 或 2026-04-09
            val cleaned = input.replace(".", "-").replace("年", "-").replace("月", "-01").trimEnd('-')
            LocalDate.parse(
                if (cleaned.count { it == '-' } == 1) "$cleaned-01" else cleaned,
                java.time.format.DateTimeFormatter.ofPattern("yyyy-M-d").withResolverStyle(java.time.format.ResolverStyle.LENIENT)
            )
        } catch (_: Exception) { null }
    }

    /** 根据配件名称推断分类 */
    private fun guessPartCategory(name: String): PartCategory = when {
        name.contains("机油") || name.contains("齿轮油") || name.contains("刹车油") || name.contains("防冻液") -> PartCategory.OIL
        name.contains("滤") -> PartCategory.FILTER
        name.contains("轮胎") -> PartCategory.TIRE
        name.contains("刹车片") || name.contains("刹车盘") || name.contains("制动") -> PartCategory.BRAKE
        name.contains("灯") || name.contains("灯泡") -> PartCategory.LIGHT
        name.contains("壳") || name.contains("挡风") || name.contains("反光镜") || name.contains("保险杠") || name.contains("坐垫") -> PartCategory.BODY
        name.contains("电瓶") || name.contains("线") || name.contains("开关") || name.contains("传感器") || name.contains("火花塞") -> PartCategory.ELECTRICAL
        else -> PartCategory.OTHER
    }
}
