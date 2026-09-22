package com.ainotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotes.app.settings.ThemeMode
import com.ainotes.app.ui.navigation.AppNav
import com.ainotes.app.ui.theme.AiNotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val container = (application as AiNotesApp).container

        setContent {
            val themeMode by container.settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val accent by container.settings.accent.collectAsState(initial = "#7C4DFF")
            val onboardingDone by container.settings.onboardingDone.collectAsState(initial = true)

            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            AiNotesTheme(darkTheme = dark, accentHex = accent) {
                AppNav(container = container, onboardingDone = onboardingDone)
            }
        }
    }
}
