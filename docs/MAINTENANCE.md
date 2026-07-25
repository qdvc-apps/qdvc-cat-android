# Maintenance & architecture

A short map of the codebase for anyone extending it.

## Layers

- **`model/`** — plain data. `ThemeMode`, `ThemeSpec`/`ThemeColors`/`SyntaxPalette`.
- **`data/`** — `SettingsRepository` (DataStore-backed preferences) and
  `ThemeRepository` (loads JSON themes from `assets/themes/`, builds Compose
  `ColorScheme`s and the highlighter's `SyntaxColors`).
- **`util/`** — the file loader and the syntax-highlighting engine.
- **`ui/`** — `AppViewModel`, the two screens (`ViewerScreen`, `SettingsScreen`),
  and the theme wiring.
- **`MainActivity`** — handles the incoming `VIEW` intent ("Open with") and
  hosts the Compose tree.

## Opening files

`MainActivity.handleIntent` reads the `Intent`'s `data` Uri and hands it to
`AppViewModel.loadUri`, which reads it on the IO dispatcher via `FileLoader`.
`FileLoader` is defensive: it caps reads at 8 MB (marking the result
truncated), sniffs for binary content (many NUL bytes), and resolves a display
name from `OpenableColumns`.

The manifest registers three `VIEW` intent-filters: one for `text/*`, one for a
set of structured `application/*` MIME types, and an extension-based fallback
(`pathPattern`) for browsers that report `application/octet-stream`.

## Syntax highlighting

The engine is a small declarative tokeniser (Monarch/Prism-style):

- **`Grammar`** — a map of named states, each an ordered list of **`Rule`**s.
  A rule has an anchored regex, an optional semantic `TokenType`, optional
  `push`/`pop` state transitions, and optional per-capture-group scopes
  (`groupTokens`) for cases like colouring a `key:` differently from its value.
- **`Tokenizer`** — runs a grammar over one line at a time, carrying a state
  stack across line boundaries so multi-line constructs work. It always makes
  progress (emitting a plain char if no rule matches) and has a per-line guard
  against pathological input.
- **`Highlighter`** — on construction does ONE cheap forward pass computing the
  entry-state for each line (no tokens retained). `tokensForLine(i)` then
  tokenises any single line in O(line length). This is what keeps the viewer
  fast: `ViewerScreen` renders lines lazily in a `LazyColumn` and only tokenises
  the handful currently on screen. The construction pass itself runs off the
  main thread (`produceState` + `Dispatchers.Default`).
- **`LanguageRegistry`** — the single place mapping file extensions to grammars.

### Adding a language

1. Create `util/grammars/FooGrammar.kt` defining a `Grammar`.
2. Add `put("foo", FooGrammar)` (and any aliases) to `LanguageRegistry`.
3. Add a label case in `LanguageRegistry.languageLabel` if you want a nice name.

Nothing else changes — grammars emit semantic scopes, and themes already map
every scope to a colour.

## Themes

Themes are JSON files in `assets/themes/`, each with an `id`, `name`, `dark`
flag, a `colors` block (the ~11 Material roles the chrome uses), and an optional
`syntax` block (the ten semantic highlighting scopes). Any omitted `syntax`
colour falls back to a sensible Material role in `ThemeRepository.syntaxColors`,
so a theme can specify as few or as many as it likes.

`Theme.kt` publishes the active `SyntaxColors` through the `LocalSyntaxColors`
composition local; `ViewerScreen` reads it when building each line's
`AnnotatedString`. Because the palette comes from the active theme, switching
themes (or light/dark) recolours everything automatically.

The base UI palettes (Regular, Pure Black, Everforest, Rosé Pine family) are the
ones from `qdvc-markdown-notebook-android`. The extra syntax accents come from
the same upstream sources that repo used — the Everforest *medium* variant
(sainnhe) and the official Rosé Pine palette — so the highlighting stays within
each theme's colour family.

## Adding a theme

Drop a new `.json` into `assets/themes/`. It's picked up automatically and
appears in the light or dark list according to its `dark` flag.

## Fonts

Adapted from `qdvc-markdown-notebook-android`'s font system (`SystemFonts`).
The stored font id is one of: the default sentinel (`FontIds.DEFAULT` →
built-in monospace), a device-font file path (discovered by scanning the system
font directories), or the custom sentinel (`FontIds.CUSTOM`). A custom font is
up to four user-supplied files — Regular / Italic / Bold / Bold-Italic — copied
into the app's private storage at fixed paths and assembled into one
`FontFamily`. `AppViewModel.fontFamilyFor` resolves the id to a `FontFamily`
that `ViewerScreen` applies to the content text (line numbers stay monospace so
the gutter width is predictable).

## Viewer layout (scroll & gutter)

`ViewerScreen` renders each line as a `Row` of `[gutter cell][content cell]`
inside a `LazyColumn`, so only on-screen lines are laid out and tokenised.

- **Whole-document horizontal scroll.** Every content cell shares ONE
  `ScrollState` via `horizontalScroll`. Because all rows read the same scroll
  offset, they move together and stay column-aligned — you can't get one line
  shifted relative to another. The gutter cells never take the scroll modifier,
  so the line-number panel stays fixed while the document scrolls sideways.
- **Continuous gutter under word wrap.** When wrap is on, a logical line can
  occupy several visual rows. The `Row` is sized with `IntrinsicSize.Min` and
  the gutter `Box` uses `fillMaxHeight`, so the gutter background paints the
  full height of the wrapped block and the panel runs unbroken down the side.
  When wrap is off we skip the intrinsic pass — it isn't needed, and
  `horizontalScroll` doesn't support intrinsic measurement, so mixing them
  would crash.
- **Gutter/text gap.** A 2 dp `GUTTER_TEXT_GAP` sits between the number panel
  and the code.
