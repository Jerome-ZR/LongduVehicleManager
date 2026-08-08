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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 数据备份/恢复/导入/导出管理器（Reminder/Part 相关代码已移除，后续重做） */
class BackupManager(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val repo = VehicleRepository(db.vehicleDao(), db.maintenanceRecordDao())
    private val dateFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    // ==================== XLSX 导出（2 个 Sheet：车辆 / 记录） ====================
    suspend fun exportXlsx(): ByteArray = withContext(Dispatchers.IO) {
        val vehicles = repo.getAllVehicles().first()
        val records = repo.getAllRecords().first()
        val strings = mutableListOf<String>()
        fun s(v: Any?): Int {
            val str = v?.toString() ?: ""
            val idx = strings.indexOf(str)
            return if (idx >= 0) idx else strings.size.also { strings.add(str) }
        }

        data class SheetData(val name: String, val headers: List<String>, val rows: List<List<String>>)
        val sheets = listOf(
            SheetData("车辆", listOf("车牌号","品牌","型号","年份","VIN码","当前里程(km)","购买日期","下次保养里程","下次保养日期","车身编号","颜色","使用人","保养规则","保养间隔(km)","备注"),
                vehicles.map { v -> listOf(v.plateNumber,v.brand,v.model,v.year.toString(),v.vinCode,
                    v.currentMileage.toString(),v.purchaseDate?.format(dateFmt)?: "",
                    v.nextMaintainMileage?.toString()?:"",v.nextMaintainDate?.format(dateFmt)?:"",
                    v.bodyNumber?.toString()?:"",v.color,v.ownerName,v.maintainRule,
                    v.maintainIntervalKm.toString(),v.remark) }),
            SheetData("保养记录", listOf("车牌号","日期","里程(km)","类型","费用(元)","项目描述","维修厂","地点"),
                records.map { r -> listOf(r.plateNumber,r.date.format(dateFmt),r.mileage.toString(),
                    r.type.name,r.cost.toString(),r.description,r.shopName,r.location) })
        )

        java.io.ByteArrayOutputStream().use { bos ->
            ZipOutputStream(bos).use { zip ->
                fun addEntry(name: String, content: String) {
                    zip.putNextEntry(ZipEntry(name)); zip.write(content.toByteArray(Charsets.UTF_8)); zip.closeEntry()
                }
                val sheetCount = 2
                val overrides = (1..sheetCount).joinToString("") { """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" }
                addEntry("[Content_Types].xml", """<?xml version="1.0"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>$overrides<Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/><Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/></Types>""")
                addEntry("_rels/.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""")
                addEntry("xl/styles.xml", """<?xml version="1.0"?><styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><fonts count="2"><font><sz val="11"/></font><font><b/><sz val="11"/><color rgb="FF1A73E8"/></font></fonts><fills count="1"><fill><patternFill patternType="none"/></fill></fills><borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders></styleSheet>""")
                val refs = sheets.withIndex().joinToString("") { (i,s) -> """<sheet name="${s.name}" sheetId="${i+1}" r:id="rId${i+1}"/>""" }
                addEntry("xl/workbook.xml", """<?xml version="1.0"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>$refs</sheets></workbook>""")
                val wbRels = sheets.withIndex().joinToString("") { (i,_) -> """<Relationship Id="rId${i+1}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet${i+1}.xml"/>""" }
                addEntry("xl/_rels/workbook.xml.rels", """<?xml version="1.0"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">$wbRels</Relationships>""")
                val ssItems = strings.withIndex().joinToString("") { (_,str) -> """<si><t xml:space="preserve">${str.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")}</t></si>""" }
                addEntry("xl/sharedStrings.xml", """<?xml version="1.0"?><sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" count="${strings.size}" uniqueCount="${strings.size}">$ssItems</sst>""")
                val cols = listOf("A","B","C","D","E","F","G","H","I","J","K","L","M","N","O")
                sheets.forEachIndexed { si, sheet ->
                    val rowsXml = StringBuilder()
                    rowsXml.append("<row>"); sheet.headers.forEachIndexed { ci, h -> rowsXml.append("""<c r="${cols[ci]}1" t="s" s="1"><v>${s(h)}</v></c>""") }; rowsXml.append("</row>")
                    sheet.rows.forEachIndexed { ri, row ->
                        rowsXml.append("<row>"); row.forEachIndexed { ci, cell -> rowsXml.append("""<c r="${cols[ci]}${ri+2}" t="s"><v>${s(cell)}</v></c>""") }; rowsXml.append("</row>")
                    }
                    addEntry("xl/worksheets/sheet${si+1}.xml", """<?xml version="1.0"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><cols>${cols.take(sheet.headers.size).joinToString("") { """<col min="${it[0].code - 'A'.code + 1}" max="${it[0].code - 'A'.code + 1}" width="16" customWidth="1"/>""" }}</cols><sheetData>$rowsXml</sheetData></worksheet>""")
                }
            }
            bos.toByteArray()
        }
    }

    // ==================== XLSX 导入 ====================
    suspend fun importFromXlsx(bytes: ByteArray): Boolean = withContext(Dispatchers.IO) {
        try {
            val sheets = mutableMapOf<String, List<List<String?>>>()
            var sharedStrings = emptyList<String>()
            ZipInputStream(bytes.inputStream()).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    val content = String(zip.readBytes(), Charsets.UTF_8)
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
            val sheetKeys = sheets.keys.sortedBy { it.removePrefix("sheet").toIntOrNull() ?: 99 }
            fun parseSheet(idx: Int) = sheetKeys.getOrNull(idx)?.let { sheets[it] } ?: emptyList()
            repo.clearAllData()

            parseSheet(0).drop(1).forEach { r ->
                if (r.getOrNull(0).isNullOrBlank()) return@forEach
                repo.insertVehicle(Vehicle(plateNumber = r[0] ?: "", brand = r.getOrNull(1) ?: "", model = r.getOrNull(2) ?: "",
                    year = (r.getOrNull(3)?.toIntOrNull() ?: 2020), vinCode = r.getOrNull(4) ?: "",
                    currentMileage = (r.getOrNull(5)?.toDoubleOrNull() ?: 0.0), purchaseDate = parseD(r.getOrNull(6)),
                    nextMaintainMileage = r.getOrNull(7)?.toDoubleOrNull(), nextMaintainDate = parseD(r.getOrNull(8)),
                    bodyNumber = r.getOrNull(9)?.toIntOrNull(), color = r.getOrNull(10) ?: "", ownerName = r.getOrNull(11) ?: "",
                    maintainRule = r.getOrNull(12) ?: "", maintainIntervalKm = (r.getOrNull(13)?.toDoubleOrNull() ?: 3000.0),
                    remark = r.getOrNull(14) ?: ""))
            }
            parseSheet(1).drop(1).forEach { r ->
                if (r.getOrNull(0).isNullOrBlank()) return@forEach
                repo.insertRecord(MaintenanceRecord(plateNumber = r[0] ?: "", date = parseD(r.getOrNull(1)) ?: LocalDate.now(),
                    mileage = (r.getOrNull(2)?.toDoubleOrNull() ?: 0.0),
                    type = try { RecordType.valueOf(r.getOrNull(3) ?: "MAINTENANCE") } catch (_: Exception) { RecordType.MAINTENANCE },
                    cost = (r.getOrNull(4)?.toDoubleOrNull() ?: 0.0), description = r.getOrNull(5) ?: "",
                    shopName = r.getOrNull(6) ?: "", location = r.getOrNull(7) ?: ""))
            }
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }

    private fun parseSharedStrings(xml: String): List<String> {
        val result = mutableListOf<String>()
        Regex("<si>(.*?)</si>", RegexOption.DOT_MATCHES_ALL).findAll(xml).forEach { m ->
            result.add(m.groupValues[1].replace(Regex("<[^>]+>"), "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">"))
        }
        return result
    }

    private fun parseWorksheet(xml: String, sharedStrings: List<String>): List<List<String?>> {
        val rows = mutableListOf<List<String?>>()
        val rowPattern = Regex("<row[^>]*>(.*?)</row>", RegexOption.DOT_MATCHES_ALL)
        val cellPattern = Regex("""<c r="([A-Z]+)\d+"[^>]*>(?:<v>(.*?)</v>)?</c>""")
        rowPattern.findAll(xml).forEach { rm ->
            val cells = sortedMapOf<Int, String?>()
            cellPattern.findAll(rm.groupValues[1]).forEach { cm ->
                val col = cm.groupValues[1].fold(0) { a, c -> a * 26 + (c - 'A' + 1) } - 1
                val valStr = cm.groupValues[2].takeIf { it.isNotBlank() }
                val isString = rm.groupValues[1].contains("""t="s"""")
                cells[col] = if (isString && valStr != null) sharedStrings.getOrNull(valStr.toIntOrNull() ?: -1) ?: valStr else valStr
            }
            if (cells.isNotEmpty()) rows.add((0..cells.keys.max()).map { cells[it] })
        }
        return rows
    }

    private fun parseD(s: String?): LocalDate? =
        try { s?.trim()?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it, dateFmt) } } catch (_: Exception) { null }

    // ==================== JSON 导出/导入 ====================
    suspend fun exportAllData(): String = withContext(Dispatchers.IO) {
        val vehicles = repo.getAllVehicles().first()
        val records = repo.getAllRecords().first()
        JSONObject().apply {
            put("version", 2); put("exportTime", System.currentTimeMillis()); put("appName", "龙都车辆管理平台")
            put("vehicles", JSONArray().apply { vehicles.forEach { v -> put(JSONObject().apply {
                put("plateNumber",v.plateNumber); put("brand",v.brand); put("model",v.model)
                put("year",v.year); put("vinCode",v.vinCode); put("currentMileage",v.currentMileage)
                if (v.purchaseDate != null) put("purchaseDate", v.purchaseDate.toEpochDay())
                if (v.nextMaintainMileage != null) put("nextMaintainMileage", v.nextMaintainMileage)
                if (v.nextMaintainDate != null) put("nextMaintainDate", v.nextMaintainDate.toEpochDay())
                if (v.bodyNumber != null) put("bodyNumber", v.bodyNumber); put("color",v.color)
                put("ownerName",v.ownerName); put("maintainRule",v.maintainRule)
                put("maintainIntervalKm",v.maintainIntervalKm); put("remark",v.remark)
            })}})
            put("records", JSONArray().apply { records.forEach { r -> put(JSONObject().apply {
                put("plateNumber",r.plateNumber); put("date",r.date.toEpochDay()); put("mileage",r.mileage)
                put("type",r.type.name); put("cost",r.cost); put("description",r.description)
                put("shopName",r.shopName); put("location",r.location)
            })}})
        }.toString(2)
    }

    suspend fun importFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            val ver = root.optInt("version", 0)
            if (ver != 1 && ver != 2) return@withContext false
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
            true
        } catch (e: Exception) { e.printStackTrace(); false }
    }
}
