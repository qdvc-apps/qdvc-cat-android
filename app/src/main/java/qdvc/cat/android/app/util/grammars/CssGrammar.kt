package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/**
 * CSS. Two states: selector context (outside braces) and declaration context
 * (inside braces), so property names, values, colours, units and !important
 * are coloured distinctly from selectors.
 */
val CssGrammar = Grammar(
    id = "css",
    states = mapOf(
        "root" to listOf(
            Rule(rx("\\s+")),
            Rule(rx("/\\*"), token = TokenType.COMMENT, push = "comment"),
            Rule(rx("@[A-Za-z-]+"), token = TokenType.KEYWORD), // at-rules
            Rule(rx("\\{"), token = TokenType.PUNCTUATION, push = "block"),
            // Selectors: element, class, id, pseudo, attribute.
            Rule(rx("[.#]?[A-Za-z_][\\w-]*"), token = TokenType.FUNCTION),
            Rule(rx("::?[A-Za-z-]+"), token = TokenType.TYPE), // pseudo
            Rule(rx("\\[[^\\]]*\\]"), token = TokenType.VARIABLE), // attr selector
            Rule(rx("[*>+~,]"), token = TokenType.OPERATOR),
        ),
        "block" to listOf(
            Rule(rx("\\s+")),
            Rule(rx("/\\*"), token = TokenType.COMMENT, push = "comment"),
            Rule(rx("\\}"), token = TokenType.PUNCTUATION, pop = true),
            // property:
            Rule(
                rx("([A-Za-z-]+)(\\s*)(:)"),
                groupTokens = mapOf(1 to TokenType.KEYWORD, 3 to TokenType.PUNCTUATION),
            ),
            Rule(rx("#[0-9A-Fa-f]{3,8}\\b"), token = TokenType.CONSTANT), // hex colour
            Rule(rx("\"(?:\\\\.|[^\"\\\\])*\""), token = TokenType.STRING),
            Rule(rx("'(?:\\\\.|[^'\\\\])*'"), token = TokenType.STRING),
            Rule(rx("-?\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|vmin|vmax|pt|cm|mm|ex|ch|fr|deg|s|ms)?\\b"), token = TokenType.NUMBER),
            Rule(rx("![A-Za-z]+"), token = TokenType.CONSTANT), // !important
            Rule(rx("[A-Za-z_][\\w-]*(?=\\s*\\()"), token = TokenType.FUNCTION), // fn like rgb(
            Rule(rx("[A-Za-z_][\\w-]*"), token = TokenType.VARIABLE),
            Rule(rx("[;:,()]"), token = TokenType.PUNCTUATION),
        ),
        "comment" to listOf(
            Rule(rx("\\*/"), token = TokenType.COMMENT, pop = true),
            Rule(rx("[^*]+"), token = TokenType.COMMENT),
            Rule(rx("\\*"), token = TokenType.COMMENT),
        ),
    ),
)
