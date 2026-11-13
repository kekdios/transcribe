package com.voicenote.transcriber.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voicenote.transcriber.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    state: UiState,
    onTranscribe: () -> Unit,
    onClear: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VoiceNote Transcriber") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (state) {
                is UiState.Idle -> Text(
                    text = "Long-press a voice note in WhatsApp, tap Share, and pick this app to transcribe it.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )

                is UiState.Ready -> {
                    Text(
                        text = state.audio.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = onTranscribe) { Text("Transcribe") }
                }

                is UiState.Transcribing -> {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("Transcribing ${state.fileName}…")
                }

                is UiState.Done -> TranscriptResult(state, onClear)
            }
        }
    }
}

@Composable
private fun ColumnScope.TranscriptResult(state: UiState.Done, onClear: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    Text(
        text = state.fileName,
        style = MaterialTheme.typography.titleMedium,
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(16.dp))
    Surface(
        tonalElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
    ) {
        Text(
            text = state.transcript,
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
    Spacer(Modifier.height(16.dp))
    Row {
        OutlinedButton(onClick = {
            clipboard.setText(AnnotatedString(state.transcript))
            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
        }) { Text("Copy") }
        Spacer(Modifier.width(12.dp))
        Button(onClick = {
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, state.transcript)
            }
            context.startActivity(
                Intent.createChooser(send, "Send transcript")
            )
        }) {
            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Send back")
        }
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(onClick = onClear) { Text("Clear") }
}
