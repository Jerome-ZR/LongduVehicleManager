package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.ReminderStatus
import com.longdu.vehicle.viewmodel.ReminderVehicle
import com.longdu.vehicle.viewmodel.ReminderViewModel

/**
 * 提醒页面 — 基于车辆保养数据实时计算，按逾期/即将/正常三组展示
 */
@Composable
fun ReminderScreen(onNavigateToDetail: (String) -> Unit) {
    val vm: ReminderViewModel = viewModel()
    val overdue by vm.overdueVehicles.collectAsState()
    val upcoming by vm.upcomingVehicles.collectAsState()
    val normal by vm.normalVehicles.collectAsState()
    val overdueCount by vm.overdueCount.collectAsState()
    val upcomingCount by vm.upcomingCount.collectAsState()

    LaunchedEffect(Unit) { vm.loadAll() }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("⏰ 保养提醒", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))
        }

        // === 逾期保养 🔴 ===
        if (overdue.isNotEmpty()) {
            item { SectionHeader("🔴 逾期保养", overdueCount, Color(0xFFEA4335)) }
            items(overdue, key = { it.plateNumber }) { v ->
                ReminderCard(v, Color(0xFFEA4335), onClick = { onNavigateToDetail(v.plateNumber) })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // === 即将保养 🟡 ===
        if (upcoming.isNotEmpty()) {
            item { SectionHeader("🟡 即将保养", upcomingCount, Color(0xFFFBBC04)) }
            items(upcoming, key = { it.plateNumber }) { v ->
                ReminderCard(v, Color(0xFFFBBC04), onClick = { onNavigateToDetail(v.plateNumber) })
            }
            item { Spacer(Modifier.height(8.dp)) }
        }

        // === 保养正常 🟢 ===
        if (normal.isNotEmpty()) {
            item { SectionHeader("🟢 保养正常", normal.size, Color(0xFF34A853)) }
            items(normal, key = { it.plateNumber }) { v ->
                ReminderCard(v, Color(0xFF34A853), onClick = { onNavigateToDetail(v.plateNumber) })
            }
        }

        if (overdue.isEmpty() && upcoming.isEmpty() && normal.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                    Text("暂无车辆数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun SectionHeader(title: String, count: Int, color: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = color)
        Spacer(Modifier.width(6.dp))
        Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
            Text("$count", Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = color, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ReminderCard(v: ReminderVehicle, accent: Color, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 3.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    v.bodyNumber?.let { Text("[$it] ", fontWeight = FontWeight.Bold, color = accent) }
                    Text(v.plateNumber, fontWeight = FontWeight.Bold)
                    v.ownerName.takeIf { it.isNotBlank() }?.let { Text(" · $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                Spacer(Modifier.height(2.dp))
                Text("${v.brand} ${v.model}".trim() + " · ${Formatters.formatMileage(v.currentMileage)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            // 右侧：剩余里程/天数
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                v.remainingKm?.let {
                    Text(
                        if (it < 0) "超${-it.toInt()}km" else "剩${it.toInt()}km",
                        fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = accent
                    )
                }
                v.remainingDays?.let {
                    Text(
                        if (it < 0) "超${-it}天" else "剩${it}天",
                        style = MaterialTheme.typography.bodySmall, color = accent.copy(alpha = 0.7f)
                    )
                }
                if (v.nextMaintainDate != null) {
                    Text(Formatters.formatDate(v.nextMaintainDate), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
