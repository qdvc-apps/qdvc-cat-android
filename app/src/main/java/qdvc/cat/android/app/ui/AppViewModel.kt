package qdvc.cat.android.app.ui

import android.app.Application
import android.net.Uri
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
import qdvc.cat.android.app.model.ThemeMode
import qdvc.cat.android.app.util.FileLoader

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
            ),
        )

    private val _screen = MutableStateFlow(Screen.VIEWER)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _doc = MutableStateFlow<DocState>(DocState.Empty)
    val doc: StateFlow<DocState> = _doc.asStateFlow()

    fun openScreen(screen: Screen) { _screen.value = screen }

    fun loadUri(uri: Uri) {
        _doc.value = DocState.Loading
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                FileLoader.load(getApplication(), uri)
            }
            _doc.value = when (result) {
                is FileLoader.Result.Text -> {
                    // Split once, keeping empty trailing lines out.
                    val lines = if (result.content.isEmpty()) emptyList()
                    else result.content.split('\n').let { list ->
                        // Drop a single trailing empty line from a final newline.
                        if (list.isNotEmpty() && list.last().isEmpty()) list.dropLast(1) else list
                    }
                    DocState.Loaded(result.displayName, lines, result.truncated)
                }
                is FileLoader.Result.Error -> DocState.Failed(result.message)
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setLightTheme(id: String) = viewModelScope.launch { settingsRepo.setLightTheme(id) }
    fun setDarkTheme(id: String) = viewModelScope.launch { settingsRepo.setDarkTheme(id) }
    fun setFontSize(sp: Int) = viewModelScope.launch { settingsRepo.setFontSize(sp) }
    fun setWordWrap(wrap: Boolean) = viewModelScope.launch { settingsRepo.setWordWrap(wrap) }
}
