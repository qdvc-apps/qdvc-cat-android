package qdvc.cat.android.app.util

import androidx.compose.ui.graphics.Color

/**
 * The active mapping from semantic [TokenType]s to concrete colours, built by
 * ThemeRepository from the current theme. Immutable and cheap to pass around.
 */
class SyntaxColors(private val map: Map<TokenType, Color>) {
    fun colorFor(type: TokenType): Color = map[type] ?: map[TokenType.PLAIN] ?: Color.Unspecified
}
