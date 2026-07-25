package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/**
 * CSV and TSV. There's no real "syntax" to a delimited file, so highlighting
 * aims purely at scannability: fields alternate between two scopes column by
 * column, quoted fields are shown as strings, delimiters as punctuation, and
 * numeric fields as numbers. Column alternation is done with two mirror states
 * that flip on every delimiter.
 */
private fun delimitedGrammar(id: String, delim: String): Grammar {
    // The delimiter character(s), regex-escaped for use in a character run.
    val quoted = "\"(?:\"\"|[^\"])*\""
    val field = "[^$delim\\n]+"
    val number = "-?\\d+(?:\\.\\d+)?"

    fun fieldRules(evenColumn: Boolean): List<Rule> {
        val fieldScope = if (evenColumn) TokenType.VARIABLE else TokenType.TYPE
        val other = if (evenColumn) "odd" else "even"
        return listOf(
            Rule(rx(delim), token = TokenType.PUNCTUATION, pop = true, push = other),
            Rule(rx(quoted), token = TokenType.STRING),
            Rule(rx("^$number$", RegexOption.MULTILINE)),
            Rule(rx(number + "(?=$delim|\$)"), token = TokenType.NUMBER),
            Rule(rx(field), token = fieldScope),
        )
    }

    return Grammar(
        id = id,
        startState = "even",
        states = mapOf(
            "even" to fieldRules(true),
            "odd" to fieldRules(false),
        ),
    )
}

val CsvGrammar = delimitedGrammar("csv", ",")
val TsvGrammar = delimitedGrammar("tsv", "\\t")
