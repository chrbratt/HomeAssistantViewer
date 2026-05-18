package se.inix.homeassistantviewer.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DensityMedium
import androidx.compose.material.icons.rounded.DensitySmall
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import se.inix.homeassistantviewer.data.settings.ColorPalette
import se.inix.homeassistantviewer.data.settings.Density
import se.inix.homeassistantviewer.data.settings.ThemeMode
import se.inix.homeassistantviewer.di.AppViewModelProvider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToConnections: () -> Unit,
    onNavigateToAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val dashboardColumns by viewModel.dashboardColumns.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val colorPalette by viewModel.colorPalette.collectAsStateWithLifecycle()
    val density by viewModel.density.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.backupFeedbackEvents.collect { feedback ->
            val message = when (feedback) {
                is SettingsViewModel.BackupFeedback.Success -> feedback.message
                is SettingsViewModel.BackupFeedback.Error -> feedback.message
            }
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            NavigationRow(
                icon = Icons.Rounded.Hub,
                title = "Connections",
                subtitle = "Manage Home Assistant installations",
                onClick = onNavigateToConnections
            )

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Dashboard columns", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Number of columns on the main screen.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        listOf(1, 2, 3).forEachIndexed { index, count ->
                            SegmentedButton(
                                selected = dashboardColumns == count,
                                onClick = { viewModel.saveDashboardColumns(count) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = 3),
                                label = { Text("$count") }
                            )
                        }
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Density", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "How tightly cards are packed on the dashboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val densityOptions = listOf(
                        Density.COMFORTABLE to Pair(Icons.Rounded.DensityMedium, "Comfortable"),
                        Density.COMPACT     to Pair(Icons.Rounded.DensitySmall,  "Compact"),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        densityOptions.forEachIndexed { index, (option, meta) ->
                            val (icon, label) = meta
                            SegmentedButton(
                                selected = density == option,
                                onClick = { viewModel.saveDensity(option) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = densityOptions.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = density == option) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Theme", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Override the system appearance or follow it automatically.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val themeOptions = listOf(
                        ThemeMode.SYSTEM to Pair(Icons.Rounded.SettingsBrightness, "System"),
                        ThemeMode.LIGHT  to Pair(Icons.Rounded.LightMode,          "Light"),
                        ThemeMode.DARK   to Pair(Icons.Rounded.DarkMode,           "Dark"),
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        themeOptions.forEachIndexed { index, (mode, meta) ->
                            val (icon, label) = meta
                            SegmentedButton(
                                selected = themeMode == mode,
                                onClick = { viewModel.saveThemeMode(mode) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index, count = themeOptions.size
                                ),
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = themeMode == mode) {
                                        Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }

            ColorPaletteCard(
                selected = colorPalette,
                onSelect = viewModel::saveColorPalette
            )

            BackupRestoreSection(viewModel = viewModel)

            NavigationRow(
                icon = Icons.Rounded.Info,
                title = "About",
                subtitle = "Version, author and more",
                onClick = onNavigateToAbout
            )
        }
    }
}

/**
 * Compact picker for the user's [ColorPalette]. Each row shows the palette's
 * three signature colours so the user gets a real-time preview without
 * having to commit. The currently-selected row is highlighted with a check
 * mark and a tinted background, so selection is obvious at a glance.
 */
@Composable
private fun ColorPaletteCard(
    selected: ColorPalette,
    onSelect: (ColorPalette) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Rounded.Palette,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text("Color palette", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Pick a colour scheme. \"Dynamic\" follows your phone's Material You " +
                    "palette on Android 12+; the others are hand-crafted and look the " +
                    "same on every device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PaletteOption.entries.forEach { option ->
                PaletteRow(
                    option = option,
                    isSelected = option.palette == selected,
                    onClick = { onSelect(option.palette) }
                )
            }
        }
    }
}

@Composable
private fun PaletteRow(
    option: PaletteOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor =
        if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant
    val contentColor =
        if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
        else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        contentColor = contentColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PaletteSwatch(option)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    option.label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.75f)
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Selected",
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun PaletteSwatch(option: PaletteOption) {
    if (option.palette == ColorPalette.DYNAMIC) {
        // The dynamic palette can't be previewed reliably (it depends on
        // the user's wallpaper), so we show a single "auto-magical" glyph
        // tinted with the current scheme's primary instead.
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            option.previewColors.forEach { dot ->
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .background(dot, CircleShape)
                )
            }
        }
    }
}

/**
 * UI-side metadata for each [ColorPalette]. Kept here (not in the data
 * layer) because the labels, descriptions and preview swatches are pure
 * presentation concerns.
 */
private enum class PaletteOption(
    val palette: ColorPalette,
    val label: String,
    val description: String,
    val previewColors: List<Color>
) {
    Dynamic(
        palette = ColorPalette.DYNAMIC,
        label = "Dynamic",
        description = "Follow Material You (Android 12+)",
        previewColors = emptyList()
    ),
    Ocean(
        palette = ColorPalette.OCEAN,
        label = "Ocean",
        description = "Calm cyan and lavender",
        previewColors = listOf(
            Color(0xFF62D2FF),
            Color(0xFFB3CAD5),
            Color(0xFFC3C3EA)
        )
    ),
    Aurora(
        palette = ColorPalette.AURORA,
        label = "Aurora",
        description = "Electric indigo and magenta",
        previewColors = listOf(
            Color(0xFFB4ACFF),
            Color(0xFFC9C2DC),
            Color(0xFFFFB1D5)
        )
    ),
    Sunset(
        palette = ColorPalette.SUNSET,
        label = "Sunset",
        description = "Warm amber with cool teal accents",
        previewColors = listOf(
            Color(0xFFFFB68F),
            Color(0xFFFFB4AA),
            Color(0xFF7AD0CC)
        )
    ),
    Ember(
        palette = ColorPalette.EMBER,
        label = "Ember",
        description = "Graphite with a single orange accent",
        previewColors = listOf(
            Color(0xFFFF8E45),
            Color(0xFFD4D4D8),
            Color(0xFF8CCFFF)
        )
    ),
    Amber(
        palette = ColorPalette.AMBER,
        label = "Amber",
        description = "Warm honey accents on brown-tinted surfaces",
        previewColors = listOf(
            Color(0xFFFFA862),
            Color(0xFFFFCBA5),
            Color(0xFF8CCFFF)
        )
    ),
    Citrine(
        palette = ColorPalette.CITRINE,
        label = "Citrine",
        description = "Warm dark with golden yellow; clear switch states",
        previewColors = listOf(
            Color(0xFFFFD23F),
            Color(0xFFFFCD61),
            Color(0xFFB6CD60)
        )
    );
}

@Composable
private fun NavigationRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
