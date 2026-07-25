package qdvc.cat.android.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import qdvc.cat.android.app.data.ThemeRepository
import qdvc.cat.android.app.model.ThemeMode
import qdvc.cat.android.app.model.ThemeSpec
import qdvc.cat.android.app.util.SyntaxColors
import qdvc.cat.android.app.util.TokenType

private val AppTypography = Typography()

/** Provides the active syntax palette to the file view. */
val LocalSyntaxColors = staticCompositionLocalOf {
    SyntaxColors(mapOf(TokenType.PLAIN to androidx.compose.ui.graphics.Color.Unspecified))
}

/**
 * Applies the app theme. Colours come from a [ThemeSpec] loaded from JSON
 * (see assets/themes/): [lightTheme] in light mode, [darkTheme] in dark mode.
 * The matching [SyntaxColors] are published via [LocalSyntaxColors]. Nulls
 * fall back to Material defaults so the app still renders if theme assets are
 * missing.
 */
@Composable
fun QdvcCatTheme(
    themeMode: ThemeMode,
    lightTheme: ThemeSpec?,
    darkTheme: ThemeSpec?,
    content: @Composable () -> Unit,
) {
    val useDark = when (themeMode) {
        ThemeMode.AUTOMATIC -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val spec = if (useDark) darkTheme else lightTheme
    val colors = when {
        spec != null -> ThemeRepository.colorScheme(spec)
        useDark -> darkColorScheme()
        else -> lightColorScheme()
    }
    val syntax = if (spec != null) {
        ThemeRepository.syntaxColors(spec)
    } else {
        SyntaxColors(mapOf(TokenType.PLAIN to colors.onBackground))
    }

    CompositionLocalProvider(LocalSyntaxColors provides syntax) {
        MaterialTheme(
            colorScheme = colors,
            typography = AppTypography,
            content = content,
        )
    }
}
