package com.jjas.labpomodoro.ui.navigation

import androidx.compose.runtime.Composable
import com.jjas.labpomodoro.ui.room.StudyRoomScreen
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.jjas.labpomodoro.ui.guide.GuideScreen
import com.jjas.labpomodoro.ui.lab.LabScreen
import com.jjas.labpomodoro.ui.main.MainScreen
import com.jjas.labpomodoro.ui.pro.ProScreen
import com.jjas.labpomodoro.ui.settings.SettingsScreen
import com.jjas.labpomodoro.ui.stats.StatsScreen

object Routes {
    const val MAIN = "main"
    const val SETTINGS = "settings"
    const val ACHIEVEMENTS = "achievements"
    const val PRO = "pro"
    const val GUIDE = "guide"
    const val STATS = "stats"
    const val STUDY_ROOM = "study_room"

    fun lab(tab: Int) = "$ACHIEVEMENTS?tab=$tab"
    fun progress(tab: Int) = "$STATS?tab=$tab"
}

/** Pestañas del Laboratorio. */
const val LAB_TABLE = 0
const val LAB_SYNTHESIZER = 1

/** Pestañas de Tu progreso. */
const val PROGRESS_STATS = 0
const val PROGRESS_MEDALS = 1
const val PROGRESS_LEAGUE = 2

@Composable
fun LabNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.MAIN) {
        composable(Routes.MAIN) {
            MainScreen(
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenAchievements = { navController.navigate(Routes.lab(LAB_TABLE)) },
                onOpenSynthesizer = { navController.navigate(Routes.lab(LAB_SYNTHESIZER)) },
                onOpenPro = { navController.navigate(Routes.PRO) },
                onOpenGuide = { navController.navigate(Routes.GUIDE) { launchSingleTop = true } },
                onOpenStats = { navController.navigate(Routes.progress(PROGRESS_STATS)) },
                onOpenMedals = { navController.navigate(Routes.progress(PROGRESS_MEDALS)) },
                onOpenLeague = { navController.navigate(Routes.progress(PROGRESS_LEAGUE)) },
                onOpenStudyRoom = { navController.navigate(Routes.STUDY_ROOM) { launchSingleTop = true } },
            )
        }
        composable(Routes.STUDY_ROOM) {
            StudyRoomScreen(onBack = navController::popBackStack, onOpenPro = { navController.navigate(Routes.PRO) })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = navController::popBackStack, onOpenPro = { navController.navigate(Routes.PRO) })
        }
        // Laboratorio (tabla y sintetizador) y Tu progreso (estadísticas, medallas y liga), cada uno con su pestaña inicial
        composable(
            "${Routes.ACHIEVEMENTS}?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = LAB_TABLE }),
        ) { entry ->
            LabScreen(onBack = navController::popBackStack, initialTab = entry.arguments?.getInt("tab") ?: LAB_TABLE)
        }
        composable(
            "${Routes.STATS}?tab={tab}",
            arguments = listOf(navArgument("tab") { type = NavType.IntType; defaultValue = PROGRESS_STATS }),
        ) { entry ->
            StatsScreen(
                onBack = navController::popBackStack,
                onOpenPro = { navController.navigate(Routes.PRO) },
                initialTab = entry.arguments?.getInt("tab") ?: PROGRESS_STATS,
            )
        }
        composable(Routes.PRO) {
            ProScreen(onBack = navController::popBackStack)
        }
        composable(Routes.GUIDE) {
            GuideScreen(onDone = navController::popBackStack)
        }
    }
}
