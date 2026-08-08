package com.longdu.vehicle.ui.screen

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
import com.longdu.vehicle.viewmodel.DashboardViewModel
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * 设置页面
 */
@Composable
fun SettingsScreen() {
    val vm: DashboardViewModel = viewModel()
    val vehicleCount by vm.vehicleCount.collectAsState()
    val recordCount by vm.recordCount.collectAsState()
    // 单独统计配件数
    var partCount by remember { mutableIntStateOf(0) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showPeriodDialog by remember { mutableStateOf(false) }
    var periodHours by remember { mutableStateOf("12") }
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            val repo = VehicleRepository(
                AppDatabase.getInstance(context).vehicleDao(),
                AppDatabase.getInstance(context).maintenanceRecordDao(),
                AppDatabase.getInstance(context).partDao(),
                AppDatabase.getInstance(context).reminderRuleDao()
            )
            partCount = repo.getPartCount()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Text("⚙️ 设置", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

        // 数据统计
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp)) {
                Text("📊 数据统计", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                StatRow("车辆总数", "$vehicleCount")
                StatRow("保养记录", "$recordCount")
                StatRow("配件数量", "$partCount")
            }
        }

        Spacer(Modifier.height(12.dp))

        // 提醒周期设置
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Row(Modifier.fillMaxWidth().clickable { showPeriodDialog = true }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("⏱️ 提醒周期", fontWeight = FontWeight.Medium)
                    Text("当前：每 ${periodHours} 小时检查一次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(12.dp))

        // 清空数据
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Row(
                Modifier.fillMaxWidth().clickable { showClearDialog = true }.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DeleteForever, null, tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("🗑️ 清空所有数据", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Medium)
                    Text("将删除所有车辆、记录、配件和提醒", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // 关于
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
            Column(Modifier.padding(16.dp)) {
                Text("ℹ️ 关于", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text("龙都车辆管理平台", style = MaterialTheme.typography.bodyMedium)
                Text("版本 v1.0.0", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(80.dp))
    }

    // 清空数据确认弹窗
    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("确认清空") },
            text = { Text("清空后所有数据将不可恢复，确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val repo = VehicleRepository(
                            AppDatabase.getInstance(context).vehicleDao(),
                            AppDatabase.getInstance(context).maintenanceRecordDao(),
                            AppDatabase.getInstance(context).partDao(),
                            AppDatabase.getInstance(context).reminderRuleDao()
                        )
                        repo.clearAllData()
                        vm.loadDashboard()
                    }
                    showClearDialog = false
                }) { Text("确认清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { showClearDialog = false }) { Text("取消") } }
        )
    }

    // 提醒周期修改弹窗
    if (showPeriodDialog) {
        AlertDialog(
            onDismissRequest = { showPeriodDialog = false },
            title = { Text("提醒检查周期") },
            text = {
                OutlinedTextField(
                    periodHours, { periodHours = it },
                    label = { Text("间隔小时数") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val hours = periodHours.toLongOrNull() ?: 12L
                    val req = PeriodicWorkRequestBuilder<ReminderWorker>(hours, TimeUnit.HOURS).build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork("reminder_check", ExistingPeriodicWorkPolicy.REPLACE, req)
                    showPeriodDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showPeriodDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
