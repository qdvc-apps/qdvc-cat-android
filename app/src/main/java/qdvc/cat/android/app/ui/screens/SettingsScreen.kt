package qdvc.cat.android.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qdvc.cat.android.app.data.SettingsRepository
import qdvc.cat.android.app.data.ThemeRepository
import qdvc.cat.android.app.model.ThemeMode
import qdvc.cat.android.app.model.ThemeSpec

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository.Settings,
    lightThemes: List<ThemeSpec>,
    darkThemes: List<ThemeSpec>,
    onThemeMode: (ThemeMode) -> Unit,
    onLightTheme: (String) -> Unit,
    onDarkTheme: (String) -> Unit,
    onFontSize: (Int) -> Unit,
    onWordWrap: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            SectionTitle("Appearance")
            Text(
                "Light / dark mode. \u201CFollow system\u201D uses whichever mode your device is in.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            ModeRow("Follow system", settings.themeMode == ThemeMode.AUTOMATIC) { onThemeMode(ThemeMode.AUTOMATIC) }
            ModeRow("Light", settings.themeMode == ThemeMode.LIGHT) { onThemeMode(ThemeMode.LIGHT) }
            ModeRow("Dark", settings.themeMode == ThemeMode.DARK) { onThemeMode(ThemeMode.DARK) }

            Divider(Modifier.padding(vertical = 16.dp))

            SectionTitle("Light theme")
            themes(lightThemes, settings.lightThemeId, onLightTheme)

            Divider(Modifier.padding(vertical = 16.dp))

            SectionTitle("Dark theme")
            themes(darkThemes, settings.darkThemeId, onDarkTheme)

            Divider(Modifier.padding(vertical = 16.dp))

            SectionTitle("Text")
            Text(
                "Font size: ${settings.fontSizeSp} sp",
                style = MaterialTheme.typography.bodyMedium,
            )
            Slider(
                value = settings.fontSizeSp.toFloat(),
                onValueChange = { onFontSize(it.toInt()) },
                valueRange = SettingsRepository.MIN_FONT_SIZE.toFloat()..SettingsRepository.MAX_FONT_SIZE.toFloat(),
                steps = SettingsRepository.MAX_FONT_SIZE - SettingsRepository.MIN_FONT_SIZE - 1,
            )
            Text(
                "The quick brown fox 123",
                fontFamily = FontFamily.Monospace,
                fontSize = settings.fontSizeSp.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Word wrap", style = MaterialTheme.typography.bodyLarge)
                Switch(checked = settings.wordWrap, onCheckedChange = onWordWrap)
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
}

@Composable
private fun ModeRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onSelect)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun themes(list: List<ThemeSpec>, selectedId: String, onSelect: (String) -> Unit) {
    for (theme in list) {
        val selected = theme.id == selectedId
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(theme.id) }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(selected = selected, onClick = { onSelect(theme.id) })
            Text(theme.name, modifier = Modifier.padding(start = 8.dp).weight(1f))
            Swatch(ThemeRepository.colorScheme(theme).background)
            Swatch(ThemeRepository.colorScheme(theme).primary)
            Swatch(ThemeRepository.colorScheme(theme).secondary)
        }
    }
}

@Composable
private fun Swatch(color: Color) {
    Surface(
        color = color,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .padding(start = 4.dp)
            .size(20.dp)
            .clip(CircleShape),
    ) {}
}
