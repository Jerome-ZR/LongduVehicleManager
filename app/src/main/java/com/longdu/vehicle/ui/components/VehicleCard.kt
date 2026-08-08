package com.longdu.vehicle.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.longdu.vehicle.data.entity.Vehicle
import com.longdu.vehicle.util.Formatters

/**
 * 车辆卡片组件（首页网格使用）
 * 显示车牌号、品牌型号、当前里程
 */
@Composable
fun VehicleCard(vehicle: Vehicle, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // 车牌号 + 车身编号
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.plateNumber, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                vehicle.bodyNumber?.let {
                    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.extraSmall) {
                        Text(" [$it]", Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // 品牌型号
            Text("${vehicle.brand} ${vehicle.model}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            // 当前里程
            Text(Formatters.formatMileage(vehicle.currentMileage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)

            // 保养状态标签
            val remaining = Formatters.getRemainingMileage(vehicle.currentMileage, vehicle.nextMaintainMileage)
            remaining?.let {
                Spacer(Modifier.height(6.dp))
                val isOverdue = it < 0
                Surface(
                    color = if (isOverdue) Color(0xFFEA4335).copy(alpha = 0.1f) else Color(0xFF34A853).copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.extraSmall
                ) {
                    Text(
                        if (isOverdue) "已超${-it.toInt()}km" else "剩${it.toInt()}km",
                        Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = if (isOverdue) Color(0xFFEA4335) else Color(0xFF34A853),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

/**
 * 空状态提示组件
 */
@Composable
fun EmptyState(icon: String, message: String, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
