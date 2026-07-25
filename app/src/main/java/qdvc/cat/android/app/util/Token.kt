package qdvc.cat.android.app.util

/**
 * Semantic token scopes, deliberately language-agnostic. Every grammar maps
 * its own concrete constructs (a Python `def`, a JSON key, a CSS selector)
 * onto this small shared vocabulary, and each theme maps this vocabulary onto
 * concrete colours. This is the same "semantic scope" idea used by TextMate /
 * VS Code, kept small so it's practical on-device.
 */
enum class TokenType {
    PLAIN,
    KEYWORD,
    STRING,
    NUMBER,
    COMMENT,
    FUNCTION,
    TYPE,
    OPERATOR,
    VARIABLE,
    CONSTANT,
    PUNCTUATION,
}

/**
 * A coloured run of text: [start] inclusive, [end] exclusive, within the line
 * (or whole document) that produced it.
 */
data class Token(
    val start: Int,
    val end: Int,
    val type: TokenType,
)
