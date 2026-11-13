package com.voicenote.transcriber

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.io.IOException

sealed interface UiState {
    data object Idle : UiState
    data class Ready(val audio: ImportedAudio) : UiState
    data class Transcribing(val fileName: String) : UiState
    data class Done(val fileName: String, val transcript: String) : UiState
}

data class ImportedAudio(val file: File, val displayName: String, val mimeType: String)

class NotAudioException : Exception()

class MainViewModel(app: Application) : AndroidViewModel(app) {

    val settings = SettingsStore(app)

    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _toasts = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val toasts = _toasts.asSharedFlow()

    fun notify(message: String) {
        _toasts.tryEmit(message)
    }

    fun onSharedAudio(uri: Uri) {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Ready(importAudio(uri))
            } catch (_: NotAudioException) {
                notify("Not an audio file")
            } catch (e: Exception) {
                notify("Couldn't read the shared file: ${e.message}")
            }
        }
    }

    fun transcribe() {
        val ready = _uiState.value as? UiState.Ready ?: return
        _uiState.value = UiState.Transcribing(ready.audio.displayName)
        viewModelScope.launch {
            try {
                val api = ApiClient.create(settings.backendUrl, BuildConfig.DEBUG)
                val body = ready.audio.file
                    .asRequestBody(ready.audio.mimeType.toMediaTypeOrNull())
                val part = MultipartBody.Part
                    .createFormData("audio", ready.audio.displayName, body)
                val transcript = api.transcribe(part).transcript
                _uiState.value = UiState.Done(ready.audio.displayName, transcript)
            } catch (e: HttpException) {
                _uiState.value = ready
                notify("Server error ${e.code()}")
            } catch (e: IOException) {
                _uiState.value = ready
                notify("Network error - check the backend URL in Settings")
            } catch (e: IllegalArgumentException) {
                _uiState.value = ready
                notify("Invalid backend URL - check Settings")
            } catch (e: Exception) {
                _uiState.value = ready
                notify("Failed: ${e.message}")
            }
        }
    }

    fun clear() {
        _uiState.value = UiState.Idle
    }

    private suspend fun importAudio(uri: Uri): ImportedAudio =
        withContext(Dispatchers.IO) {
            val resolver = getApplication<Application>().contentResolver

            val displayName = resolver
                .query(uri, null, null, null, null)?.use { cursor ->
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                }
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
                ?: "voice_note"

            val mime = resolver.getType(uri)
            val extension = displayName.substringAfterLast('.', "").lowercase()
            val looksLikeAudio = mime?.startsWith("audio/") == true ||
                extension in AUDIO_EXTENSIONS
            if (!looksLikeAudio) throw NotAudioException()

            val safeName = displayName
                .replace(Regex("[^A-Za-z0-9._-]"), "_")
                .ifBlank { "voice_note.ogg" }
            val dir = File(getApplication<Application>().filesDir, "inbox").apply { mkdirs() }
            val dest = File(dir, "${System.currentTimeMillis()}_$safeName")

            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { input.copyTo(it) }
            } ?: throw IOException("Cannot open shared file")

            ImportedAudio(dest, displayName, mime ?: "audio/*")
        }

    private companion object {
        val AUDIO_EXTENSIONS = setOf(
            "ogg", "oga", "opus", "mp3", "m4a", "aac", "wav", "amr",
            "3gp", "flac", "mp4", "m4b", "webm"
        )
    }
}
