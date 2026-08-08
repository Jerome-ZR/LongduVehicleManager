package com.longdu.vehicle

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.longdu.vehicle.ui.VehicleTheme
import com.longdu.vehicle.ui.screen.*

/**
 * 主 Activity — Compose 入口，定义全屏导航路由
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VehicleTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToDetail = { nav.navigate("detail/$it") },
                onNavigateToAdd = { nav.navigate("addVehicle") }
            )
        }
        composable("addVehicle") {
            AddEditVehicleScreen(onBack = { nav.popBackStack() })
        }
        composable(
            "detail/{plate}",
            arguments = listOf(navArgument("plate") { type = NavType.StringType })
        ) { entry ->
            val plate = entry.arguments?.getString("plate") ?: ""
            VehicleDetailScreen(
                plate = plate,
                onBack = { nav.popBackStack() },
                onAddRecord = { nav.navigate("addRecord/$it") },
                onAddPart = { nav.navigate("addPart/$it") }
            )
        }
        composable(
            "addRecord/{plate}",
            arguments = listOf(navArgument("plate") { type = NavType.StringType })
        ) { entry ->
            AddRecordScreen(plate = entry.arguments?.getString("plate") ?: "", onBack = { nav.popBackStack() })
        }
        composable(
            "addPart/{plate}",
            arguments = listOf(navArgument("plate") { type = NavType.StringType })
        ) { entry ->
            AddPartScreen(plate = entry.arguments?.getString("plate") ?: "", onBack = { nav.popBackStack() })
        }
        composable(
            "reminderSettings/{plate}",
            arguments = listOf(navArgument("plate") { type = NavType.StringType })
        ) { entry ->
            ReminderSettingsScreen(plate = entry.arguments?.getString("plate") ?: "", onBack = { nav.popBackStack() })
        }
    }
}
