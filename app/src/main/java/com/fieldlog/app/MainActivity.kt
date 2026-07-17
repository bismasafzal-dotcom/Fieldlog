package com.fieldlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.fieldlog.app.ui.AppScaffold
import com.fieldlog.app.ui.theme.FieldLogTheme
import com.fieldlog.app.ui.theme.SettingsStore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            // The whole app repaints the moment the user picks a different
            // background or accent — no restart, no reload.
            val settings by SettingsStore.settings.collectAsState()

            FieldLogTheme(mode = settings.mode, accent = settings.accent) {
                AppScaffold()
            }
        }
    }
}
