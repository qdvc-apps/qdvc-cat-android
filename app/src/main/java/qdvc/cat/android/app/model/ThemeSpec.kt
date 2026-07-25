package qdvc.cat.android.app.model

/**
 * A colour theme, loaded from a JSON file in assets/themes/. It carries both
 * the handful of Material roles the chrome uses ([colors]) and a richer
 * [syntax] palette used by the highlighter, because a good multi-language
 * highlighter needs more distinct colours than the ~11 Material roles the
 * upstream qdvc-markdown-notebook app exposed.
 *
 * [dark] selects whether it appears in the light-mode or dark-mode list (and
 * which Material base scheme it builds on).
 *
 * The JSON shape is:
 * ```
 * {
 *   "id": "everforest_dark",
 *   "name": "Everforest Dark",
 *   "dark": true,
 *   "colors": { ... 11 Material roles ... },
 *   "syntax": {
 *     "keyword": "#E67E80",
 *     "string": "#A7C080",
 *     ...
 *   }
 * }
 * ```
 * Every colour is an "#RRGGBB" (or "#AARRGGBB") hex string. The `syntax`
 * object is optional; any missing token falls back to a sensible Material
 * role so older/minimal theme files still work.
 */
data class ThemeSpec(
    val id: String,
    val name: String,
    val dark: Boolean,
    val colors: ThemeColors,
    val syntax: SyntaxPalette,
)

data class ThemeColors(
    val background: String,
    val surface: String,
    val surfaceVariant: String,
    val onBackground: String,
    val onSurfaceVariant: String,
    val outline: String,
    val primary: String,
    val onPrimary: String,
    val secondary: String,
    val onSecondary: String,
    val error: String,
)

/**
 * Semantic syntax-highlighting colours. These are language-agnostic *roles*;
 * each language grammar maps its token classes onto them (see
 * [qdvc.cat.android.app.util.TokenType]). Keeping the palette semantic rather
 * than per-language means one small colour set drives every grammar.
 *
 * All fields are nullable: a null means "fall back to the derived default"
 * (see ThemeRepository), so a theme JSON may specify as few or as many as it
 * likes.
 */
data class SyntaxPalette(
    val keyword: String? = null,     // language keywords, YAML/JSON keys, MD headings
    val string: String? = null,      // string / quoted literals
    val number: String? = null,      // numeric & boolean/null literals
    val comment: String? = null,     // comments
    val function: String? = null,    // function / method names, CSS selectors
    val type: String? = null,        // types, classes, SQL identifiers, MD links
    val operator: String? = null,    // operators & punctuation of significance
    val variable: String? = null,    // variables, properties, CSV/TSV alt columns
    val constant: String? = null,    // constants, decorators, MD emphasis
    val punctuation: String? = null, // structural punctuation (braces, commas)
)
