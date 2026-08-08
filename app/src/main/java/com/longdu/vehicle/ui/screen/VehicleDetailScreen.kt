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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    plate: String, onBack: () -> Unit, onAddRecord: (String) -> Unit,
    onEditRecord: (Long) -> Unit = {}, onEditVehicle: (String) -> Unit = {}
) {
    val vm: VehicleDetailViewModel = viewModel()
    val vehicle by vm.vehicle.collectAsState()
    val records by vm.records.collectAsState()
    val today = remember { LocalDate.now() }

    LaunchedEffect(plate) { vm.loadVehicle(plate) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(vehicle?.plateNumber ?: plate) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } },
                actions = { IconButton(onClick = { onEditVehicle(plate) }) { Icon(Icons.Filled.Edit, "编辑") } })
        }
    ) { padding ->
        val v = vehicle ?: return@Scaffold

        Column(Modifier.fillMaxSize().padding(padding)) {
            // === 保养统计卡片 ===
            Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        v.bodyNumber?.let { Text("[$it] ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Text(v.plateNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Text("${v.brand} ${v.model}".trim(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    // 保养统计网格
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        StatBubble("当前里程", Formatters.formatMileage(v.currentMileage), Color(0xFF1A73E8))
                        StatBubble("应保养里程", v.nextMaintainMileage?.let { Formatters.formatMileage(it) } ?: "-", Color(0xFFFBBC04))
                        val remainingKm = v.nextMaintainMileage?.let { it - v.currentMileage }
                        val color = when { remainingKm == null -> Color.Gray; remainingKm < 0 -> Color(0xFFEA4335); remainingKm <= 500 -> Color(0xFFFBBC04); else -> Color(0xFF34A853) }
                        StatBubble("剩余里程", remainingKm?.let { if (it < 0) "超${-it.toInt()}km" else "${it.toInt()}km" } ?: "-", color)
                        val remainingDays = v.nextMaintainDate?.let { ChronoUnit.DAYS.between(today, it) }
                        val dayColor = when { remainingDays == null -> Color.Gray; remainingDays < 0 -> Color(0xFFEA4335); remainingDays <= 30 -> Color(0xFFFBBC04); else -> Color(0xFF34A853) }
                        StatBubble("下次保养", v.nextMaintainDate?.let { Formatters.formatDate(it) } ?: "-", dayColor)
                    }
                    Spacer(Modifier.height(4.dp))
                    // 上次保养信息
                    if (v.lastMaintainDate != null || v.lastMaintainKm != null) {
                        Text("上次保养：${v.lastMaintainDate?.let { Formatters.formatDate(it) } ?: ""} ${v.lastMaintainKm?.let { "· ${it.toInt()}km" } ?: ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (v.ownerName.isNotBlank()) Text("使用人：${v.ownerName}", style = MaterialTheme.typography.bodySmall)
                    if (v.maintainRule.isNotBlank()) Text("保养规则：${v.maintainRule}", style = MaterialTheme.typography.bodySmall)
                    v.inspectionDate?.let { Text("审车日期：${Formatters.formatDate(it)}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF1A73E8)) }
                }
            }

            // === 保养记录 ===
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("📋 记录 (${records.size})", fontWeight = FontWeight.Bold)
                TextButton(onClick = { onAddRecord(plate) }) { Text("➕ 添加") }
            }

            if (records.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无记录", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(records, key = { it.id }) { r ->
                        Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(Formatters.formatDate(r.date), fontWeight = FontWeight.Medium)
                                        Spacer(Modifier.width(6.dp))
                                        RecordTypeChip(r.type)
                                    }
                                    if (r.description.isNotBlank()) Text(r.description, style = MaterialTheme.typography.bodySmall)
                                    if (r.shopName.isNotBlank()) Text("📍 ${r.shopName}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
fun StatBubble(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(4.dp)) {
        Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

