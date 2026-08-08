package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.viewmodel.ReminderViewModel

/**
 * 提醒页面 — 所有提醒规则列表 + 添加 FAB
 */
@Composable
fun ReminderScreen(onNavigateToDetail: (String) -> Unit) {
    val vm: ReminderViewModel = viewModel()
    val rules by vm.rules.collectAsState()
    val allVehicles by vm.allVehicles.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // 页面首次加载时读取所有规则（不从特定车牌加载）
    LaunchedEffect(Unit) { vm.loadAllRules() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Text("⏰ 所有提醒规则", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

            if (rules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无提醒规则，点击右下角添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(rule, onToggle = { vm.toggleRule(rule) }, onDelete = { vm.deleteRule(rule) }, onClickPlate = { onNavigateToDetail(rule.plateNumber) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }

        // FAB 添加提醒
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) { Icon(Icons.Filled.Add, "添加提醒") }
    }

    // 添加提醒弹窗
    if (showAddDialog) AddReminderDialog(allVehicles, onDismiss = { showAddDialog = false }, onAdd = { plate, type, threshold, content ->
        vm.addRule(plate, type, threshold, content); showAddDialog = false
    })
}

@Composable
private fun RuleCard(rule: ReminderRule, onToggle: () -> Unit, onDelete: () -> Unit, onClickPlate: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.plateNumber, fontWeight = FontWeight.Medium, modifier = Modifier.clickable { onClickPlate() })
                Text("${if (rule.type == ReminderType.MILEAGE) "按里程" else "按日期"} · 阈值: ${rule.threshold}", style = MaterialTheme.typography.bodySmall)
                if (rule.content.isNotBlank()) Text(rule.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Switch(checked = rule.isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddReminderDialog(
    allVehicles: List<com.longdu.vehicle.data.entity.Vehicle>,
    onDismiss: () -> Unit,
    onAdd: (String, ReminderType, Double, String) -> Unit
) {
    var plate by remember { mutableStateOf(allVehicles.firstOrNull()?.plateNumber ?: "") }
    var type by remember { mutableStateOf(ReminderType.MILEAGE) }
    var threshold by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var vehExpanded by remember { mutableStateOf(false) }

    AlertDialog(onDismissRequest = onDismiss, title = { Text("添加提醒规则") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ExposedDropdownMenuBox(expanded = vehExpanded, onExpandedChange = { vehExpanded = it }) {
                OutlinedTextField(plate, {}, readOnly = true, label = { Text("车辆") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehExpanded) }, modifier = Modifier.menuAnchor())
                ExposedDropdownMenu(expanded = vehExpanded, onDismissRequest = { vehExpanded = false }) {
                    allVehicles.forEach { v ->
                        DropdownMenuItem(text = { Text(v.plateNumber) }, onClick = { plate = v.plateNumber; vehExpanded = false })
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(type == ReminderType.MILEAGE, { type = ReminderType.MILEAGE }, label = { Text("按里程") })
                FilterChip(type == ReminderType.DATE, { type = ReminderType.DATE }, label = { Text("按日期") })
            }
            OutlinedTextField(threshold, { threshold = it }, label = { Text(if (type == ReminderType.MILEAGE) "阈值(km)" else "阈值(天)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
            OutlinedTextField(content, { content = it }, label = { Text("提醒内容") }, singleLine = true)
        }
    }, confirmButton = { TextButton(onClick = { onAdd(plate, type, threshold.toDoubleOrNull() ?: 0.0, content) }) { Text("添加") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } })
}
