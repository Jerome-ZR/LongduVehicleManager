package com.longdu.vehicle.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.longdu.vehicle.viewmodel.DashboardViewModel

/**
 * 主页仪表盘 — 统计卡片 + 待办提醒 + 最近记录
 */
@Composable
fun HomeScreen(onNavigateToDetail: (String) -> Unit, onNavigateToAdd: () -> Unit, onNavigateToAddRecord: () -> Unit = {}) {
    val vm: DashboardViewModel = viewModel()
    val overdue by vm.overdue.collectAsState()
    val upcoming by vm.upcoming.collectAsState()
    val vehicleCount by vm.vehicleCount.collectAsState()
    val recordCount by vm.recordCount.collectAsState()