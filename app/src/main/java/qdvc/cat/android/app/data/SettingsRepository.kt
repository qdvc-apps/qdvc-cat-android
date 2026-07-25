package qdvc.cat.android.app.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import qdvc.cat.android.app.model.CustomFontSet
import qdvc.cat.android.app.model.FontIds
import qdvc.cat.android.app.model.FontVariant
import qdvc.cat.android.app.model.ThemeMode

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * Persists user settings: theme mode (defaults to AUTOMATIC = follow OS),
 * chosen light/dark theme ids, monospace text size, word wrap, and the font
 * selection (a font id plus the custom-font-set slot names).
 */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LIGHT_THEME = stringPreferencesKey("light_theme_id")
        val DARK_THEME = stringPreferencesKey("dark_theme_id")
        val FONT_SIZE = intPreferencesKey("font_size_sp")
        val WORD_WRAP = booleanPreferencesKey("word_wrap")

        // Font selection: id (default sentinel, a device-font path, or the
        // custom sentinel) plus one display-name per custom-font variant slot.
        val FONT_ID = stringPreferencesKey("font_id")
        val FONT_REGULAR = stringPreferencesKey("font_regular_name")
        val FONT_ITALIC = stringPreferencesKey("font_italic_name")
        val FONT_BOLD = stringPreferencesKey("font_bold_name")
        val FONT_BOLD_ITALIC = stringPreferencesKey("font_bold_italic_name")
    }

    data class Settings(
        val themeMode: ThemeMode,
        val lightThemeId: String,
        val darkThemeId: String,
        val fontSizeSp: Int,
        val wordWrap: Boolean,
        val fontId: String,
        val customFontSet: CustomFontSet,
    )

    val settings: Flow<Settings> = context.dataStore.data.map { it.toSettings() }

    private fun Preferences.toSettings() = Settings(
        themeMode = runCatching { ThemeMode.valueOf(this[Keys.THEME_MODE] ?: "") }
            .getOrDefault(ThemeMode.AUTOMATIC),
        lightThemeId = this[Keys.LIGHT_THEME] ?: ThemeRepository.DEFAULT_LIGHT_ID,
        darkThemeId = this[Keys.DARK_THEME] ?: ThemeRepository.DEFAULT_DARK_ID,
        fontSizeSp = (this[Keys.FONT_SIZE] ?: DEFAULT_FONT_SIZE).coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE),
        wordWrap = this[Keys.WORD_WRAP] ?: false,
        fontId = this[Keys.FONT_ID] ?: FontIds.DEFAULT,
        customFontSet = CustomFontSet(
            regularName = this[Keys.FONT_REGULAR],
            italicName = this[Keys.FONT_ITALIC],
            boldName = this[Keys.FONT_BOLD],
            boldItalicName = this[Keys.FONT_BOLD_ITALIC],
        ),
    )

    suspend fun setThemeMode(mode: ThemeMode) =
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }

    suspend fun setLightTheme(id: String) =
        context.dataStore.edit { it[Keys.LIGHT_THEME] = id }

    suspend fun setDarkTheme(id: String) =
        context.dataStore.edit { it[Keys.DARK_THEME] = id }

    suspend fun setFontSize(sp: Int) =
        context.dataStore.edit { it[Keys.FONT_SIZE] = sp.coerceIn(MIN_FONT_SIZE, MAX_FONT_SIZE) }

    suspend fun setWordWrap(wrap: Boolean) =
        context.dataStore.edit { it[Keys.WORD_WRAP] = wrap }

    suspend fun setFontId(id: String) =
        context.dataStore.edit { it[Keys.FONT_ID] = id }

    /** Records or clears the stored display name for one custom-font slot. */
    suspend fun setCustomFontVariantName(variant: FontVariant, name: String?) {
        context.dataStore.edit { prefs ->
            val key = when (variant) {
                FontVariant.REGULAR -> Keys.FONT_REGULAR
                FontVariant.ITALIC -> Keys.FONT_ITALIC
                FontVariant.BOLD -> Keys.FONT_BOLD
                FontVariant.BOLD_ITALIC -> Keys.FONT_BOLD_ITALIC
            }
            if (name == null) prefs.remove(key) else prefs[key] = name
        }
    }

    companion object {
        const val DEFAULT_FONT_SIZE = 14
        const val MIN_FONT_SIZE = 8
        const val MAX_FONT_SIZE = 28
    }
}
