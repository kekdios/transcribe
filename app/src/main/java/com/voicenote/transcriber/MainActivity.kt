package com.voicenote.transcriber

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.IntentCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voicenote.transcriber.ui.MainScreen
import com.voicenote.transcriber.ui.SettingsScreen
import com.voicenote.transcriber.ui.VoiceNoteTranscriberTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        setContent {
            VoiceNoteTranscriberTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()
                var showSettings by rememberSaveable { mutableStateOf(false) }
                val context = LocalContext.current

                LaunchedEffect(Unit) {
                    viewModel.toasts.collect {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }

                if (showSettings) {
                    SettingsScreen(
                        initialUrl = viewModel.settings.backendUrl,
                        onSave = {
                            viewModel.settings.backendUrl = it
                            showSettings = false
                            viewModel.notify("Backend URL saved")
                        },
                        onBack = { showSettings = false }
                    )
                } else {
                    MainScreen(
                        state = state,
                        onTranscribe = viewModel::transcribe,
                        onClear = viewModel::clear,
                        onOpenSettings = { showSettings = true }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action != Intent.ACTION_SEND) return
        val uri = IntentCompat.getParcelableExtra(
            intent, Intent.EXTRA_STREAM, Uri::class.java
        ) ?: intent.data
            ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
        if (uri != null) {
            viewModel.onSharedAudio(uri)
        } else {
            viewModel.notify("No audio file in share")
        }
    }
}
