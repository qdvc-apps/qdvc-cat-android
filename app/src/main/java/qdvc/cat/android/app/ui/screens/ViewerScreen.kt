package qdvc.cat.android.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@Composable
private fun FileContent(
    lines: List<String>,
    grammar: Grammar?,
    fontSizeSp: Int,
    wordWrap: Boolean,
    contentPadding: PaddingValues,
) {
    // Building the highlighter does a one-time forward pass to compute per-line
    // grammar states; for large files that's non-trivial, so do it off the main
    // thread and show the raw (uncoloured) text until it's ready.
    val highlighterState = produceState<Highlighter?>(initialValue = null, lines, grammar) {
        value = withContext(Dispatchers.Default) { Highlighter(lines, grammar) }
    }
    val highlighter = highlighterState.value
    val syntaxColors = LocalSyntaxColors.current
    val gutterWidth = remember(lines.size) { (lines.size.toString().length) }
    val hScroll = rememberScrollState()

    val fontSize = fontSizeSp.sp
    val lineNumberColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gutterBg = MaterialTheme.colorScheme.surface

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
            Row(modifier = Modifier.fillMaxWidth()) {
                // Line-number gutter (fixed, doesn't scroll horizontally).
                Text(
                    text = (index + 1).toString().padStart(gutterWidth, ' '),
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    color = lineNumberColor,
                    modifier = Modifier
                        .background(gutterBg)
                        .padding(horizontal = 8.dp),
                )
                val textModifier = if (wordWrap) {
                    Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                } else {
                    Modifier
                        .weight(1f)
                        .horizontalScroll(hScroll)
                        .widthIn(min = 1.dp)
                        .padding(end = 8.dp)
                }
                Text(
                    text = annotated,
                    fontFamily = FontFamily.Monospace,
                    fontSize = fontSize,
                    softWrap = wordWrap,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = textModifier,
                )
            }
        }
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
