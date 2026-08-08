package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.DashboardViewModel

@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit, onNavigateToAddRecord: () -> Unit = {}) {
    val vm: DashboardViewModel = viewModel()
    val overdue by vm.overdue.collectAsState()
    val upcoming by vm.upcoming.collectAsState()
    val vehicleCount by vm.vehicleCount.collectAsState()
    val recordCount by vm.recordCount.collectAsState()
    val recentRecords by vm.recentRecords.collectAsState()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        // 统计卡片
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("已逾期保养", overdue, Color(0xFFE53935), Modifier.weight(1f))
            StatCard("即将保养", upcoming, Color(0xFFFFA726), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatCard("车辆总数", vehicleCount, Color(0xFF1A73E8), Modifier.weight(1f))
            StatCard("维修记录", recordCount, Color(0xFF43A047), Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))

        // 快捷操作
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ElevatedButton(onClick = onNavigateToAddRecord, Modifier.weight(1f)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("添加记录") }
            OutlinedButton(onClick = onNavigateToAdd, Modifier.weight(1f)) { Icon(Icons.Default.Add, null, Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)); Text("添加车辆") }
        }

        Spacer(Modifier.height(20.dp))

        // 最近记录
        if (recentRecords.isNotEmpty()) {
            Text("最近记录", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            recentRecords.forEach { r ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { onNavigateToDetail(r.plateNumber) }) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.plateNumber, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(Formatters.formatDate(r.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(r.description, style = MaterialTheme.typography.bodyMedium)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(r.shopName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("¥${Formatters.formatPrice(r.cost)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
                        }
                    }
                }
            }
        } else {
            Text("暂无记录", modifier = Modifier.padding(32.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun StatCard(label: String, value: Int, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))) {
        Column(Modifier.padding(16.dp)) {
            Text(value.toString(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
