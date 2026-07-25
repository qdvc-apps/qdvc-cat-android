package qdvc.cat.android.app.model

/** The four style variants a custom font can supply for syntax highlighting. */
enum class FontVariant(val label: String) {
    REGULAR("Regular"),
    ITALIC("Italic"),
    BOLD("Bold"),
    BOLD_ITALIC("Bold Italic"),
}

/**
 * The display names of the custom-font files currently copied into the four
 * slots. A null entry means that slot is empty. The actual font files live in
 * app storage at fixed paths, so only these labels need to be surfaced to the
 * UI.
 */
data class CustomFontSet(
    val regularName: String? = null,
    val italicName: String? = null,
    val boldName: String? = null,
    val boldItalicName: String? = null,
) {
    fun nameFor(variant: FontVariant): String? = when (variant) {
        FontVariant.REGULAR -> regularName
        FontVariant.ITALIC -> italicName
        FontVariant.BOLD -> boldName
        FontVariant.BOLD_ITALIC -> boldItalicName
    }

    /** True if at least one slot has a file. */
    val hasAny: Boolean
        get() = regularName != null || italicName != null ||
            boldName != null || boldItalicName != null
}

/**
 * Font-selection sentinels stored in the id preference. A real device-font id
 * is that font file's absolute path; these two special values mean "use the
 * built-in monospace face" and "use the custom font set" respectively.
 */
object FontIds {
    const val DEFAULT = "__default__"
    const val CUSTOM = "__custom__"
}
