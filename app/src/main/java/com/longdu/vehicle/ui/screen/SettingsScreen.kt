package com.longdu.vehicle.ui.screen

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.longdu.vehicle.data.database.AppDatabase
import com.longdu.vehicle.repository.VehicleRepository
import com.longdu.vehicle.service.ReminderWorker
import com.longdu.vehicle.util.BackupManager
import com.longdu.vehicle.util.SettingsManager
import com.longdu.vehicle.viewmodel.DashboardViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * 设置页面 — 提醒阈值 / 数据导入导出 / 备份还原 / 清空数据
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val vm: DashboardViewModel = viewModel()
    val vehicleCount by vm.vehicleCount.collectAsState()
    val recordCount by vm.recordCount.collectAsState()
    val scope = rememberCoroutineScope()

    val settingsMgr = remember { SettingsManager(context) }
    val backupMgr = remember { BackupManager(context) }
    val repo = remember {
        VehicleRepository(
            AppDatabase.getInstance(context).vehicleDao(),
            AppDatabase.getInstance(context).maintenanceRecordDao(),
            AppDatabase.getInstance(context).partDao(),
            AppDatabase.getInstance(context).reminderRuleDao()
        )
    }

    var partCount by remember { mutableIntStateOf(0) }
    var maintainKm by remember { mutableStateOf(settingsMgr.maintainMileageThreshold.toInt().toString()) }
    var maintainDays by remember { mutableStateOf(settingsMgr.maintainDaysThreshold.toString()) }
    var inspectDays by remember { mutableStateOf(settingsMgr.inspectionDaysThreshold.toString()) }
    var periodHours by remember { mutableStateOf(settingsMgr.reminderPeriodHours.toString()) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var restoreJson by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { withContext(Dispatchers.IO) { partCount = repo.getPartCount() } }

    // 导出文件选择器
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = backupMgr.exportAllData()
                    backupMgr.saveToUri(context, it, json)
                    message = "导出成功 ✅"
                } catch (e: Exception) { message = "导出失败：${e.message}" }
            }
        }
    }

    // 导入/备份文件选择器
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val json = backupMgr.readFromUri(context, it)
                    restoreJson = json
                    showRestoreConfirm = true
                } catch (e: Exception) { message = "读取文件失败：${e.message}" }
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("⚙️ 设置", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

        // === 保养提醒阈值 ===
        SectionTitle("🔧 保养提醒阈值")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(maintainKm, { maintainKm = it; settingsMgr.maintainMileageThreshold = it.toDoubleOrNull() ?: 500.0 },
                label = { Text("公里数(km)") }, modifier = Modifier.weight(1f), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            OutlinedTextField(maintainDays, { maintainDays = it; settingsMgr.maintainDaysThreshold = it.toIntOrNull() ?: 30 },
                label = { Text("天数") }, modifier = Modifier.weight(1f), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
        }

        Spacer(Modifier.height(12.dp))

        // === 年审提醒阈值 ===
        SectionTitle("📋 年审提醒阈值")
        OutlinedTextField(inspectDays, { inspectDays = it; settingsMgr.inspectionDaysThreshold = it.toIntOrNull() ?: 30 },
            label = { Text("提前天数(天)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

        Spacer(Modifier.height(12.dp))

        // === 提醒周期 ===
        SectionTitle("⏱️ 后台提醒检查周期")
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(periodHours, { periodHours = it; settingsMgr.reminderPeriodHours = it.toIntOrNull() ?: 12 },
                label = { Text("间隔(小时)") }, singleLine = true, modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.width(8.dp))
            Button(onClick = {
                val hours = periodHours.toLongOrNull() ?: 12L
                val req = PeriodicWorkRequestBuilder<ReminderWorker>(hours, TimeUnit.HOURS).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork("reminder_check", ExistingPeriodicWorkPolicy.REPLACE, req)
                message = "已更新 ✅"
            }) { Text("应用") }
        }

        Spacer(Modifier.height(16.dp))

        // === 数据操作 ===
        SectionTitle("💾 数据备份与还原")
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { exportLauncher.launch("龙都车辆管理_备份.json") }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Upload, null); Spacer(Modifier.width(4.dp)); Text("导出数据")
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Download, null); Spacer(Modifier.width(4.dp)); Text("导入/还原")
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 数据统计 ===
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Column(Modifier.padding(16.dp)) {
            Text("📊 数据统计", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            StatRow("车辆总数", "$vehicleCount"); StatRow("保养记录", "$recordCount"); StatRow("配件数量", "$partCount")
        }}

        Spacer(Modifier.height(12.dp))

        // 关于
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) { Column(Modifier.padding(16.dp)) {
            Text("ℹ️ 关于", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text("龙都车辆管理平台"); Text("版本 v1.5.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }}

        Spacer(Modifier.height(12.dp))

        // 清空数据
        Card(Modifier.fillMaxWidth().clickable { showClearDialog = true }, shape = MaterialTheme.shapes.medium) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(12.dp))
            Text("🗑️ 清空所有数据", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
        }}

        Spacer(Modifier.height(80.dp))
    }

    // 清空确认弹窗
    if (showClearDialog) AlertDialog(
        onDismissRequest = { showClearDialog = false },
        title = { Text("⚠️ 确认清空") },
        text = { Text("建议先导出备份！清空后不可恢复。") },
        confirmButton = { TextButton(onClick = {
            scope.launch { repo.clearAllData(); vm.loadDashboard(); message = "已清空" }
            showClearDialog = false
        }) { Text("确认清空", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
    )

    // 导入确认弹窗
    if (showRestoreConfirm) AlertDialog(
        onDismissRequest = { showRestoreConfirm = false; restoreJson = "" },
        title = { Text("📥 确认导入") },
        text = { Text("导入将覆盖现有数据（建议先导出备份），确定继续？") },
        confirmButton = { TextButton(onClick = {
            scope.launch { if (backupMgr.importFromJson(restoreJson)) { vm.loadDashboard(); message = "导入成功 ✅" } else message = "导入失败 ❌" }
            showRestoreConfirm = false
        }) { Text("确认导入") } },
        dismissButton = { TextButton(onClick = { showRestoreConfirm = false }) { Text("取消") } }
    )

    // Toast 消息
    message?.let { msg ->
        LaunchedEffect(msg) { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show(); kotlinx.coroutines.delay(2000); message = null }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(bottom = 4.dp))
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
