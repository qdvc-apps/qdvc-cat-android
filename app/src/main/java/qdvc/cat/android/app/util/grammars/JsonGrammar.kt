package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/** JSON. Object keys are highlighted distinctly from string values. */
val JsonGrammar = Grammar(
    id = "json",
    states = mapOf(
        "root" to listOf(
            Rule(rx("\\s+")),
            // A quoted string immediately followed by a colon is a key.
            Rule(
                rx("(\"(?:\\\\.|[^\"\\\\])*\")(\\s*)(:)"),
                groupTokens = mapOf(1 to TokenType.KEYWORD, 3 to TokenType.PUNCTUATION),
            ),
            Rule(rx("\"(?:\\\\.|[^\"\\\\])*\""), token = TokenType.STRING),
            Rule(rx("-?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?"), token = TokenType.NUMBER),
            Rule(rx("\\b(?:true|false|null)\\b"), token = TokenType.CONSTANT),
            Rule(rx("[\\[\\]{},]"), token = TokenType.PUNCTUATION),
            Rule(rx(":"), token = TokenType.PUNCTUATION),
        ),
    ),
)
