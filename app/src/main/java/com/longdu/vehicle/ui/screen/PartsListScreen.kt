package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.Part
import com.longdu.vehicle.data.entity.PartCategory
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.VehicleDetailViewModel

/**
 * 配件价格页面 — 独立 Tab，统一管理所有配件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartsListScreen(onAddPart: () -> Unit) {
    val vm: VehicleDetailViewModel = viewModel()

    // 加载所有通用+各车辆的配件
    LaunchedEffect(Unit) { vm.loadAllParts() }

    val parts by vm.parts.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<PartCategory?>(null) }
    var deleteTarget by remember { mutableStateOf<Part?>(null) }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("🔩 配件价格", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 12.dp))

        // 搜索 + 添加
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                searchQuery, { searchQuery = it }, placeholder = { Text("搜索配件…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onAddPart) { Icon(Icons.Filled.Add, "添加") }
        }

        Spacer(Modifier.height(8.dp))

        // 分类筛选
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(selectedCategory == null, onClick = { selectedCategory = null }, label = { Text("全部") })
            PartCategory.entries.forEach { cat ->
                FilterChip(
                    selectedCategory == cat,
                    onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                    label = { Text(cat.name) }
                )
            }
        }

        // 筛选后的列表
        val filtered = parts.filter { p ->
            (selectedCategory == null || p.category == selectedCategory) &&
            (searchQuery.isBlank() || p.partName.contains(searchQuery, true) || p.supplier.contains(searchQuery, true))
        }

        if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无配件数据", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Text("共 ${filtered.size} 项", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(filtered, key = { it.id }) { part ->
                    PartCard(part, onDelete = { deleteTarget = part })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // 删除确认
    deleteTarget?.let { p ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确认删除") },
            text = { Text("删除配件「${p.partName}」？") },
            confirmButton = { TextButton(onClick = { vm.deletePart(p); deleteTarget = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("取消") } }
        )
    }
}

@Composable
private fun PartCard(part: Part, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(part.partName, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                        Text(part.category.name, Modifier.padding(horizontal = 6.dp, vertical = 1.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                }
                Text("${part.supplier}${if (part.plateNumber != "通用") " · ${part.plateNumber}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (part.remark.isNotBlank()) Text(part.remark, style = MaterialTheme.typography.bodySmall)
            }
            Text(Formatters.formatMoney(part.price), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
        }
    }
}
