package qdvc.cat.android.app.util

/**
 * A tiny declarative, regex-rule-based grammar engine in the spirit of
 * Monarch (Monaco), Prism, and CodeMirror's "simple mode". A language is
 * described purely as data: a set of named [states], each an ordered list of
 * [Rule]s. This is the "industry-standard-ish" format the brief asked for
 * without the overkill of a full TextMate/oniguruma engine.
 *
 * Tokenising walks each line left to right. Within the current state, rules
 * are tried in order; the first whose regex matches at the cursor wins,
 * emitting a token of its [Rule.token] and optionally switching state via
 * [Rule.push] / [Rule.pop]. State carries across line boundaries, so
 * constructs like block comments or YAML frontmatter that span multiple lines
 * work correctly.
 *
 * Grammars are defined once as immutable objects in the `grammars` package and
 * registered in [LanguageRegistry], so adding a language is a self-contained,
 * modular change: drop in one file, register it.
 */
data class Grammar(
    val id: String,
    val states: Map<String, List<Rule>>,
    val startState: String = "root",
)

/**
 * One tokenising rule.
 *
 * @param pattern regex anchored implicitly at the cursor (the engine only
 *   accepts matches that begin exactly at the current position).
 * @param token the semantic scope to colour the matched text with. If null,
 *   the match is consumed without emitting a coloured token (useful for
 *   whitespace or pure state transitions).
 * @param push if set, push this state name onto the stack after matching.
 * @param pop if true, pop one state off the stack after matching.
 * @param groupTokens optional per-capture-group scopes. When provided, instead
 *   of colouring the whole match as [token], each numbered capture group is
 *   coloured with its mapped scope (group 0 = whole match). Text outside any
 *   listed group is left plain. This lets a single rule colour, e.g., a YAML
 *   `key:` as KEYWORD while leaving the value plain.
 */
data class Rule(
    val pattern: Regex,
    val token: TokenType? = null,
    val push: String? = null,
    val pop: Boolean = false,
    val groupTokens: Map<Int, TokenType>? = null,
)

/** Convenience for building an anchored regex from a pattern string. */
fun rx(pattern: String, vararg options: RegexOption): Regex =
    Regex(pattern, options.toSet())
