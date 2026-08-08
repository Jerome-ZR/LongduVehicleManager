package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.ReminderRule
import com.longdu.vehicle.data.entity.ReminderType
import com.longdu.vehicle.viewmodel.ReminderViewModel

/**
 * 提醒设置页面
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(plate: String, onBack: () -> Unit) {
    val vm: ReminderViewModel = viewModel()
    val rules by vm.rules.collectAsStateWithLifecycle()
    val allVehicles by vm.allVehicles.collectAsStateWithLifecycle()

    LaunchedEffect(plate) { vm.loadRules(plate) }

    var showAddDialog by remember { mutableStateOf(false) }
    var addPlate by remember { mutableStateOf(plate) }
    var addType by remember { mutableStateOf(ReminderType.MILEAGE) }
    var addThreshold by remember { mutableStateOf("") }
    var addContent by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("提醒设置") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (rules.isEmpty()) {
                com.longdu.vehicle.ui.components.EmptyState("🔔", "暂无提醒规则\n点击下方添加")
            } else {
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(rules, key = { it.id }) { rule ->
                        RuleCard(rule, onToggle = { vm.toggleRule(rule) }, onDelete = { vm.deleteRule(rule) })
                    }
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            OutlinedButton(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, null); Spacer(Modifier.width(4.dp)); Text("添加提醒规则")
            }
        }
    }

    // 添加规则弹窗
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("添加提醒规则") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // 关联车辆下拉
                    var vehExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = vehExpanded, onExpandedChange = { vehExpanded = it }) {
                        OutlinedTextField(addPlate, {}, readOnly = true, label = { Text("车辆") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = vehExpanded) },
                            modifier = Modifier.menuAnchor())
                        ExposedDropdownMenu(expanded = vehExpanded, onDismissRequest = { vehExpanded = false }) {
                            allVehicles.forEach { v ->
                                DropdownMenuItem(text = { Text(v.plateNumber) }, onClick = { addPlate = v.plateNumber; vehExpanded = false })
                            }
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(addType == ReminderType.MILEAGE, { addType = ReminderType.MILEAGE }, label = { Text("按里程") })
                        FilterChip(addType == ReminderType.DATE, { addType = ReminderType.DATE }, label = { Text("按日期") })
                    }

                    OutlinedTextField(addThreshold, { addThreshold = it }, label = { Text(if (addType == ReminderType.MILEAGE) "阈值(km)" else "阈值(天)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    OutlinedTextField(addContent, { addContent = it }, label = { Text("提醒内容") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addRule(addPlate, addType, addThreshold.toDoubleOrNull() ?: 0.0, addContent)
                    showAddDialog = false; addThreshold = ""; addContent = ""
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun RuleCard(rule: ReminderRule, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(rule.plateNumber, fontWeight = FontWeight.Medium)
                Text(rule.content.ifEmpty { "阈值：${rule.threshold}" }, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = rule.isEnabled, onCheckedChange = { onToggle() })
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除") }
        }
    }
}
