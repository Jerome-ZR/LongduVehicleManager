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
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.RecordType
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.VehicleDetailViewModel

/**
 * 车辆详情页 — TabRow 切换保养记录/配件列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleDetailScreen(plate: String, onBack: () -> Unit, onAddRecord: (String) -> Unit, onAddPart: (String) -> Unit) {
    val vm: VehicleDetailViewModel = viewModel()
    val vehicle by vm.vehicle.collectAsState()
    val records by vm.records.collectAsState()
    val parts by vm.parts.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(plate) { vm.loadVehicle(plate) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(vehicle?.plateNumber ?: plate) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        val v = vehicle ?: return@Scaffold
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 车辆信息摘要
            Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), shape = MaterialTheme.shapes.medium) {
                Column(Modifier.padding(12.dp)) {
                    Text("${v.brand} ${v.model} · ${v.color}", fontWeight = FontWeight.Medium)
                    Text(Formatters.formatMileage(v.currentMileage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    v.ownerName.takeIf { it.isNotEmpty() }?.let { Text("使用人：$it", style = MaterialTheme.typography.bodySmall) }
                }
            }

            // Tab 切换
            TabRow(selectedTabIndex = tabIndex) {
                Tab(selected = tabIndex == 0, onClick = { tabIndex = 0 }, text = { Text("保养记录 (${records.size})") })
                Tab(selected = tabIndex == 1, onClick = { tabIndex = 1 }, text = { Text("配件 (${parts.size})") })
            }

            when (tabIndex) {
                0 -> MaintenanceRecordList(records, onDelete = { vm.deleteRecord(it) }, onAdd = { onAddRecord(plate) })
                1 -> PartsList(parts, onDelete = { vm.deletePart(it) }, onAdd = { onAddPart(plate) })
            }
        }
    }
}

@Composable
private fun MaintenanceRecordList(records: List<MaintenanceRecord>, onDelete: (MaintenanceRecord) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onAdd) { Text("➕ 添加记录") }
        }
        if (records.isEmpty()) {
            com.longdu.vehicle.ui.components.EmptyState("📋", "暂无保养记录")
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
                            IconButton(onClick = { onDelete(r) }) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
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

@Composable
private fun PartsList(parts: List<Part>, onDelete: (Part) -> Unit, onAdd: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onAdd) { Text("➕ 添加配件") }
        }
        if (parts.isEmpty()) {
            com.longdu.vehicle.ui.components.EmptyState("🔩", "暂无配件")
        } else {
            LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(parts, key = { it.id }) { p ->
                    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(p.partName, fontWeight = FontWeight.Medium)
                                Text("${p.supplier} · ${p.category.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(Formatters.formatMoney(p.price), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { onDelete(p) }) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
                        }
                    }
                }
            }
        }
    }
}
