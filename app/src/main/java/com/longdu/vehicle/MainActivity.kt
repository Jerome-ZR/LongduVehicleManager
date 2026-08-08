package com.longdu.vehicle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.longdu.vehicle.ui.VehicleTheme
import com.longdu.vehicle.ui.screen.*

/**
 * 底部导航栏 Tab 定义
 */
enum class NavTab(val label: String, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    HOME("主页", Icons.Filled.Home, Icons.Outlined.Home),
    VEHICLES("车辆", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar),
    REMINDERS("提醒", Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PARTS("配件", Icons.Filled.Build, Icons.Outlined.Build),
    SETTINGS("设置", Icons.Filled.Settings, Icons.Outlined.Settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleTheme { MainScreen() }
        }
    }
}

/**
 * 主导航容器：底部 NavigationBar + 内容区 NavHost
 */
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var selectedTab by remember { mutableStateOf(NavTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            // 切换 Tab 时回到起始路由
                            navController.navigate(tab.name) {
                                popUpTo(navController.graph.startDestinationId) { saveState = false }
                                launchSingleTop = true
                                restoreState = false
                            }
                        },
                        icon = { Icon(if (selectedTab == tab) tab.selectedIcon else tab.unselectedIcon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = NavTab.HOME.name,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ===== 主页 =====
            composable(NavTab.HOME.name) {
                HomeScreen(
                    onNavigateToDetail = { navController.navigate("detail/$it") },
                    onNavigateToAdd = { navController.navigate("addVehicle") }
                )
            }

            // ===== 车辆列表 =====
            composable(NavTab.VEHICLES.name) {
                VehicleListScreen(
                    onNavigateToDetail = { navController.navigate("detail/$it") },
                    onNavigateToAdd = { navController.navigate("addVehicle") }
                )
            }

            // ===== 提醒 =====
            composable(NavTab.REMINDERS.name) {
                ReminderScreen(onNavigateToDetail = { navController.navigate("detail/$it") })
            }

            // ===== 配件 =====
            composable(NavTab.PARTS.name) {
                PartsListScreen(onAddPart = { navController.navigate("addPart") })
            }

            // ===== 设置 =====
            composable(NavTab.SETTINGS.name) { SettingsScreen() }

            // ===== 子路由 =====
            composable("addVehicle") {
                AddEditVehicleScreen(onBack = { navController.popBackStack() })
            }

            composable("detail/{plate}", arguments = listOf(navArgument("plate") { type = NavType.StringType })) { entry ->
                val plate = entry.arguments?.getString("plate") ?: ""
                VehicleDetailScreen(
                    plate = plate, onBack = { navController.popBackStack() },
                    onAddRecord = { navController.navigate("addRecord/$it") }
                )
            }

            composable("addRecord/{plate}", arguments = listOf(navArgument("plate") { type = NavType.StringType })) { entry ->
                AddRecordScreen(plate = entry.arguments?.getString("plate") ?: "", onBack = { navController.popBackStack() })
            }

            composable("addPart") {
                AddPartScreen(plate = "", onBack = { navController.popBackStack() })
            }
        }
    }
}
