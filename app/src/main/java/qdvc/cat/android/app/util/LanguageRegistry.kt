package qdvc.cat.android.app.util

import qdvc.cat.android.app.util.grammars.CssGrammar
import qdvc.cat.android.app.util.grammars.CsvGrammar
import qdvc.cat.android.app.util.grammars.JsonGrammar
import qdvc.cat.android.app.util.grammars.MarkdownGrammar
import qdvc.cat.android.app.util.grammars.PythonGrammar
import qdvc.cat.android.app.util.grammars.SqlGrammar
import qdvc.cat.android.app.util.grammars.TsvGrammar
import qdvc.cat.android.app.util.grammars.YamlGrammar

/**
 * The single registration point for syntax support. Adding a language is a
 * modular, self-contained change: write a [Grammar] in the `grammars` package
 * and add one line here mapping its file extension(s) to it. Nothing else in
 * the app needs to change.
 */
object LanguageRegistry {

    /** Lower-cased extension (without the dot) -> grammar. */
    private val byExtension: Map<String, Grammar> = buildMap {
        put("json", JsonGrammar)
        put("yaml", YamlGrammar)
        put("yml", YamlGrammar)
        put("csv", CsvGrammar)
        put("tsv", TsvGrammar)
        put("sql", SqlGrammar)
        put("py", PythonGrammar)
        put("css", CssGrammar)
        put("md", MarkdownGrammar)
        put("markdown", MarkdownGrammar)
        // .txt, .text, .log and unknown -> null (no highlighting).
    }

    /** Grammar for a filename, or null for plain text. */
    fun grammarForFileName(fileName: String?): Grammar? {
        val ext = fileName?.substringAfterLast('.', "")?.lowercase()
        if (ext.isNullOrEmpty()) return null
        return byExtension[ext]
    }

    /** A human-readable label for the status line ("JSON", "Plain text"). */
    fun languageLabel(grammar: Grammar?): String = when (grammar?.id) {
        "json" -> "JSON"
        "yaml" -> "YAML"
        "csv" -> "CSV"
        "tsv" -> "TSV"
        "sql" -> "SQL"
        "python" -> "Python"
        "css" -> "CSS"
        "markdown" -> "Markdown"
        else -> "Plain text"
    }
}
