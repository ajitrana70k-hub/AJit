package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "my_video_prefs")

enum class ThemePreference { SYSTEM, DARK, LIGHT }

class AppPreferences(private val context: Context) {
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("current_user_id")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_PUSH_NOTIFS = booleanPreferencesKey("push_notifs_enabled")
        private val KEY_AUTOPLAY = booleanPreferencesKey("autoplay_enabled")
        private val KEY_CAPTIONS_DEFAULT = booleanPreferencesKey("captions_default")
        private val KEY_YT_CLIENT_ID = stringPreferencesKey("youtube_oauth_client_id")
    }

    val currentUserId: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_ID]
    }

    val themeMode: Flow<ThemePreference> = context.dataStore.data.map { prefs ->
        val raw = prefs[KEY_THEME_MODE] ?: ThemePreference.DARK.name
        ThemePreference.entries.firstOrNull { it.name == raw } ?: ThemePreference.DARK
    }

    val pushNotificationsEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PUSH_NOTIFS] ?: true
    }

    val autoplayEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AUTOPLAY] ?: true
    }

    val captionsDefaultEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_CAPTIONS_DEFAULT] ?: false
    }

    val youtubeOAuthClientId: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_YT_CLIENT_ID] ?: ""
    }

    suspend fun setCurrentUserId(userId: String?) {
        context.dataStore.edit { prefs ->
            if (userId == null) prefs.remove(KEY_USER_ID)
            else prefs[KEY_USER_ID] = userId
        }
    }

    suspend fun setThemeMode(mode: ThemePreference) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    suspend fun setPushNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PUSH_NOTIFS] = enabled
        }
    }

    suspend fun setAutoplayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTOPLAY] = enabled
        }
    }

    suspend fun setCaptionsDefaultEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CAPTIONS_DEFAULT] = enabled
        }
    }

    suspend fun setYoutubeOAuthClientId(clientId: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_YT_CLIENT_ID] = clientId.trim()
        }
    }
}
