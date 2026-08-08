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
 * 车辆详情页 — 完整属性信息 + 保养记录列表（可编辑）
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(
    plate: String, onBack: () -> Unit,
    onAddRecord: (String) -> Unit,
    onEditRecord: (Long) -> Unit = {},
    onEditVehicle: (String) -> Unit = {}
) {
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

        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // === 车辆属性卡片 ===
            item {
                Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            v.bodyNumber?.let { Text("[$it] ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                            Text(v.plateNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("${v.brand} ${v.model}".trim() + if (v.color.isNotBlank()) " · ${v.color}" else "", style = MaterialTheme.typography.bodyMedium)
                        v.ownerName.takeIf { it.isNotBlank() }?.let { Text("使用人：$it", style = MaterialTheme.typography.bodySmall) }
                        v.remark.takeIf { it.isNotBlank() }?.let { Text("备注：$it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }

                        Spacer(Modifier.height(12.dp))
                        Divider()
                        Spacer(Modifier.height(8.dp))

                        // 保养核心数据
                        InfoRow("当前公里数", Formatters.formatMileage(v.currentMileage))
                        v.nextMaintainMileage?.let { InfoRow("应保养公里数", Formatters.formatMileage(it)) }
                        val remaining = v.nextMaintainMileage?.let { it - v.currentMileage }
                        remaining?.let {
                            InfoRow("距保养剩余", "${if (it < 0) "超${-it.toInt()}" else "${it.toInt()}"} km",
                                if (it < 0) Color(0xFFEA4335) else if (it <= 500) Color(0xFFFBBC04) else Color(0xFF34A853))
                        }
                        v.nextMaintainDate?.let {
                            val days = java.time.temporal.ChronoUnit.DAYS.between(java.time.LocalDate.now(), it)
                            InfoRow("下次保养时间", Formatters.formatDate(it),
                                if (days < 0) Color(0xFFEA4335) else if (days <= 30) Color(0xFFFBBC04) else MaterialTheme.colorScheme.onSurface)
                        }
                        v.lastMaintainDate?.let { InfoRow("上次保养时间", Formatters.formatDate(it)) }
                        v.lastMaintainKm?.let { InfoRow("上次保养公里数", Formatters.formatMileage(it)) }
                        v.inspectionDate?.let { InfoRow("审车日期", Formatters.formatDate(it)) }
                        if (v.maintainRule.isNotBlank()) InfoRow("保养规则", v.maintainRule)
                    }
                }
            }

            // === 记录标题 + 添加按钮 ===
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("📋 保养记录 (${records.size})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onAddRecord(plate) }) { Text("➕ 添加") }
                }
            }

            if (records.isEmpty()) {
                item { Text("暂无保养记录", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 16.dp)) }
            } else {
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
                                if (r.shopName.isNotBlank()) Text(r.shopName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(Formatters.formatMileage(r.mileage), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Formatters.formatMoney(r.cost), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onEditRecord(r.id) }) { Icon(Icons.Filled.Edit, "编辑") }
                            IconButton(onClick = { vm.deleteRecord(r) }) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = FontWeight.Medium)
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
