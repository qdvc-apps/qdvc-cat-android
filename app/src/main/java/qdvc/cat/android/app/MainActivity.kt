package qdvc.cat.android.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import qdvc.cat.android.app.data.ThemeRepository
import qdvc.cat.android.app.ui.AppViewModel
import qdvc.cat.android.app.ui.DocState
import qdvc.cat.android.app.ui.Screen
import qdvc.cat.android.app.ui.screens.SettingsScreen
import qdvc.cat.android.app.ui.screens.ViewerScreen
import qdvc.cat.android.app.ui.theme.QdvcCatTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val screen by viewModel.screen.collectAsState()
            val doc by viewModel.doc.collectAsState()

            val context = this
            val lightTheme = ThemeRepository.lightOrDefault(context, settings.lightThemeId)
            val darkTheme = ThemeRepository.darkOrDefault(context, settings.darkThemeId)

            QdvcCatTheme(
                themeMode = settings.themeMode,
                lightTheme = lightTheme,
                darkTheme = darkTheme,
            ) {
                AnimatedContent(
                    targetState = screen,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "screen",
                ) { target ->
                    when (target) {
                        Screen.VIEWER -> ViewerScreen(
                            doc = doc,
                            fontSizeSp = settings.fontSizeSp,
                            wordWrap = settings.wordWrap,
                            onOpenSettings = { viewModel.openScreen(Screen.SETTINGS) },
                        )
                        Screen.SETTINGS -> SettingsScreen(
                            settings = settings,
                            lightThemes = ThemeRepository.lightThemes(context),
                            darkThemes = ThemeRepository.darkThemes(context),
                            onThemeMode = viewModel::setThemeMode,
                            onLightTheme = viewModel::setLightTheme,
                            onDarkTheme = viewModel::setDarkTheme,
                            onFontSize = viewModel::setFontSize,
                            onWordWrap = viewModel::setWordWrap,
                            onBack = { viewModel.openScreen(Screen.VIEWER) },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
        viewModel.openScreen(Screen.VIEWER)
    }

    /** If launched via VIEW (Open with), load the supplied Uri. */
    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.action == Intent.ACTION_VIEW) {
            val uri = intent.data
            if (uri != null) {
                viewModel.loadUri(uri)
                return
            }
        }
        // MAIN launch with nothing loaded yet stays on the empty state.
        if (viewModel.doc.value == DocState.Empty) {
            viewModel.openScreen(Screen.VIEWER)
        }
    }
}
