package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.MaintenanceRecord
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.DashboardViewModel

/**
 * 主页仪表盘 — 统计卡片 + 待办提醒 + 最近记录
 */
@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit) {
    val vm: DashboardViewModel = viewModel()
    val overdue by vm.overdue.collectAsState()
    val upcoming by vm.upcoming.collectAsState()
    val vehicleCount by vm.vehicleCount.collectAsState()
    val recordCount by vm.recordCount.collectAsState()
    val reminders by vm.reminders.collectAsState()
    val recentRecords by vm.recentRecords.collectAsState()
    val vehicles by vm.vehicles.collectAsState()

    LaunchedEffect(Unit) { vm.loadDashboard() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)
    ) {
        Text("🏠 仪表盘", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

        // === 统计卡片（横向滚动） ===
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            item { StatCard("⚠️ 逾期保养", overdue, Color(0xFFEA4335)) }
            item { StatCard("⏰ 即将保养", upcoming, Color(0xFFFBBC04)) }
            item { StatCard("🏍️ 车辆总数", vehicleCount, Color(0xFF1A73E8)) }
            item { StatCard("🔧 维修记录", recordCount, Color(0xFF34A853)) }
        }

        Spacer(Modifier.height(20.dp))

        // === 待办提醒 ===
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("⏰ 待办提醒", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (reminders.isEmpty()) {
                    Text("暂无待办提醒 ✅", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reminders.forEach { r ->
                        val v = vehicles.find { it.plateNumber == r.plateNumber }
                        val remainingInfo = when (r.type) {
                            ReminderType.MILEAGE -> {
                                val rem = v?.let { (it.nextMaintainMileage ?: 0.0) - it.currentMileage }
                                if (rem != null && rem > 0) "剩余 ${rem.toInt()} km" else "已逾期"
                            }
                            ReminderType.DATE -> {
                                val rem = Formatters.getRemainingDays(v?.nextMaintainDate)
                                if (rem != null && rem > 0) "剩余 $rem 天" else "已逾期"
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth().clickable { onNavigateToDetail(r.plateNumber) }.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(r.plateNumber, fontWeight = FontWeight.Medium)
                                Text(r.content, style = MaterialTheme.typography.bodySmall)
                            }
                            Surface(color = Color(0xFFFBBC04).copy(alpha = 0.15f), shape = MaterialTheme.shapes.extraSmall) {
                                Text(remainingInfo, Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // === 最近记录 ===
        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(2.dp)) {
            Column(Modifier.padding(14.dp)) {
                Text("📋 最近保养记录", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                if (recentRecords.isEmpty()) {
                    Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    recentRecords.forEach { r ->
                        Row(Modifier.fillMaxWidth().clickable { onNavigateToDetail(r.plateNumber) }.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(r.plateNumber, fontWeight = FontWeight.Medium)
                                    Spacer(Modifier.width(6.dp))
                                    RecordTypeChip(r.type)
                                }
                                Text(Formatters.formatDate(r.date) + " · " + r.description.take(15), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Formatters.formatMoney(r.cost), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                        if (r != recentRecords.last()) Divider(Modifier.padding(vertical = 2.dp))
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp)) // 底部留白避免被 NavigationBar 遮挡
    }
}

/** 统计卡片组件 */
@Composable
fun StatCard(label: String, value: Int, color: Color) {
    Card(
        Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value.toString(), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.headlineSmall, color = color)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
