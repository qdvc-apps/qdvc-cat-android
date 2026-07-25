package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/** SQL (ANSI-ish, case-insensitive keywords). Handles /* */ block comments. */
val SqlGrammar = Grammar(
    id = "sql",
    states = mapOf(
        "root" to listOf(
            Rule(rx("\\s+")),
            Rule(rx("--.*$"), token = TokenType.COMMENT),
            Rule(rx("/\\*"), token = TokenType.COMMENT, push = "block_comment"),
            Rule(rx("'(?:''|[^'])*'"), token = TokenType.STRING),
            Rule(rx("\"(?:\"\"|[^\"])*\""), token = TokenType.STRING),
            Rule(rx("`(?:[^`])*`"), token = TokenType.STRING),
            Rule(rx("\\b\\d+(?:\\.\\d+)?\\b"), token = TokenType.NUMBER),
            Rule(
                rx(
                    "(?i)\\b(?:select|from|where|insert|into|values|update|set|delete|" +
                        "create|alter|drop|truncate|table|view|index|trigger|procedure|" +
                        "function|database|schema|join|inner|left|right|full|outer|cross|" +
                        "on|using|group|by|order|having|limit|offset|union|all|distinct|" +
                        "as|and|or|not|null|is|in|between|like|exists|case|when|then|else|" +
                        "end|primary|key|foreign|references|constraint|unique|default|check|" +
                        "cascade|begin|commit|rollback|transaction|grant|revoke|with|" +
                        "returning|desc|asc|add|column|if|and|between)\\b",
                ),
                token = TokenType.KEYWORD,
            ),
            Rule(
                rx(
                    "(?i)\\b(?:int|integer|bigint|smallint|tinyint|decimal|numeric|float|" +
                        "real|double|char|varchar|nvarchar|text|clob|blob|date|time|" +
                        "timestamp|datetime|boolean|bool|serial|uuid|json|jsonb|bytea)\\b",
                ),
                token = TokenType.TYPE,
            ),
            Rule(rx("(?i)\\b(?:true|false|null)\\b"), token = TokenType.CONSTANT),
            // function calls: name(
            Rule(rx("[A-Za-z_][\\w]*(?=\\s*\\()"), token = TokenType.FUNCTION),
            Rule(rx("[=<>!]+|[-+*/%|]"), token = TokenType.OPERATOR),
            Rule(rx("[(),;.]"), token = TokenType.PUNCTUATION),
            Rule(rx("[A-Za-z_][\\w]*"), token = TokenType.VARIABLE),
        ),
        "block_comment" to listOf(
            Rule(rx("\\*/"), token = TokenType.COMMENT, pop = true),
            Rule(rx("[^*]+"), token = TokenType.COMMENT),
            Rule(rx("\\*"), token = TokenType.COMMENT),
        ),
    ),
)
