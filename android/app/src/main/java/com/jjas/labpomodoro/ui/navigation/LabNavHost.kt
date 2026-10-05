package com.jjas.labpomodoro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jjas.labpomodoro.ui.guide.GuideScreen
import com.jjas.labpomodoro.ui.lab.LabScreen
import com.jjas.labpomodoro.ui.main.MainScreen
import com.jjas.labpomodoro.ui.pro.ProScreen
import com.jjas.labpomodoro.ui.settings.SettingsScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val PRO = "pro"
    const val GUIDE = "guide"
}

@Composable
fun LabNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onOpenPro = { navController.navigate(Routes.PRO) },
                onOpenGuide = { navController.navigate(Routes.GUIDE) { launchSingleTop = true } },
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack, onOpenPro = { navController.navigate(Routes.PRO) })
        }
        // Destinos provisionales; cada uno se implementa en su fase
        composable(Routes.ACHIEVEMENTS) {
            LabScreen(onBack = navController::popBackStack)
        }
        composable(Routes.PRO) {
            ProScreen(onBack = navController::popBackStack)
        }
        composable(Routes.GUIDE) {
            GuideScreen(onDone = navController::popBackStack)
        }
    }
}
