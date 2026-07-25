package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/**
 * YAML. Handles comments, document markers, keys, anchors/aliases, tags,
 * block-list dashes, quoted and bare scalars, and common literal constants.
 */
val YamlGrammar = Grammar(
    id = "yaml",
    states = mapOf(
        "root" to listOf(
            Rule(rx("#.*$"), token = TokenType.COMMENT),
            Rule(rx("^(?:---|\\.\\.\\.)\\s*$"), token = TokenType.PUNCTUATION),
            Rule(rx("\\s+")),
            // "- " list markers.
            Rule(rx("-(?=\\s)"), token = TokenType.PUNCTUATION),
            // key:  (bare or quoted) followed by whitespace/EOL.
            Rule(
                rx("(\"(?:\\\\.|[^\"\\\\])*\"|'(?:''|[^'])*'|[\\w.\\-/]+)(\\s*)(:)(?=\\s|$)"),
                groupTokens = mapOf(1 to TokenType.KEYWORD, 3 to TokenType.PUNCTUATION),
            ),
            // Anchors, aliases, tags, merge keys.
            Rule(rx("[&*][\\w\\-]+"), token = TokenType.VARIABLE),
            Rule(rx("!!?[\\w\\-/]+"), token = TokenType.TYPE),
            Rule(rx("\"(?:\\\\.|[^\"\\\\])*\""), token = TokenType.STRING),
            Rule(rx("'(?:''|[^'])*'"), token = TokenType.STRING),
            Rule(rx("\\b(?:true|false|null|yes|no|on|off|~|Null|True|False)\\b"), token = TokenType.CONSTANT),
            Rule(rx("[-+]?\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?\\b"), token = TokenType.NUMBER),
            Rule(rx("[\\[\\]{},]"), token = TokenType.PUNCTUATION),
            Rule(rx("[|>][+-]?"), token = TokenType.OPERATOR),
        ),
    ),
)
