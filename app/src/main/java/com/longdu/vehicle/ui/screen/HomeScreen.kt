package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.ui.components.EmptyState
import com.longdu.vehicle.ui.components.VehicleCard
import com.longdu.vehicle.viewmodel.VehicleListViewModel

/**
 * 首页 — 车辆卡片网格 + 搜索 + FAB 添加车辆
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit) {
    val vm: VehicleListViewModel = viewModel()
    val vehicles by vm.vehicles.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val errorMsg by vm.errorMsg.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf<Vehicle?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("龙都车辆管理") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNavigateToAdd) {
                Icon(Icons.Filled.Add, "添加车辆")
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            // 搜索栏
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it; vm.search(it) },
                placeholder = { Text("搜索车牌号/品牌/使用人…") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                singleLine = true, shape = MaterialTheme.shapes.medium
            )

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (vehicles.isEmpty()) {
                EmptyState("🏍️", "暂无车辆\n点击右下角添加")
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(vehicles, key = { it.plateNumber }) { vehicle ->
                        VehicleCard(
                            vehicle = vehicle,
                            onClick = { onNavigateToDetail(vehicle.plateNumber) },
                            onLongClick = { showDeleteDialog = vehicle }
                        )
                    }
                }
            }
        }
    }

    // 删除确认弹窗
    showDeleteDialog?.let { v ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("确认删除") },
            text = { Text("确定删除车辆「${v.plateNumber}」？") },
            confirmButton = { TextButton(onClick = { vm.deleteVehicle(v); showDeleteDialog = null }) { Text("删除", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = null }) { Text("取消") } }
        )
    }

    // 错误 SnackBar
    errorMsg?.let {
        LaunchedEffect(it) { vm.clearError() }
        Snackbar(modifier = Modifier.padding(16.dp)) { Text(it) }
    }
}
