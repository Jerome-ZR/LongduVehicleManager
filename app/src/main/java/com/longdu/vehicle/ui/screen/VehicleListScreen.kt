package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
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
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.util.Formatters
import com.longdu.vehicle.viewmodel.VehicleListViewModel

/** 汽车车牌号（排列在前面） */
private val CAR_PLATES = setOf("豫J0298警", "豫J0308警", "豫JQ157Q")

/** 排序规则：汽车在前，摩托车在后；同类按车身编号升序 */
private fun sortVehicles(list: List<Vehicle>): List<Vehicle> = list.sortedWith(
    compareByDescending<Vehicle> { CAR_PLATES.contains(it.plateNumber) }
        .thenBy { it.bodyNumber ?: 9999 }
)

/**
 * 车辆列表页 — 卡片样式列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleListScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit) {
    val vm: VehicleListViewModel = viewModel()
    val vehicles by vm.vehicles.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<Vehicle?>(null) }

    Column(Modifier.fillMaxSize()) {
        // 搜索 + 添加
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it; vm.search(it) },
                placeholder = { Text("搜索车牌号/品牌…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.weight(1f), singleLine = true, shape = MaterialTheme.shapes.medium
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(onClick = onNavigateToAdd) { Icon(Icons.Filled.Add, "添加") }
        }

        if (vehicles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("暂无车辆，点击右上角添加", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sortVehicles(vehicles), key = { it.plateNumber }) { vehicle ->
                    VehicleListCard(
                        vehicle = vehicle,
                        onClick = { onNavigateToDetail(vehicle.plateNumber) },
                        onDelete = { showDeleteDialog = vehicle }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) } // 底部留白
            }
        }
    }

    // 删除确认弹窗
    showDeleteDialog?.let { v ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定删除「${v.plateNumber}」？") },
            confirmButton = { TextButton(onClick = { vm.deleteVehicle(v); showDeleteDialog = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") } }
        )
    }
}

/**
 * 车辆列表卡片 — 横向布局：图标 | 信息 | 状态标签
 */
@Composable
fun VehicleListCard(vehicle: Vehicle, onClick: () -> Unit, onDelete: () -> Unit) {
    // 判断状态：逾期(红) / 即将保养(黄) / 正常(绿)
    val remaining = Formatters.getRemainingMileage(vehicle.currentMileage, vehicle.nextMaintainMileage)
    val statusColor = when {
        remaining == null -> Color(0xFF34A853)
        remaining < 0 -> Color(0xFFEA4335)
        remaining <= 500 -> Color(0xFFFBBC04)
        else -> Color(0xFF34A853)
    }
    val statusText = when {
        remaining == null -> "正常"
        remaining < 0 -> "已逾期"
        remaining <= 500 -> "即将保养"
        else -> "正常"
    }
    val vehicleType = if (vehicle.model.contains("摩托") || vehicle.brand.contains("春风")) "🏍️" else "🚗"

    Card(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                // 左侧图标
                Text(vehicleType, style = MaterialTheme.typography.headlineMedium)

                Spacer(Modifier.width(12.dp))

                // 中间信息
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        vehicle.bodyNumber?.let { Text("[$it] ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) }
                        Text(vehicle.plateNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    vehicle.ownerName.takeIf { it.isNotBlank() }?.let { Text("使用人：$it", style = MaterialTheme.typography.bodySmall) }
                    Text("${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.formatMileage(vehicle.currentMileage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }

                // 状态标签
                Surface(color = statusColor.copy(alpha = 0.12f), shape = MaterialTheme.shapes.extraSmall) {
                    Text(statusText, Modifier.padding(horizontal = 8.dp, vertical = 2.dp), color = statusColor, style = MaterialTheme.typography.labelSmall)
                }
            }

            // 底部：剩余里程 + 操作按钮
            Divider()
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (remaining != null) {
                    Text(
                        if (remaining >= 0) "剩余 ${remaining.toInt()} km" else "已超 ${-remaining.toInt()} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = statusColor
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onClick) { Text("查看详情") }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, "删除", tint = Color(0xFFEA4335)) }
                }
            }
        }
    }
}
