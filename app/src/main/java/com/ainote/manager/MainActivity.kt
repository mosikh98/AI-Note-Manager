package com.ainote.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.ainote.manager.ui.navigation.AppNavGraph
import com.ainote.manager.ui.theme.AppearancePreferences
import com.ainote.manager.ui.theme.AppearanceSettings
import com.ainote.manager.ui.theme.AiNoteManagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AiNoteManagerApp()
        }
    }
}

@Composable
fun AiNoteManagerApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val appearance by AppearancePreferences.observe(context).collectAsState(initial = AppearanceSettings())
    val systemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val darkTheme = when (appearance.theme) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    AiNoteManagerTheme(
        darkTheme = darkTheme,
        accent = appearance.accent,
        background = appearance.background,
        fontScale = appearance.fontScale,
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNavGraph()
        }
    }
}
