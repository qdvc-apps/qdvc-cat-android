package qdvc.cat.android.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import qdvc.cat.android.app.data.SettingsRepository
import qdvc.cat.android.app.data.ThemeRepository
import qdvc.cat.android.app.model.CustomFontSet
import qdvc.cat.android.app.model.FontIds
import qdvc.cat.android.app.model.FontVariant
import qdvc.cat.android.app.model.ThemeMode
import qdvc.cat.android.app.model.ThemeSpec
import qdvc.cat.android.app.util.CustomFont
import qdvc.cat.android.app.util.SystemFont

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: SettingsRepository.Settings,
    lightThemes: List<ThemeSpec>,
    darkThemes: List<ThemeSpec>,
    systemFonts: List<SystemFont>,
    customFont: CustomFont?,
    onThemeMode: (ThemeMode) -> Unit,
    onLightTheme: (String) -> Unit,
    onDarkTheme: (String) -> Unit,
    onFontSize: (Int) -> Unit,
    onWordWrap: (Boolean) -> Unit,
    onFontId: (String) -> Unit,
    onSelectCustomFont: () -> Unit,
    onPickCustomVariant: (FontVariant, Uri) -> Unit,
    onClearCustomVariant: (FontVariant) -> Unit,
    onBack: () -> Unit,
) {
    // Sub-page toggle: main settings vs the font picker vs custom-font slots.
    var page by remember { mutableStateOf(FontPage.MAIN) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(page.title) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (page == FontPage.MAIN) onBack() else page = FontPage.MAIN
                    }) {
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
            when (page) {
                FontPage.MAIN -> MainSettings(
                    settings = settings,
                    lightThemes = lightThemes,
                    darkThemes = darkThemes,
                    systemFonts = systemFonts,
                    onThemeMode = onThemeMode,
                    onLightTheme = onLightTheme,
                    onDarkTheme = onDarkTheme,
                    onFontSize = onFontSize,
                    onWordWrap = onWordWrap,
                    onOpenFontPicker = { page = FontPage.FONT },
                )
                FontPage.FONT -> FontPicker(
                    selectedId = settings.fontId,
                    systemFonts = systemFonts,
                    customFontSet = settings.customFontSet,
                    onFontId = onFontId,
                    onSelectCustom = onSelectCustomFont,
                    onOpenCustom = { page = FontPage.CUSTOM },
                )
                FontPage.CUSTOM -> CustomFontSlots(
                    customFontSet = settings.customFontSet,
                    onPick = onPickCustomVariant,
                    onClear = onClearCustomVariant,
                )
            }
        }
    }
}

private enum class FontPage(val title: String) {
    MAIN("Settings"),
    FONT("Font"),
    CUSTOM("Custom font"),
}

@Composable
private fun MainSettings(
    settings: SettingsRepository.Settings,
    lightThemes: List<ThemeSpec>,
    darkThemes: List<ThemeSpec>,
    systemFonts: List<SystemFont>,
    onThemeMode: (ThemeMode) -> Unit,
    onLightTheme: (String) -> Unit,
    onDarkTheme: (String) -> Unit,
    onFontSize: (Int) -> Unit,
    onWordWrap: (Boolean) -> Unit,
    onOpenFontPicker: () -> Unit,
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

    SectionTitle("Font")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenFontPicker() }
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Typeface", style = MaterialTheme.typography.bodyLarge)
        Text(
            fontDisplayName(settings.fontId, systemFonts),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    Divider(Modifier.padding(vertical = 16.dp))

    SectionTitle("Text")
    Text("Font size: ${settings.fontSizeSp} sp", style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun FontPicker(
    selectedId: String,
    systemFonts: List<SystemFont>,
    customFontSet: CustomFontSet,
    onFontId: (String) -> Unit,
    onSelectCustom: () -> Unit,
    onOpenCustom: () -> Unit,
) {
    SectionTitle("Font")
    Text(
        "Choose the typeface used to display files. Device fonts are discovered automatically.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp),
    )

    FontChoiceRow(
        label = "Default (Monospace)",
        preview = FontFamily.Monospace,
        selected = selectedId == FontIds.DEFAULT,
        onClick = { onFontId(FontIds.DEFAULT) },
    )

    Divider(Modifier.padding(vertical = 12.dp))
    SectionTitle("Custom font")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FontChoiceRow(
            label = if (customFontSet.hasAny) "Custom font" else "Custom font (none loaded)",
            preview = FontFamily.Default,
            selected = selectedId == FontIds.CUSTOM,
            onClick = { if (customFontSet.hasAny) onSelectCustom() },
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onOpenCustom) { Text("Manage") }
    }

    if (systemFonts.isNotEmpty()) {
        Divider(Modifier.padding(vertical = 12.dp))
        SectionTitle("Device fonts")
        for (font in systemFonts) {
            FontChoiceRow(
                label = font.displayName,
                preview = font.fontFamily,
                selected = selectedId == font.id,
                onClick = { onFontId(font.id) },
            )
        }
    }
}

@Composable
private fun CustomFontSlots(
    customFontSet: CustomFontSet,
    onPick: (FontVariant, Uri) -> Unit,
    onClear: (FontVariant) -> Unit,
) {
    SectionTitle("Custom font files")
    Text(
        "Supply a .ttf or .otf file for each style. Regular is used for normal text; the others are used where a format's highlighting calls for bold or italic.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 12.dp),
    )
    for (variant in FontVariant.entries) {
        CustomFontSlotRow(
            variant = variant,
            name = customFontSet.nameFor(variant),
            onPick = { uri -> onPick(variant, uri) },
            onClear = { onClear(variant) },
        )
    }
}

@Composable
private fun CustomFontSlotRow(
    variant: FontVariant,
    name: String?,
    onPick: (Uri) -> Unit,
    onClear: () -> Unit,
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> if (uri != null) onPick(uri) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(variant.label, style = MaterialTheme.typography.bodyLarge)
            Text(
                name ?: "Not set",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (name != null) {
            TextButton(onClick = onClear) { Text("Clear") }
        }
        OutlinedButton(onClick = { launcher.launch("*/*") }) {
            Text(if (name == null) "Choose" else "Replace")
        }
    }
}

@Composable
private fun FontChoiceRow(
    label: String,
    preview: FontFamily,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                "AaBbCc 0123 {}",
                fontFamily = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun fontDisplayName(id: String?, systemFonts: List<SystemFont>): String {
    if (id == null || id == FontIds.DEFAULT) return "Default (Monospace)"
    if (id == FontIds.CUSTOM) return "Custom font"
    return systemFonts.firstOrNull { it.id == id }?.displayName ?: "Default (Monospace)"
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
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .padding(start = 4.dp)
            .size(20.dp)
            .clip(CircleShape),
    ) {}
}
