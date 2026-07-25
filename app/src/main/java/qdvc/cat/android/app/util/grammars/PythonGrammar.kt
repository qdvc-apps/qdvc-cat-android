package qdvc.cat.android.app.util.grammars

import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Rule
import qdvc.cat.android.app.util.TokenType
import qdvc.cat.android.app.util.rx

/**
 * Python. Handles comments, decorators, triple-quoted strings (multi-line via
 * dedicated states), single/double strings with optional prefixes, numbers,
 * keywords, builtins/constants, def/class names, and function calls.
 */
val PythonGrammar = Grammar(
    id = "python",
    states = mapOf(
        "root" to listOf(
            Rule(rx("\\s+")),
            Rule(rx("#.*$"), token = TokenType.COMMENT),
            Rule(rx("@[A-Za-z_][\\w.]*"), token = TokenType.CONSTANT), // decorator
            // Triple-quoted strings.
            Rule(rx("[rRbBfFuU]{0,2}\"\"\""), token = TokenType.STRING, push = "tdq"),
            Rule(rx("[rRbBfFuU]{0,2}'''"), token = TokenType.STRING, push = "tsq"),
            // Single-line strings.
            Rule(rx("[rRbBfFuU]{0,2}\"(?:\\\\.|[^\"\\\\])*\""), token = TokenType.STRING),
            Rule(rx("[rRbBfFuU]{0,2}'(?:\\\\.|[^'\\\\])*'"), token = TokenType.STRING),
            Rule(rx("\\b\\d+(?:\\.\\d+)?(?:[eE][+-]?\\d+)?[jJ]?\\b"), token = TokenType.NUMBER),
            Rule(rx("\\b0[xX][0-9a-fA-F]+\\b"), token = TokenType.NUMBER),
            // def / class introduce a highlighted name.
            Rule(
                rx("\\b(def|class)(\\s+)([A-Za-z_]\\w*)"),
                groupTokens = mapOf(1 to TokenType.KEYWORD, 3 to TokenType.FUNCTION),
            ),
            Rule(
                rx(
                    "\\b(?:and|as|assert|async|await|break|continue|del|elif|else|except|" +
                        "finally|for|from|global|if|import|in|is|lambda|nonlocal|not|or|" +
                        "pass|raise|return|try|while|with|yield|match|case)\\b",
                ),
                token = TokenType.KEYWORD,
            ),
            Rule(rx("\\b(?:True|False|None|self|cls|__name__|NotImplemented|Ellipsis)\\b"), token = TokenType.CONSTANT),
            Rule(
                rx(
                    "\\b(?:print|len|range|int|str|float|bool|list|dict|set|tuple|" +
                        "open|type|isinstance|super|enumerate|zip|map|filter|sorted|" +
                        "sum|min|max|abs|round|input|format|repr)\\b",
                ),
                token = TokenType.TYPE,
            ),
            Rule(rx("[A-Za-z_]\\w*(?=\\s*\\()"), token = TokenType.FUNCTION),
            Rule(rx("[+\\-*/%=<>!&|^~@]+|:="), token = TokenType.OPERATOR),
            Rule(rx("[()\\[\\]{}:;,.]"), token = TokenType.PUNCTUATION),
            Rule(rx("[A-Za-z_]\\w*"), token = TokenType.VARIABLE),
        ),
        "tdq" to listOf(
            Rule(rx("\"\"\""), token = TokenType.STRING, pop = true),
            Rule(rx("[^\"]+"), token = TokenType.STRING),
            Rule(rx("\""), token = TokenType.STRING),
        ),
        "tsq" to listOf(
            Rule(rx("'''"), token = TokenType.STRING, pop = true),
            Rule(rx("[^']+"), token = TokenType.STRING),
            Rule(rx("'"), token = TokenType.STRING),
        ),
    ),
)
