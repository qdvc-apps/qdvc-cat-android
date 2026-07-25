package qdvc.cat.android.app.ui

import android.app.Application
import android.net.Uri
import androidx.compose.ui.text.font.FontFamily
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import qdvc.cat.android.app.data.SettingsRepository
import qdvc.cat.android.app.model.CustomFontSet
import qdvc.cat.android.app.model.FontIds
import qdvc.cat.android.app.model.FontVariant
import qdvc.cat.android.app.model.ThemeMode
import qdvc.cat.android.app.util.CustomFont
import qdvc.cat.android.app.util.FileLoader
import qdvc.cat.android.app.util.SystemFont
import qdvc.cat.android.app.util.SystemFonts

/** Screens the app can show. */
enum class Screen { VIEWER, SETTINGS }

sealed interface DocState {
    data object Empty : DocState
    data object Loading : DocState
    data class Loaded(
        val displayName: String?,
        val lines: List<String>,
        val truncated: Boolean,
    ) : DocState
    data class Failed(val message: String) : DocState
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val settingsRepo = SettingsRepository(app)

    val settings: StateFlow<SettingsRepository.Settings> =
        settingsRepo.settings.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            SettingsRepository.Settings(
                themeMode = ThemeMode.AUTOMATIC,
                lightThemeId = "regular_light",
                darkThemeId = "regular_dark",
                fontSizeSp = SettingsRepository.DEFAULT_FONT_SIZE,
                wordWrap = false,
                fontId = FontIds.DEFAULT,
                customFontSet = CustomFontSet(),
            ),
        )

    private val _screen = MutableStateFlow(Screen.VIEWER)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _doc = MutableStateFlow<DocState>(DocState.Empty)
    val doc: StateFlow<DocState> = _doc.asStateFlow()

    private val _systemFonts = MutableStateFlow<List<SystemFont>>(emptyList())
    val systemFonts: StateFlow<List<SystemFont>> = _systemFonts.asStateFlow()

    private val _customFont = MutableStateFlow<CustomFont?>(null)
    val customFont: StateFlow<CustomFont?> = _customFont.asStateFlow()

    init {
        loadSystemFonts()
        reloadCustomFont()
    }

    fun openScreen(screen: Screen) { _screen.value = screen }

    fun loadUri(uri: Uri) {
        _doc.value = DocState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FileLoader.load(getApplication(), uri)
            }
            _doc.value = when (result) {
                is FileLoader.Result.Text -> {
                    val lines = if (result.content.isEmpty()) emptyList()
                    else result.content.split('\n').let { list ->
                        if (list.isNotEmpty() && list.last().isEmpty()) list.dropLast(1) else list
                    }
                    DocState.Loaded(result.displayName, lines, result.truncated)
                }
                is FileLoader.Result.Error -> DocState.Failed(result.message)
            }
        }
    }

    // ---- Fonts ----

    private fun loadSystemFonts() {
        viewModelScope.launch {
            _systemFonts.value = withContext(Dispatchers.IO) { SystemFonts.discover() }
        }
    }

    private fun reloadCustomFont() {
        viewModelScope.launch {
            _customFont.value = withContext(Dispatchers.IO) {
                SystemFonts.loadCustomFont(getApplication())
            }
        }
    }

    /** Copies a picked font file into a variant slot, then persists its name. */
    fun setCustomFontVariant(variant: FontVariant, uri: Uri) {
        viewModelScope.launch {
            val name = withContext(Dispatchers.IO) {
                SystemFonts.copyIntoSlot(getApplication(), uri, variant)
            }
            settingsRepo.setCustomFontVariantName(variant, name)
            reloadCustomFont()
        }
    }

    fun clearCustomFontVariant(variant: FontVariant) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { SystemFonts.clearSlot(getApplication(), variant) }
            settingsRepo.setCustomFontVariantName(variant, null)
            reloadCustomFont()
        }
    }

    fun selectCustomFont() = viewModelScope.launch { settingsRepo.setFontId(FontIds.CUSTOM) }

    /**
     * Resolves a stored font id to a Compose [FontFamily]. Falls back to the
     * built-in monospace face for the default sentinel, an unknown id, or a
     * custom selection with no loaded files.
     */
    fun fontFamilyFor(id: String?): FontFamily {
        if (id == null || id == FontIds.DEFAULT) return FontFamily.Monospace
        if (id == FontIds.CUSTOM) return _customFont.value?.fontFamily ?: FontFamily.Monospace
        return _systemFonts.value.firstOrNull { it.id == id }?.fontFamily ?: FontFamily.Monospace
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setLightTheme(id: String) = viewModelScope.launch { settingsRepo.setLightTheme(id) }
    fun setDarkTheme(id: String) = viewModelScope.launch { settingsRepo.setDarkTheme(id) }
    fun setFontSize(sp: Int) = viewModelScope.launch { settingsRepo.setFontSize(sp) }
    fun setWordWrap(wrap: Boolean) = viewModelScope.launch { settingsRepo.setWordWrap(wrap) }
    fun setFontId(id: String) = viewModelScope.launch { settingsRepo.setFontId(id) }
}
