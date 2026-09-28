package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.spore.ui.screens.CellEditorScreen
import com.example.spore.ui.screens.GameScreen
import com.example.spore.ui.screens.MainMenuScreen
import com.example.spore.ui.screens.TrophicWebScreen
import com.example.spore.ui.viewmodel.AppScreen
import com.example.spore.ui.viewmodel.SporeViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: SporeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()

                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                    color = Color(0xFF040D1A)
                ) {
                    Crossfade(
                        targetState = currentScreen,
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
                            AppScreen.MAIN_MENU -> MainMenuScreen(viewModel = viewModel)
                            AppScreen.GAME -> GameScreen(viewModel = viewModel)
                            AppScreen.CELL_EDITOR -> CellEditorScreen(viewModel = viewModel)
                            AppScreen.TROPHIC_WEB -> TrophicWebScreen(viewModel = viewModel)
                        }
                    }
                }
            }
        }
    }
}
