package com.longdu.vehicle.util

import android.content.Context
import android.net.Uri
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.data.entity.*
import com.longdu.vehicle.repository.VehicleRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * 数据备份/恢复/导入/导出管理器
 * 支持 JSON 和 XLSX（Excel）格式
 */
class BackupManager(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val repo = VehicleRepository(db.vehicleDao(), db.maintenanceRecordDao(), db.partDao(), db.reminderRuleDao())
    private val settingsMgr = SettingsManager(context)

    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // =================================================================
    // ===== XLSX 格式导出（4个Sheet：车辆/记录/配件/提醒） =====
    // =================================================================

    /** 导出所有数据为 XLSX 字节数组 */
    suspend fun exportXlsx(): ByteArray = withContext(Dispatchers.IO) {
        val vehicles = repo.getAllVehicles().first()
        val records = repo.getAllRecords().first()
        val partsData = repo.getPartsByPlate("通用").first()
        val reminders = repo.getAllReminderRules().first()

        val strings = mutableListOf<String>()
        fun s(v: Any?): Int {
            val str = v?.toString() ?: ""
            val idx = strings.indexOf(str)
            return if (idx >= 0) idx else strings.size.also { strings.add(str) }
        }

        // 为每张表准备行数据（每行是 String 数组的引用索引）
        data class SheetData(val name: String, val headers: List<String>, val rows: List<List<String>>)

        val sheets = listOf(
            SheetData("车辆", listOf("车牌号","品牌","型号","年份","VIN码","当前里程(km)","购买日期","下次保养里程","下次保养日期","车身编号","颜色","使用人","保养规则","保养间隔(km)","备注"),
                vehicles.map { v -> listOf(v.plateNumber,v.brand,v.model,v.year.toString(),v.vinCode,
                    v.currentMileage.toString(),v.purchaseDate?.format(dateFmt)?:"",
                    v.nextMaintainMileage?.toString()?:"",v.nextMaintainDate?.format(dateFmt)?:"",
                    v.bodyNumber?.toString()?:"",v.color,v.ownerName,v.maintainRule,
                    v.maintainIntervalKm.toString(),v.remark) }),
            SheetData("保养记录", listOf("车牌号","日期","里程(km)","类型","费用(元)","项目描述","维修厂","地点"),
                records.map { r -> listOf(r.plateNumber,r.date.format(dateFmt),r.mileage.toString(),
                    r.type.name,r.cost.toString(),r.description,r.shopName,r.location) }),
            SheetData("配件价格", listOf("适用车牌号","配件名称","品牌","分类","参考价格(元)","供应商","更换日期","备注"),
                partsData.map { p -> listOf(p.plateNumber,p.partName,p.brand,p.category.name,
                    p.price.toString(),p.supplier,p.replaceDate?.format(dateFmt)?:"",p.remark) }),
            SheetData("提醒规则", listOf("车牌号","类型","阈值","提醒内容","是否启用"),
                reminders.map { r -> listOf(r.plateNumber,r.type.name,r.threshold.toString(),r.content,r.isEnabled.toString()) })
        )

        // 构建 XLSX ZIP
        java.io.ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zip ->
                fun addEntry(name: String, content: String) {
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content.toByteArray(Charsets.UTF_8))
                    zip.closeEntry()
                }

                // [Content_Types].xml
                addEntry("[Content_Types].xml", """<?xml version="1.0" encoding="UTF-8"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""")
                // _rels/.rels
                addEntry("_rels/.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""")
                // xl/styles.xml
                addEntry("xl/styles.xml", """<?xml version="1.0" encoding="UTF-8"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/></font><font><b/><sz val="11"/><color rgb="FF1A73E8"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
</styleSheet>""")
                // xl/workbook.xml
                val sheetRefs = sheets.withIndex().joinToString("") { (i, s) ->
                    """<sheet name="${s.name}" sheetId="${i+1}" r:id="rId${i+1}"/>""" }
                addEntry("xl/workbook.xml", """<?xml version="1.0" encoding="UTF-8"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>${sheetRefs}</sheets></workbook>""")
                // xl/_rels/workbook.xml.rels
                val wbRels = sheets.withIndex().joinToString("") { (i, _) ->
                    """<Relationship Id="rId${i+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i+1}.xml"/>""" }
                addEntry("xl/_rels/workbook.xml.rels", """<?xml version="1.0" encoding="UTF-8"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$wbRels</Relationships>""")
                // xl/sharedStrings.xml
                val ssItems = strings.withIndex().joinToString("") { (_, str) ->
                    """<si><t xml:space="preserve">${str.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")}</t></si>""" }
                addEntry("xl/sharedStrings.xml", """<?xml version="1.0" encoding="UTF-8"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">$ssItems</sst>""")

                // 每个 Sheet 的 worksheet XML
                val cols = listOf("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O")
                sheets.forEachIndexed { si, sheet ->
                    val rowsXml = StringBuilder()
                    // 表头行 (style index 1 = bold)
                    rowsXml.append("<row>")
                    sheet.headers.forEachIndexed { ci, h ->
                        rowsXml.append("""<c r="${cols[ci]}1" t="s" s="1"><v>${s(h)}</v></c>""") }
                    rowsXml.append("</row>")
                    // 数据行
                    sheet.rows.forEachIndexed { ri, row ->
                        rowsXml.append("<row>")
                        row.forEachIndexed { ci, cell -> rowsXml.append("""<c r="${cols[ci]}${ri+2}" t="s"><v>${s(cell)}</v></c>""") }
                        rowsXml.append("</row>")
                    }
                    addEntry("xl/worksheets/sheet${si+1}.xml", """<?xml version="1.0" encoding="UTF-8"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<cols>${cols.take(sheet.headers.size).joinToString("") { """<col min="${colNum(it)}" max="${colNum(it)}" width="16" customWidth="1"/>""" }}</cols>
<sheetData>$rowsXml</sheetData></worksheet>""")
                }
            }
            bos.toByteArray()
        }
    }

    private fun colNum(col: String): Int = col[0].code - 'A'.code + 1

    // =================================================================
    // ===== XLSX 格式导入 =====
    // =================================================================

    /** 从 XLSX 字节数组导入数据 */
    suspend fun importFromXlsx(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val sheets = mutableMapOf<String, List<List<String?>>>()
            var sharedStrings = emptyList<String>()

            // 解析 ZIP + XML
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val content = zip.readBytes().toString(Charsets.UTF_8)
                    when {
                        entry.name == "xl/sharedStrings.xml" -> sharedStrings = parseSharedStrings(content)
                        entry.name.matches(Regex("xl/worksheets/sheet\\d+\\.xml")) -> {
                            val name = entry.name.removePrefix("xl/worksheets/").removeSuffix(".xml")
                            sheets[name] = parseWorksheet(content, sharedStrings)
                        }
                    }
                    entry = zip.nextEntry
                }
            }

            // 默认使用 sheet1→车辆, sheet2→记录, sheet3→配件, sheet4→提醒
            val sheetKeys = sheets.keys.sortedBy { it.removePrefix("sheet").toIntOrNull() ?: 99 }
            repo.clearAllData()

            fun parseSheet(idx: Int): List<List<String?>> = sheetKeys.getOrNull(idx)?.let { sheets[it] } ?: emptyList()

            // 导入车辆
            parseSheet(0).drop(1).forEach { r ->
                if (r.getOrNull(0).isNullOrBlank()) return@forEach
                repo.insertVehicle(Vehicle(
                    plateNumber = r[0] ?: "", brand = r.getOrNull(1) ?: "", model = r.getOrNull(2) ?: "",
                    year = (r.getOrNull(3)?.toIntOrNull() ?: 2020), vinCode = r.getOrNull(4) ?: "",
                    currentMileage = (r.getOrNull(5)?.toDoubleOrNull() ?: 0.0),
                    purchaseDate = parseD(r.getOrNull(6)),
                    nextMaintainMileage = r.getOrNull(7)?.toDoubleOrNull(),
                    nextMaintainDate = parseD(r.getOrNull(8)),
                    bodyNumber = r.getOrNull(9)?.toIntOrNull(), color = r.getOrNull(10) ?: "",
                    ownerName = r.getOrNull(11) ?: "", maintainRule = r.getOrNull(12) ?: "",
                    maintainIntervalKm = (r.getOrNull(13)?.toDoubleOrNull() ?: 3000.0), remark = r.getOrNull(14) ?: ""
                ))
            }
            // 导入记录
            parseSheet(1).drop(1).forEach { r ->
                if (r.getOrNull(0).isNullOrBlank()) return@forEach
                repo.insertRecord(MaintenanceRecord(
                    plateNumber = r[0] ?: "", date = parseD(r.getOrNull(1)) ?: LocalDate.now(),
                    mileage = (r.getOrNull(2)?.toDoubleOrNull() ?: 0.0),
                    type = try { RecordType.valueOf(r.getOrNull(3) ?: "MAINTENANCE") } catch (_: Exception) { RecordType.MAINTENANCE },
                    cost = (r.getOrNull(4)?.toDoubleOrNull() ?: 0.0), description = r.getOrNull(5) ?: "",
                    shopName = r.getOrNull(6) ?: "", location = r.getOrNull(7) ?: ""
                ))
            }
            // 导入配件
            parseSheet(2).drop(1).forEach { r ->
                if (r.getOrNull(1).isNullOrBlank()) return@forEach
                repo.insertPart(Part(
                    plateNumber = (r.getOrNull(0)?.ifBlank { "通用" } ?: "通用"), partName = r[1] ?: "",
                    brand = r.getOrNull(2) ?: "",
                    category = try { PartCategory.valueOf(r.getOrNull(3) ?: "OTHER") } catch (_: Exception) { PartCategory.OTHER },
                    price = (r.getOrNull(4)?.toDoubleOrNull() ?: 0.0), supplier = r.getOrNull(5) ?: "",
                    replaceDate = parseD(r.getOrNull(6)), remark = r.getOrNull(7) ?: ""
                ))
            }
            // 导入提醒
            parseSheet(3).drop(1).forEach { r ->
                if (r.getOrNull(0).isNullOrBlank()) return@forEach
                repo.insertRule(ReminderRule(
                    plateNumber = r[0] ?: "",
                    type = try { ReminderType.valueOf(r.getOrNull(1) ?: "MILEAGE") } catch (_: Exception) { ReminderType.MILEAGE },
                    threshold = (r.getOrNull(2)?.toDoubleOrNull() ?: 0.0),
                    content = r.getOrNull(3) ?: "",
                    isEnabled = r.getOrNull(4)?.lowercase() != "false"
                ))
            }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    /** 解析 sharedStrings.xml */
    private fun parseSharedStrings(xml: String): List<String> {
        val result = mutableListOf<String>()
        val pattern = Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL)
        pattern.findAll(xml).forEach { m ->
            val t = m.groupValues[1].replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            result.add(t)
        }
        return result
    }

    /** 解析 worksheet XML → List<Row> */
    private fun parseWorksheet(xml: String, sharedStrings: List<String>): List<List<String?>> {
        val rows = mutableListOf<MutableMap<String, String?>>()
        val rowPattern = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellPattern = Regex("<c r=\"([A-Z]+)(\\d+)\"[^>]*>(?:<v>(.*?)</v>)?</c>")
        var currentRow = 0
        rowPattern.findAll(xml).forEach { rm ->
            val rowCells = mutableMapOf<String, String?>()
            cellPattern.findAll(rm.groupValues[1]).forEach { cm ->
                val col = cm.groupValues[1]
                val value = cm.groupValues[3].takeIf { it.isNotBlank() }
                // 数字列是 s (string) 类型
                val isString = rm.groupValues[1].contains("t=\"s\"")
                rowCells[col] = if (isString && value != null) {
                    sharedStrings.getOrNull(value.toIntOrNull() ?: -1) ?: value
                } else value
            }
            if (rowCells.isNotEmpty()) rows.add(rowCells)
        }
        // 补齐缺失的列
        val allCols = ('A'..'P').toList()
        return rows.map { row -> allCols.map { c -> row[c?.toString() ?: ""] } }
    }

    private fun parseD(s: String?): LocalDate? =
        try { s?.trim()?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, dateFmt) } } catch (_: Exception) { null }

    // =================================================================
    // ===== JSON 导出/导入 =====
    // =================================================================

    suspend fun exportAllData(): String = withContext(Dispatchers.IO) {
        val vehicles = repo.getAllVehicles().first()
        val records = repo.getAllRecords().first()
        val pd = repo.getPartsByPlate("通用").first()
        val reminders = repo.getAllReminderRules().first()
        JSONObject().apply {
            put("version", 1); put("exportTime", System.currentTimeMillis()); put("appName", "龙都车辆管理平台")
            put("vehicles", JSONArray().apply { vehicles.forEach { v -> put(JSONObject().apply {
                put("plateNumber", v.plateNumber); put("brand", v.brand); put("model", v.model)
                put("year", v.year); put("vinCode", v.vinCode); put("currentMileage", v.currentMileage)
                if (v.purchaseDate != null) put("purchaseDate", v.purchaseDate.toEpochDay())
                if (v.nextMaintainMileage != null) put("nextMaintainMileage", v.nextMaintainMileage)
                if (v.nextMaintainDate != null) put("nextMaintainDate", v.nextMaintainDate.toEpochDay())
                if (v.bodyNumber != null) put("bodyNumber", v.bodyNumber)
                put("color", v.color); put("ownerName", v.ownerName); put("maintainRule", v.maintainRule)
                put("maintainIntervalKm", v.maintainIntervalKm); put("remark", v.remark)
            }) }})
            put("records", JSONArray().apply { records.forEach { r -> put(JSONObject().apply {
                put("plateNumber", r.plateNumber); put("date", r.date.toEpochDay()); put("mileage", r.mileage)
                put("type", r.type.name); put("cost", r.cost); put("description", r.description)
                put("shopName", r.shopName); put("location", r.location)
            }) }})
            put("parts", JSONArray().apply { pd.forEach { p -> put(JSONObject().apply {
                put("plateNumber", p.plateNumber); put("partName", p.partName); put("brand", p.brand)
                put("category", p.category.name); put("price", p.price); put("supplier", p.supplier)
                if (p.replaceDate != null) put("replaceDate", p.replaceDate.toEpochDay()); put("remark", p.remark)
            }) }})
            put("reminders", JSONArray().apply { reminders.forEach { r -> put(JSONObject().apply {
                put("plateNumber", r.plateNumber); put("type", r.type.name); put("threshold", r.threshold)
                put("content", r.content); put("isEnabled", r.isEnabled)
            }) }})
        }.toString(2)
    }

    suspend fun importFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            if (root.optInt("version", 0) != 1) return@withContext false
            repo.clearAllData()
            val va = root.optJSONArray("vehicles") ?: JSONArray()
            for (i in 0 until va.length()) { val v = va.getJSONObject(i)
                repo.insertVehicle(Vehicle(plateNumber = v.getString("plateNumber"), brand = v.optString("brand"), model = v.optString("model"),
                    year = v.optInt("year", 2020), vinCode = v.optString("vinCode"), currentMileage = v.optDouble("currentMileage", 0.0),
                    purchaseDate = if (v.has("purchaseDate")) LocalDate.ofEpochDay(v.getLong("purchaseDate")) else null,
                    nextMaintainMileage = if (v.has("nextMaintainMileage")) v.getDouble("nextMaintainMileage") else null,
                    nextMaintainDate = if (v.has("nextMaintainDate")) LocalDate.ofEpochDay(v.getLong("nextMaintainDate")) else null,
                    bodyNumber = if (v.has("bodyNumber")) v.getInt("bodyNumber") else null,
                    color = v.optString("color"), ownerName = v.optString("ownerName"), maintainRule = v.optString("maintainRule"),
                    maintainIntervalKm = v.optDouble("maintainIntervalKm", 3000.0), remark = v.optString("remark")))
            }
            val ra = root.optJSONArray("records") ?: JSONArray()
            for (i in 0 until ra.length()) { val r = ra.getJSONObject(i)
                repo.insertRecord(MaintenanceRecord(plateNumber = r.getString("plateNumber"), date = LocalDate.ofEpochDay(r.getLong("date")),
                    mileage = r.optDouble("mileage", 0.0), type = try { RecordType.valueOf(r.getString("type")) } catch (_: Exception) { RecordType.MAINTENANCE },
                    cost = r.optDouble("cost", 0.0), description = r.optString("description"), shopName = r.optString("shopName"), location = r.optString("location")))
            }
            val pa = root.optJSONArray("parts") ?: JSONArray()
            for (i in 0 until pa.length()) { val p = pa.getJSONObject(i)
                repo.insertPart(Part(plateNumber = p.optString("plateNumber", "通用"), partName = p.getString("partName"), brand = p.optString("brand"),
                    category = try { PartCategory.valueOf(p.getString("category")) } catch (_: Exception) { PartCategory.OTHER },
                    price = p.optDouble("price", 0.0), supplier = p.optString("supplier"),
                    replaceDate = if (p.has("replaceDate")) LocalDate.ofEpochDay(p.getLong("replaceDate")) else null, remark = p.optString("remark")))
            }
            val rma = root.optJSONArray("reminders") ?: JSONArray()
            for (i in 0 until rma.length()) { val r = rma.getJSONObject(i)
                repo.insertRule(ReminderRule(plateNumber = r.getString("plateNumber"),
                    type = try { ReminderType.valueOf(r.getString("type")) } catch (_: Exception) { ReminderType.MILEAGE },
                    threshold = r.optDouble("threshold", 0.0), content = r.optString("content"), isEnabled = r.optBoolean("isEnabled", true)))
            }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    suspend fun importLegacyData(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            if (!root.has("data") || root.has("version")) return@withContext false
            val data = root.getJSONObject("data")
            repo.clearAllData()
            data.optJSONArray("vehicles")?.let { va ->
                for (i in 0 until va.length()) { val v = va.getJSONObject(i)
                    val plate = v.getString("plate"); val mt = v.optString("type")
                    repo.insertVehicle(Vehicle(plateNumber = plate, brand = if (mt.contains("摩托")) "春风" else "",
                        model = if (mt.contains("摩托")) "650TR-G" else "",
                        currentMileage = v.optDouble("currentKmNum", 0.0),
                        nextMaintainMileage = v.optDouble("shouldMaintainKmNum", 0.0).takeIf { it > 0 },
                        bodyNumber = if (v.has("bodyNum") && !v.isNull("bodyNum")) v.getInt("bodyNum") else null,
                        ownerName = v.optString("user"), maintainRule = v.optString("maintainRule"),
                        maintainIntervalKm = if (mt.contains("摩托")) 3000.0 else 10000.0, remark = v.optString("remark")))
                }
            }
            val stf = mutableMapOf("Q157Q" to "豫JQ157Q", "豫JQ157Q" to "豫JQ157Q")
            data.optJSONArray("records")?.let { ra ->
                for (i in 0 until ra.length()) { val r = ra.getJSONObject(i)
                    val short = r.optString("plateShort"); val full = stf[short] ?: "豫J${short}警"
                    val cat = r.optString("category", "保养")
                    repo.insertRecord(MaintenanceRecord(plateNumber = full, date = parseOldD(r.optString("date")) ?: LocalDate.now(),
                        type = if (cat.contains("维修")) RecordType.REPAIR else RecordType.MAINTENANCE,
                        cost = r.optDouble("price", 0.0), description = r.optString("project"),
                        shopName = r.optString("location"), location = r.optString("locationNormalized")))
                }
            }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    private fun parseOldD(input: String): LocalDate? {
        if (input.isBlank() || input.contains("无")) return null
        return try { val c = input.replace(".","-").replace("年","-").replace("月","-01").trimEnd('-')
            LocalDate.parse(if (c.count { it == '-' } == 1) "$c-01" else c, DateTimeFormatter.ofPattern("yyyy-M-d")) } catch (_: Exception) { null }
    }
}
