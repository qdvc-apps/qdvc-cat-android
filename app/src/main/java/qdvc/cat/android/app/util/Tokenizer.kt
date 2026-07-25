package qdvc.cat.android.app.util

/**
 * Runs a [Grammar] over text. Designed for lazy, per-line use so the viewer
 * only ever tokenises the lines currently on screen: [tokenizeLine] takes the
 * grammar-state stack left by the previous line and returns this line's tokens
 * plus the stack to feed into the next line.
 *
 * The state stack is what makes multi-line constructs (block comments, YAML
 * frontmatter, triple-quoted strings) correct without ever holding the whole
 * document tokenisation in memory.
 */
object Tokenizer {

    /** The grammar-state stack at a line boundary. Immutable; cheap to copy. */
    @JvmInline
    value class State(val stack: List<String>) {
        companion object {
            fun initial(grammar: Grammar) = State(listOf(grammar.startState))
        }
    }

    data class LineResult(val tokens: List<Token>, val endState: State)

    /**
     * Tokenise a single [line] (no trailing newline) starting from [state].
     * Guaranteed to terminate: if no rule matches at the cursor, one character
     * is emitted as PLAIN and the cursor advances.
     */
    fun tokenizeLine(grammar: Grammar, line: String, state: State): LineResult {
        if (line.isEmpty()) return LineResult(emptyList(), state)

        val tokens = ArrayList<Token>(16)
        var stack = state.stack.toMutableList()
        var pos = 0
        val len = line.length

        // Hard cap on rule attempts per line to stay safe on pathological input.
        var guard = 0
        val guardLimit = len * 32 + 64

        while (pos < len) {
            if (guard++ > guardLimit) {
                // Give up gracefully: colour the remainder plain.
                tokens.add(Token(pos, len, TokenType.PLAIN))
                break
            }

            val currentState = stack.lastOrNull() ?: grammar.startState
            val rules = grammar.states[currentState] ?: emptyList()

            var matched = false
            for (rule in rules) {
                val m = rule.pattern.matchAt(line, pos) ?: continue
                val matchEnd = m.range.last + 1
                if (matchEnd <= pos) continue // zero-width guard

                emit(tokens, m, rule)

                // Apply state transitions.
                if (rule.pop && stack.size > 1) stack.removeAt(stack.size - 1)
                if (rule.push != null) stack.add(rule.push)

                pos = matchEnd
                matched = true
                break
            }

            if (!matched) {
                // No rule applied: emit one plain char and advance.
                tokens.add(Token(pos, pos + 1, TokenType.PLAIN))
                pos += 1
            }
        }

        return LineResult(mergePlain(tokens), State(stack))
    }

    private fun emit(out: MutableList<Token>, m: MatchResult, rule: Rule) {
        val gt = rule.groupTokens
        if (gt != null) {
            // Colour specific capture groups; leave the rest plain.
            for ((groupIndex, type) in gt.entries.sortedBy { it.key }) {
                val g = m.groups[groupIndex] ?: continue
                if (g.value.isEmpty()) continue
                out.add(Token(g.range.first, g.range.last + 1, type))
            }
        } else if (rule.token != null) {
            out.add(Token(m.range.first, m.range.last + 1, rule.token))
        }
        // token == null && groupTokens == null -> consumed silently
    }

    /**
     * Merge adjacent tokens of the same type and fill gaps with PLAIN so the
     * renderer receives a clean, contiguous, sorted span list.
     */
    private fun mergePlain(tokens: List<Token>): List<Token> {
        if (tokens.isEmpty()) return tokens
        val sorted = tokens.sortedBy { it.start }
        val merged = ArrayList<Token>(sorted.size)
        for (t in sorted) {
            val last = merged.lastOrNull()
            if (last != null && last.type == t.type && last.end == t.start) {
                merged[merged.size - 1] = last.copy(end = t.end)
            } else if (last != null && t.start < last.end) {
                // Overlap (shouldn't normally happen) — skip the overlapping part.
                if (t.end > last.end) merged.add(t.copy(start = last.end))
            } else {
                merged.add(t)
            }
        }
        return merged
    }
}
