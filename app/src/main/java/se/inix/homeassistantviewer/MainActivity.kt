package se.inix.homeassistantviewer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.map
import se.inix.homeassistantviewer.data.model.FavoriteItem
import se.inix.homeassistantviewer.data.settings.ThemeMode
import se.inix.homeassistantviewer.di.AppViewModelProvider
import se.inix.homeassistantviewer.ui.about.AboutScreen
import se.inix.homeassistantviewer.ui.connections.ConnectionsScreen
import se.inix.homeassistantviewer.ui.dashboard.DashboardScreen
import se.inix.homeassistantviewer.ui.dashboard.cards.CardSpacing
import se.inix.homeassistantviewer.ui.dashboard.cards.LocalCardSpacing
import se.inix.homeassistantviewer.ui.detail.ConnectionPoolDataSource
import se.inix.homeassistantviewer.ui.detail.EntityDetailScreen
import se.inix.homeassistantviewer.ui.detail.EntityDetailViewModel
import se.inix.homeassistantviewer.ui.picker.EntityPickerScreen
import se.inix.homeassistantviewer.ui.settings.SettingsScreen
import se.inix.homeassistantviewer.ui.settings.SettingsViewModel
import se.inix.homeassistantviewer.ui.theme.HomeAssistantStugaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // SettingsViewModel is owned by the Activity — the same repository
            // instance propagates the theme change to the SettingsScreen's
            // ViewModel automatically.
            val settingsViewModel: SettingsViewModel =
                viewModel(factory = AppViewModelProvider.Factory)
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val colorPalette by settingsViewModel.colorPalette.collectAsStateWithLifecycle()
            val density by settingsViewModel.density.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT   -> false
                ThemeMode.DARK    -> true
                ThemeMode.SYSTEM  -> isSystemInDarkTheme()
            }

            HomeAssistantStugaTheme(darkTheme = darkTheme, palette = colorPalette) {
                // Binds the chosen density once at the root of the
                // composition so every card and the grid read it via
                // LocalCardSpacing without anyone passing it around.
                CompositionLocalProvider(
                    LocalCardSpacing provides CardSpacing.forDensity(density)
                ) {
                    StugaApp()
                }
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
                onNavigateToEntityPicker = { navController.navigate("entity_picker") },
                onNavigateToEntityDetail = { connectionId, entityId ->
                    // URL-encode entity IDs so a dot like "sensor.kitchen" passes through
                    // the route segment cleanly. Connection IDs are UUIDs so they don't
                    // need escaping, but we encode both for symmetry and safety.
                    val encodedConn = java.net.URLEncoder.encode(connectionId, "UTF-8")
                    val encodedEntity = java.net.URLEncoder.encode(entityId, "UTF-8")
                    navController.navigate("entity_detail/$encodedConn/$encodedEntity")
                }
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
            EntityPickerScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("connections") {
            ConnectionsScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable("about") {
            AboutScreen(onNavigateBack = { navController.popBackStack() })
        }
        composable(
            route = "entity_detail/{${EntityDetailViewModel.ARG_CONNECTION_ID}}/{${EntityDetailViewModel.ARG_ENTITY_ID}}",
            arguments = listOf(
                navArgument(EntityDetailViewModel.ARG_CONNECTION_ID) { type = NavType.StringType },
                navArgument(EntityDetailViewModel.ARG_ENTITY_ID) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val connectionId = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString(EntityDetailViewModel.ARG_CONNECTION_ID).orEmpty(),
                "UTF-8"
            )
            val entityId = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString(EntityDetailViewModel.ARG_ENTITY_ID).orEmpty(),
                "UTF-8"
            )
            val container = (LocalContext.current.applicationContext as StugaApplication).container
            // Detail VM is constructed lazily — Compose-Nav only enters this block
            // when the route is navigated to, so no history-related code (including
            // Vico) is paid for on the dashboard path.
            val viewModel: EntityDetailViewModel = viewModel(factory = viewModelFactory {
                initializer {
                    val repo = container.settingsRepository
                    EntityDetailViewModel(
                        savedStateHandle = SavedStateHandle(
                            mapOf(
                                EntityDetailViewModel.ARG_CONNECTION_ID to connectionId,
                                EntityDetailViewModel.ARG_ENTITY_ID to entityId
                            )
                        ),
                        dataSource = ConnectionPoolDataSource(container.connectionPool, connectionId),
                        customNameSource = repo.favorites.map { favs ->
                            favs
                                .filterIsInstance<FavoriteItem.Entity>()
                                .firstOrNull {
                                    it.connectionId == connectionId &&
                                        it.entityId == entityId
                                }
                                ?.customName
                        },
                        saveCustomName = { name ->
                            repo.setFavoriteCustomName(connectionId, entityId, name)
                        },
                        // Lets the detail screen dispatch toggles, brightness,
                        // cover positions etc. via the same dispatcher path
                        // the dashboard uses.
                        connectionPool = container.connectionPool
                    )
                }
            })
            EntityDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
