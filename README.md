# QDVC Cat

A simple, fast Android text-file viewer. It opens a plaintext file and shows it
in a monospace font, with optional syntax highlighting for common formats.

## Features

- **Plain-text viewing** in a monospace font with line numbers.
- **Syntax highlighting** for YAML (`.yaml`, `.yml`), JSON (`.json`), CSV, TSV,
  SQL (`.sql`), Python (`.py`), CSS (`.css`), and Markdown (`.md`, `.markdown`,
  including YAML frontmatter). Plain `.txt` (and any unknown extension) is shown
  without highlighting.
- **"Open with" integration** — QDVC Cat registers as a handler for text files,
  so it appears in your file browser's *Open with* menu.
- **Settings** — light/dark mode (defaulting to whichever mode your OS is in),
  a choice of colour themes, a font selection (built-in monospace, any device
  font, or a custom font supplied as up to four `.ttf`/`.otf` style files),
  adjustable text size, and word wrap.
- **Colour themes** carried over from
  [`qdvc-markdown-notebook-android`](https://github.com/qdvc-apps/qdvc-markdown-notebook-android):
  Regular Light/Dark, Pure Black (OLED), Everforest Light/Dark, and Rosé Pine /
  Rosé Pine Moon / Rosé Pine Dawn — each extended with a full syntax palette.

## Build & run

Requires Android Studio (Koala / 2024.1+), Android SDK 34, min device API 26
(Android 8.0), and JDK 17.

```
./gradlew assembleDebug
```

The debug APK lands in `app/build/outputs/apk/debug/`.

## How syntax highlighting is organised

Highlighting is deliberately **modular and data-driven**. Each language is a
declarative grammar — an ordered set of regex rules with optional state
transitions — in the spirit of Monaco's *Monarch*, Prism, and CodeMirror's
"simple mode". This is a pragmatic, industry-recognisable format that avoids the
overkill of a full TextMate/oniguruma engine while still supporting multi-line
constructs (block comments, triple-quoted strings, YAML frontmatter) via a
per-line state stack.

Adding a language is a self-contained change:

1. Write a `Grammar` in `app/src/main/java/qdvc/cat/android/app/util/grammars/`.
2. Register its file extension(s) in `LanguageRegistry`.

Grammars emit **semantic scopes** (`KEYWORD`, `STRING`, `COMMENT`, …), not
concrete colours. Each theme maps those scopes to colours via its `syntax`
block, so one small palette drives every language and switching themes recolours
all highlighting automatically. See `docs/MAINTENANCE.md` for details.

## Namespace

`qdvc.cat.android.app`
