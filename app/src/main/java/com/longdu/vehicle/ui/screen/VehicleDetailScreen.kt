package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.longdu.vehicle.viewmodel.VehicleDetailViewModel

/**
 * 车辆详情页 — 车辆信息 + 保养记录列表（配件已移至独立页面）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(plate: String, onBack: () -> Unit, onAddRecord: (String) -> Unit, onEditRecord: (Long) -> Unit = {}, onEditVehicle: (String) -> Unit = {}) {
    val vm: VehicleDetailViewModel = viewModel()
    val vehicle by vm.vehicle.collectAsState()
    val records by vm.records.collectAsState()

    LaunchedEffect(plate) { vm.loadVehicle(plate) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(vehicle?.plateNumber ?: plate) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { onEditVehicle(plate) }) { Icon(Icons.Filled.Edit, "编辑车辆") } })
        }
    ) { padding ->
        val v = vehicle ?: return@Scaffold

        Column(Modifier.fillMaxSize().padding(padding)) {
            // 车辆信息卡片
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        v.bodyNumber?.let { Text("[$it] ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Text(v.plateNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${v.brand} ${v.model} · ${v.color}", style = MaterialTheme.typography.bodyMedium)
                    Text(Formatters.formatMileage(v.currentMileage), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    v.ownerName.takeIf { it.isNotBlank() }?.let { Text("使用人：$it", style = MaterialTheme.typography.bodySmall) }

                    // 保养状态
                    val remaining = Formatters.getRemainingMileage(v.currentMileage, v.nextMaintainMileage)
                    remaining?.let {
                        Spacer(Modifier.height(6.dp))
                        val isOverdue = it < 0
                        Surface(
                            color = if (isOverdue) Color(0xFFEA4335).copy(alpha = 0.1f) else Color(0xFF34A853).copy(alpha = 0.1f),
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                if (isOverdue) "⚠️ 保养逾期 ${-it.toInt()} km" else "✅ 剩余 ${it.toInt()} km",
                                Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                color = if (isOverdue) Color(0xFFEA4335) else Color(0xFF34A853),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            // 保养记录标题 + 添加按钮
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📋 保养记录 (${records.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = { onAddRecord(plate) }) { Text("➕ 添加") }
            }

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无保养记录", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records, key = { it.id }) { r ->
                        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Formatters.formatDate(r.date), fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(8.dp))
                                        RecordTypeChip(r.type)
                                    }
                                    if (r.description.isNotBlank()) Text(r.description, style = MaterialTheme.typography.bodySmall)
                                    Text(Formatters.formatMileage(r.mileage), style = MaterialTheme.typography.bodySmall)
                                }
                                Text(Formatters.formatMoney(r.cost), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                IconButton(onClick = { onEditRecord(r.id) }) { Icon(Icons.Filled.Edit, "编辑") }
                                IconButton(onClick = { vm.deleteRecord(r) }) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordTypeChip(type: RecordType) {
    val (label, color) = when (type) {
        RecordType.MAINTENANCE -> "保养" to Color(0xFF34A853)
        RecordType.REPAIR -> "维修" to Color(0xFFEA4335)
        RecordType.INSPECTION -> "年检" to Color(0xFF1A73E8)
        RecordType.INSURANCE -> "保险" to Color(0xFFFBBC04)
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
        Text(label, Modifier.padding(horizontal = 6.dp, vertical = 1.dp), color = color, style = MaterialTheme.typography.labelSmall)
    }
}
