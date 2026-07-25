package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/**
 * Markdown, including optional YAML frontmatter delimited by `---` on the very
 * first line. The engine starts in [frontmatterProbe]: if the first line is
 * exactly `---`, it enters a frontmatter state that reuses YAML-ish rules
 * until the closing `---`; otherwise it drops straight into normal markdown.
 *
 * Because the probe only fires meaningfully on line 1 (the closing `---`
 * transitions out and there's no way back in), frontmatter is recognised only
 * at the top of the document, as intended.
 */
val MarkdownGrammar = Grammar(
    id = "markdown",
    startState = "frontmatterProbe",
    states = mapOf(
        "frontmatterProbe" to listOf(
            // First line is exactly '---' -> enter frontmatter.
            Rule(rx("^---\\s*$"), token = TokenType.PUNCTUATION, pop = true, push = "frontmatter"),
            // Anything else -> this isn't frontmatter; behave as markdown.
            // We pop the probe and re-process as markdown by pushing "md".
            Rule(rx("(?=.)"), pop = true, push = "md"),
        ),
        "frontmatter" to listOf(
            Rule(rx("^(?:---|\\.\\.\\.)\\s*$"), token = TokenType.PUNCTUATION, pop = true, push = "md"),
            Rule(rx("#.*$"), token = TokenType.COMMENT),
            Rule(rx("\\s+")),
            Rule(
                rx("([\\w.\\-/]+)(\\s*)(:)(?=\\s|$)"),
                groupTokens = mapOf(1 to TokenType.KEYWORD, 3 to TokenType.PUNCTUATION),
            ),
            Rule(rx("-(?=\\s)"), token = TokenType.PUNCTUATION),
            Rule(rx("\"(?:\\\\.|[^\"\\\\])*\""), token = TokenType.STRING),
            Rule(rx("'(?:''|[^'])*'"), token = TokenType.STRING),
            Rule(rx("\\b(?:true|false|null|yes|no)\\b"), token = TokenType.CONSTANT),
            Rule(rx("[-+]?\\d+(?:\\.\\d+)?\\b"), token = TokenType.NUMBER),
        ),
        "md" to listOf(
            // ATX headings.
            Rule(rx("^#{1,6}\\s.*$"), token = TokenType.KEYWORD),
            // Fenced code blocks.
            Rule(rx("^\\s*(?:```|~~~).*$"), token = TokenType.STRING, push = "codefence"),
            // Blockquote.
            Rule(rx("^\\s*>.*$"), token = TokenType.COMMENT),
            // List markers at line start.
            Rule(rx("^\\s*(?:[-*+]|\\d+\\.)\\s"), token = TokenType.OPERATOR),
            // Horizontal rule.
            Rule(rx("^\\s*(?:---|\\*\\*\\*|___)\\s*$"), token = TokenType.PUNCTUATION),
            // Inline code.
            Rule(rx("`[^`]+`"), token = TokenType.STRING),
            // Bold / italic.
            Rule(rx("\\*\\*[^*]+\\*\\*"), token = TokenType.CONSTANT),
            Rule(rx("__[^_]+__"), token = TokenType.CONSTANT),
            Rule(rx("\\*[^*\\n]+\\*"), token = TokenType.CONSTANT),
            Rule(rx("_[^_\\n]+_"), token = TokenType.CONSTANT),
            // Links / images: [text](url)
            Rule(
                rx("(!?\\[)([^\\]]*)(\\]\\()([^)]*)(\\))"),
                groupTokens = mapOf(2 to TokenType.FUNCTION, 4 to TokenType.TYPE),
            ),
            // Bare autolink.
            Rule(rx("<https?://[^>]+>"), token = TokenType.TYPE),
            Rule(rx("\\s+")),
            Rule(rx("[^`*_\\[<\\s]+")), // plain word run
        ),
        "codefence" to listOf(
            Rule(rx("^\\s*(?:```|~~~).*$"), token = TokenType.STRING, pop = true),
            Rule(rx(".*$"), token = TokenType.PLAIN),
        ),
    ),
)
