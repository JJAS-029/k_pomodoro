package com.jjas.labpomodoro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jjas.labpomodoro.ui.main.MainScreen
import com.jjas.labpomodoro.ui.main.PlaceholderScreen
import com.jjas.labpomodoro.ui.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val PRO = "pro"
}

@Composable
fun LabNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onOpenPro = { navController.navigate(Routes.PRO) },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack)
        }
        // Destinos provisionales; cada uno se implementa en su fase
        composable(Routes.ACHIEVEMENTS) {
            PlaceholderScreen(title = "Logros", phase = 4, onBack = navController::popBackStack)
        }
        composable(Routes.PRO) {
            PlaceholderScreen(title = "Pro", phase = 5, onBack = navController::popBackStack)
        }
    }
}
