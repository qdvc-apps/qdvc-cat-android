package qdvc.cat.android.app.data

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import org.json.JSONObject
import qdvc.cat.android.app.model.SyntaxPalette
import qdvc.cat.android.app.model.ThemeColors
import qdvc.cat.android.app.model.ThemeSpec
import qdvc.cat.android.app.util.SyntaxColors
import qdvc.cat.android.app.util.TokenType

/**
 * Loads colour themes from JSON files bundled in assets/themes/. Themes are
 * data, not code: adding a new .json file there makes a new theme available.
 *
 * Each theme carries the Material roles the chrome uses plus an optional
 * `syntax` block. Where a syntax colour is omitted, it's derived from the
 * Material roles so every theme yields a complete, coherent highlighting
 * palette without having to specify all ten scopes by hand.
 */
object ThemeRepository {

    const val DEFAULT_LIGHT_ID = "regular_light"
    const val DEFAULT_DARK_ID = "regular_dark"

    private const val THEMES_DIR = "themes"

    @Volatile
    private var cache: List<ThemeSpec>? = null

    fun all(context: Context): List<ThemeSpec> {
        cache?.let { return it }
        val loaded = load(context)
        cache = loaded
        return loaded
    }

    fun lightThemes(context: Context): List<ThemeSpec> = all(context).filter { !it.dark }
    fun darkThemes(context: Context): List<ThemeSpec> = all(context).filter { it.dark }

    fun byId(context: Context, id: String?): ThemeSpec? =
        if (id == null) null else all(context).firstOrNull { it.id == id }

    fun lightOrDefault(context: Context, id: String?): ThemeSpec? =
        byId(context, id)?.takeIf { !it.dark }
            ?: byId(context, DEFAULT_LIGHT_ID)
            ?: lightThemes(context).firstOrNull()

    fun darkOrDefault(context: Context, id: String?): ThemeSpec? =
        byId(context, id)?.takeIf { it.dark }
            ?: byId(context, DEFAULT_DARK_ID)
            ?: darkThemes(context).firstOrNull()

    private fun load(context: Context): List<ThemeSpec> {
        val assets = context.assets
        val files = try {
            assets.list(THEMES_DIR)?.filter { it.endsWith(".json") } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
        return files.mapNotNull { fileName ->
            try {
                val text = assets.open("$THEMES_DIR/$fileName").use {
                    it.readBytes().toString(Charsets.UTF_8)
                }
                parse(text)
            } catch (e: Exception) {
                null
            }
        }.sortedWith(compareBy({ it.dark }, { it.name.lowercase() }))
    }

    private fun parse(json: String): ThemeSpec? {
        return try {
            val root = JSONObject(json)
            val colors = root.getJSONObject("colors")
            val syntax = root.optJSONObject("syntax")
            ThemeSpec(
                id = root.getString("id"),
                name = root.getString("name"),
                dark = root.getBoolean("dark"),
                colors = ThemeColors(
                    background = colors.getString("background"),
                    surface = colors.getString("surface"),
                    surfaceVariant = colors.getString("surfaceVariant"),
                    onBackground = colors.getString("onBackground"),
                    onSurfaceVariant = colors.getString("onSurfaceVariant"),
                    outline = colors.getString("outline"),
                    primary = colors.getString("primary"),
                    onPrimary = colors.getString("onPrimary"),
                    secondary = colors.getString("secondary"),
                    onSecondary = colors.getString("onSecondary"),
                    error = colors.getString("error"),
                ),
                syntax = SyntaxPalette(
                    keyword = syntax?.optStringOrNull("keyword"),
                    string = syntax?.optStringOrNull("string"),
                    number = syntax?.optStringOrNull("number"),
                    comment = syntax?.optStringOrNull("comment"),
                    function = syntax?.optStringOrNull("function"),
                    type = syntax?.optStringOrNull("type"),
                    operator = syntax?.optStringOrNull("operator"),
                    variable = syntax?.optStringOrNull("variable"),
                    constant = syntax?.optStringOrNull("constant"),
                    punctuation = syntax?.optStringOrNull("punctuation"),
                ),
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun JSONObject.optStringOrNull(key: String): String? =
        if (has(key) && !isNull(key)) getString(key) else null

    /** Builds a Compose [ColorScheme] from a theme spec. */
    fun colorScheme(spec: ThemeSpec): ColorScheme {
        val c = spec.colors
        val base = if (spec.dark) darkColorScheme() else lightColorScheme()
        return base.copy(
            primary = hex(c.primary),
            onPrimary = hex(c.onPrimary),
            secondary = hex(c.secondary),
            onSecondary = hex(c.onSecondary),
            background = hex(c.background),
            onBackground = hex(c.onBackground),
            surface = hex(c.surface),
            onSurface = hex(c.onBackground),
            surfaceVariant = hex(c.surfaceVariant),
            onSurfaceVariant = hex(c.onSurfaceVariant),
            outline = hex(c.outline),
            error = hex(c.error),
        )
    }

    /**
     * Builds the highlighter's [SyntaxColors] for a theme. Explicit `syntax`
     * values win; anything omitted falls back to a Material role so the
     * palette is always complete and on-theme.
     */
    fun syntaxColors(spec: ThemeSpec): SyntaxColors {
        val c = spec.colors
        val s = spec.syntax
        fun pick(explicit: String?, fallback: String): Color = hex(explicit ?: fallback)
        val map = mapOf(
            TokenType.PLAIN to hex(c.onBackground),
            TokenType.KEYWORD to pick(s.keyword, c.primary),
            TokenType.STRING to pick(s.string, c.secondary),
            TokenType.NUMBER to pick(s.number, c.secondary),
            TokenType.COMMENT to pick(s.comment, c.onSurfaceVariant),
            TokenType.FUNCTION to pick(s.function, c.primary),
            TokenType.TYPE to pick(s.type, c.secondary),
            TokenType.OPERATOR to pick(s.operator, c.onSurfaceVariant),
            TokenType.VARIABLE to pick(s.variable, c.onBackground),
            TokenType.CONSTANT to pick(s.constant, c.error),
            TokenType.PUNCTUATION to pick(s.punctuation, c.onSurfaceVariant),
        )
        return SyntaxColors(map)
    }

    /** Parses "#RRGGBB" or "#AARRGGBB" into a Compose [Color]. */
    private fun hex(value: String): Color {
        val clean = value.removePrefix("#")
        val argb = when (clean.length) {
            6 -> 0xFF000000.toInt() or clean.toInt(16)
            8 -> clean.toLong(16).toInt()
            else -> 0xFF000000.toInt()
        }
        return Color(argb)
    }
}
