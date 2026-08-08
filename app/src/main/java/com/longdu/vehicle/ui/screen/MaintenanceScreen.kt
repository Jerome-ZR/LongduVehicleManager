package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
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
import com.longdu.vehicle.viewmodel.MaintenanceViewModel

/**
 * 维修/保养记录页 — 独立 Tab，展示所有记录
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintenanceScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit) {
    val vm: MaintenanceViewModel = viewModel()
    val allRecords by vm.allRecords.collectAsState()
    val plateMap by vm.vehiclePlateMap.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<MaintenanceRecord?>(null) }
    var filterType by remember { mutableStateOf<RecordType?>(null) }

    // 筛选后的列表
    val filtered = allRecords.filter { r ->
        (filterType == null || r.type == filterType) &&
        (searchQuery.isBlank() || r.description.contains(searchQuery, true) || r.plateNumber.contains(searchQuery, true))
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("🔧 维修 / 保养", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

        // 搜索 + 添加
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(searchQuery, { searchQuery = it }, placeholder = { Text("搜索…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium)
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onNavigateToAdd) { Icon(Icons.Filled.Add, "添加") }
        }

        Spacer(Modifier.height(8.dp))

        // 类型筛选
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(filterType == null, onClick = { filterType = null }, label = { Text("全部") })
            RecordType.entries.forEach { type ->
                FilterChip(filterType == type, onClick = { filterType = if (filterType == type) null else type },
                    label = { Text(when(type) { RecordType.MAINTENANCE -> "保养"; RecordType.REPAIR -> "维修"; RecordType.INSPECTION -> "年检"; RecordType.INSURANCE -> "保险" }) })
            }
        }

        Text("共 ${filtered.size} 条", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("暂无记录") }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { record ->
                    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(2.dp)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(plateMap[record.plateNumber] ?: record.plateNumber, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(8.dp))
                                    RecordTypeChip(record.type)
                                }
                                Text(Formatters.formatDate(record.date) + " · " + record.description.take(20), style = MaterialTheme.typography.bodySmall)
                                Text(Formatters.formatMileage(record.mileage), style = MaterialTheme.typography.bodySmall)
                            }
                            Text(Formatters.formatMoney(record.cost), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            IconButton(onClick = { deleteTarget = record }) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    deleteTarget?.let { r ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("删除「${r.description}」记录？") },
            confirmButton = { TextButton(onClick = { vm.deleteRecord(r); deleteTarget = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}
