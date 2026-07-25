package qdvc.cat.android.app.model

/**
 * How the app decides between its light and dark colour themes.
 *
 * [AUTOMATIC] follows the OS setting (the default). [LIGHT] and [DARK] pin the
 * app to one mode regardless of the system.
 */
enum class ThemeMode {
    AUTOMATIC,
    LIGHT,
    DARK,
}
