package se.inix.homeassistantviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import se.inix.homeassistantviewer.data.ThemeMode
import se.inix.homeassistantviewer.ui.screens.AboutScreen
import se.inix.homeassistantviewer.ui.screens.ConnectionsScreen
import se.inix.homeassistantviewer.ui.screens.DashboardScreen
import se.inix.homeassistantviewer.ui.screens.EntityPickerScreen
import se.inix.homeassistantviewer.ui.screens.SettingsScreen
import se.inix.homeassistantviewer.ui.theme.HomeAssistantStugaTheme
import se.inix.homeassistantviewer.viewmodel.AppViewModelProvider
import se.inix.homeassistantviewer.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // SettingsViewModel is owned by the Activity — the same repository instance
            // propagates the theme change to the SettingsScreen ViewModel automatically.
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT   -> false
                ThemeMode.DARK    -> true
                ThemeMode.SYSTEM  -> isSystemInDarkTheme()
            }

            HomeAssistantStugaTheme(darkTheme = darkTheme) {
                StugaApp()
            }
        }
    }
}

@Composable
fun StugaApp() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") {
            DashboardScreen(
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToEntityPicker = { navController.navigate("entity_picker") }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToConnections = { navController.navigate("connections") },
                onNavigateToAbout = { navController.navigate("about") }
            )
        }
        composable("entity_picker") {
            EntityPickerScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("connections") {
            ConnectionsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
