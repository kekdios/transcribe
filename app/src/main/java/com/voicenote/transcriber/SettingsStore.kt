package com.voicenote.transcriber

import android.content.Context

class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var backendUrl: String
        get() = prefs.getString(KEY_BACKEND_URL, DEFAULT_BACKEND_URL)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_BACKEND_URL
        set(value) = prefs.edit().putString(KEY_BACKEND_URL, value).apply()

    companion object {
        private const val PREFS_NAME = "settings"
        private const val KEY_BACKEND_URL = "backend_url"

        // 10.0.2.2 reaches the host machine from the Android emulator.
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:8000"
    }
}
