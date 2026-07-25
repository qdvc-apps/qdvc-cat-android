package qdvc.cat.android.app.util

/**
 * Bridges a document, a [Grammar], and the on-screen renderer.
 *
 * Highlighting a large file eagerly would be wasteful, but tokenising a line
 * needs the grammar state carried over from all the lines above it. The
 * compromise: on construction we do ONE cheap forward pass computing the
 * entry-state for each line (just a tiny state stack per line — no tokens
 * retained). Then [tokensForLine] can tokenise any single line on demand in
 * O(line length), which is exactly what a lazily-scrolled viewer needs.
 *
 * If there's no grammar (plain text), everything is a single PLAIN token.
 */
class Highlighter(
    val lines: List<String>,
    private val grammar: Grammar?,
) {
    private val entryStates: Array<Tokenizer.State>? =
        if (grammar == null) null else computeEntryStates(grammar, lines)

    fun tokensForLine(index: Int): List<Token> {
        if (index !in lines.indices) return emptyList()
        val line = lines[index]
        val g = grammar ?: return if (line.isEmpty()) emptyList()
        else listOf(Token(0, line.length, TokenType.PLAIN))
        val entry = entryStates!![index]
        return Tokenizer.tokenizeLine(g, line, entry).tokens
    }

    companion object {
        /** Cap on lines we pre-scan for state; beyond this, later lines start fresh. */
        private const val MAX_STATE_LINES = 200_000

        private fun computeEntryStates(grammar: Grammar, lines: List<String>): Array<Tokenizer.State> {
            val initial = Tokenizer.State.initial(grammar)
            val states = Array(lines.size) { initial }
            var state = initial
            val limit = minOf(lines.size, MAX_STATE_LINES)
            for (i in 0 until limit) {
                states[i] = state
                state = Tokenizer.tokenizeLine(grammar, lines[i], state).endState
            }
            // Lines beyond the cap keep `initial` — a safe, if imperfect, fallback.
            return states
        }
    }
}
