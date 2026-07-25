package qdvc.cat.android.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import qdvc.cat.android.app.ui.DocState
import qdvc.cat.android.app.ui.theme.LocalSyntaxColors
import qdvc.cat.android.app.util.Grammar
import qdvc.cat.android.app.util.Highlighter
import qdvc.cat.android.app.util.LanguageRegistry
import qdvc.cat.android.app.util.SyntaxColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    doc: DocState,
    fontSizeSp: Int,
    wordWrap: Boolean,
    fontFamily: FontFamily,
    onOpenSettings: () -> Unit,
) {
    val loaded = doc as? DocState.Loaded
    val grammar: Grammar? = remember(loaded?.displayName) {
        LanguageRegistry.grammarForFileName(loaded?.displayName)
    }
    val title = when (doc) {
        is DocState.Loaded -> doc.displayName ?: "Untitled"
        DocState.Empty -> "QDVC Cat"
        DocState.Loading -> "Opening…"
        is DocState.Failed -> "Couldn't open"
    }
    val subtitle = if (loaded != null) LanguageRegistry.languageLabel(grammar) else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                        )
                        if (subtitle != null) {
                            Text(
                                text = subtitle + if (loaded?.truncated == true) " · truncated" else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        when (doc) {
            is DocState.Loaded -> FileContent(
                lines = doc.lines,
                grammar = grammar,
                fontSizeSp = fontSizeSp,
                wordWrap = wordWrap,
                fontFamily = fontFamily,
                contentPadding = padding,
            )
            DocState.Empty -> CenteredMessage(
                "Open a text file with QDVC Cat",
                "Use \u201COpen with\u201D on a text file in your file browser.",
                padding,
            )
            DocState.Loading -> CenteredMessage("Opening\u2026", null, padding)
            is DocState.Failed -> CenteredMessage("Couldn't open the file", doc.message, padding)
        }
    }
}

@Composable
private fun CenteredMessage(title: String, body: String?, padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (body != null) {
                Text(
                    body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }
}

/** 1–2 px breathing room between the gutter and the code. */
private val GUTTER_TEXT_GAP: Dp = 2.dp

@Composable
private fun FileContent(
    lines: List<String>,
    grammar: Grammar?,
    fontSizeSp: Int,
    wordWrap: Boolean,
    fontFamily: FontFamily,
    contentPadding: PaddingValues,
) {
    // The entry-state pass is non-trivial for large files, so build off the
    // main thread; until it's ready, show raw (uncoloured) text.
    val highlighterState = produceState<Highlighter?>(initialValue = null, lines, grammar) {
        value = withContext(Dispatchers.Default) { Highlighter(lines, grammar) }
    }
    val highlighter = highlighterState.value
    val syntaxColors = LocalSyntaxColors.current

    val fontSize = fontSizeSp.sp
    val digits = remember(lines.size) { lines.size.toString().length }
    // Fixed gutter width based on digit count (monospace ≈ 0.62em per char).
    val gutterWidth: Dp = remember(digits, fontSizeSp) {
        (digits * fontSizeSp * 0.62f + 20f).dp
    }

    val lineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gutterBg = MaterialTheme.colorScheme.surface
    val contentColor = MaterialTheme.colorScheme.onBackground

    val textStyle = remember(fontSize, fontFamily, contentColor) {
        TextStyle(fontFamily = fontFamily, fontSize = fontSize, color = contentColor)
    }
    val numberStyle = remember(fontSize, lineNumberColor) {
        TextStyle(fontFamily = FontFamily.Monospace, fontSize = fontSize, color = lineNumberColor)
    }

    // ONE horizontal scroll state shared by every content cell. Because all
    // rows read the same offset, they move together and their columns stay
    // perfectly aligned. The gutter cells never get this modifier, so the
    // line-number panel stays fixed while the document scrolls sideways.
    val hScroll = rememberScrollState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        items(count = lines.size, key = { it }) { index ->
            val annotated = remember(index, syntaxColors, highlighter) {
                if (highlighter == null) {
                    val raw = lines[index]
                    AnnotatedString(if (raw.isEmpty()) " " else raw)
                } else {
                    buildLine(highlighter, index, syntaxColors)
                }
            }
            LineRow(
                lineNumber = index + 1,
                annotated = annotated,
                textStyle = textStyle,
                numberStyle = numberStyle,
                gutterWidth = gutterWidth,
                gutterBg = gutterBg,
                wordWrap = wordWrap,
                hScroll = hScroll,
            )
        }
    }
}

@Composable
private fun LineRow(
    lineNumber: Int,
    annotated: AnnotatedString,
    textStyle: TextStyle,
    numberStyle: TextStyle,
    gutterWidth: Dp,
    gutterBg: androidx.compose.ui.graphics.Color,
    wordWrap: Boolean,
    hScroll: androidx.compose.foundation.ScrollState,
) {
    // When wrapping, a line can span several visual rows, so we size the Row to
    // IntrinsicSize.Min and let the gutter fillMaxHeight to match — that keeps
    // the line-number panel's background continuous top-to-bottom (fix #3).
    // When NOT wrapping, every line is exactly one row tall, so we skip the
    // intrinsic pass entirely: it isn't needed AND horizontalScroll (used for
    // the whole-document sideways scroll) does not support intrinsic
    // measurement, so combining the two would crash.
    val rowModifier = Modifier
        .fillMaxWidth()
        .then(if (wordWrap) Modifier.height(IntrinsicSize.Min) else Modifier)
    Row(modifier = rowModifier) {
        // Gutter cell — fixed width, NOT horizontally scrolled. fillMaxHeight
        // only bites under IntrinsicSize.Min (the wrap case); otherwise the
        // cell is naturally one row tall, which is exactly right.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(gutterWidth)
                .background(gutterBg),
        ) {
            Text(
                text = lineNumber.toString(),
                style = numberStyle,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 6.dp),
            )
        }

        // Content cell.
        val contentModifier = if (wordWrap) {
            Modifier
                .weight(1f)
                .padding(start = GUTTER_TEXT_GAP, end = 8.dp)
        } else {
            // Whole-document horizontal scroll: shared state across all rows.
            Modifier
                .weight(1f)
                .horizontalScroll(hScroll)
                .padding(start = GUTTER_TEXT_GAP, end = 8.dp)
        }
        Text(
            text = annotated,
            style = textStyle,
            softWrap = wordWrap,
            modifier = contentModifier,
        )
    }
}

/** Turns one line's tokens into a coloured [AnnotatedString]. */
private fun buildLine(
    highlighter: Highlighter,
    index: Int,
    colors: SyntaxColors,
): AnnotatedString {
    val line = highlighter.lines.getOrNull(index) ?: return AnnotatedString(" ")
    if (line.isEmpty()) return AnnotatedString(" ") // keep row height for blank lines
    val tokens = highlighter.tokensForLine(index)
    if (tokens.isEmpty()) return AnnotatedString(line)
    return buildAnnotatedString {
        var cursor = 0
        for (t in tokens) {
            if (t.start > cursor) append(line.substring(cursor, t.start))
            val safeEnd = t.end.coerceAtMost(line.length)
            if (safeEnd > t.start) {
                withStyle(SpanStyle(color = colors.colorFor(t.type))) {
                    append(line.substring(t.start, safeEnd))
                }
            }
            cursor = safeEnd
        }
        if (cursor < line.length) append(line.substring(cursor))
    }
}
