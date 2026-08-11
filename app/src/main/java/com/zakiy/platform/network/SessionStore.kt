package com.zakiy.platform.network

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "zakiy_session")

/** تخزين محلي لجلسة Supabase + تفضيلات التطبيق (لغة/مظهر) - يبقى بعد إغلاق
 * التطبيق (مطابق UserDefaults بـ iOS / localStorage بالموقع). */
class SessionStore(private val context: Context) {
    private object Keys {
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val USER_ID = stringPreferencesKey("user_id")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USERNAME = stringPreferencesKey("username")
        val LANGUAGE = stringPreferencesKey("language")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val GUEST_MODE = booleanPreferencesKey("guest_mode")
    }

    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { it[Keys.ACCESS_TOKEN] }
    val userIdFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_ID] }
    val usernameFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USERNAME] }
    val emailFlow: Flow<String?> = context.dataStore.data.map { it[Keys.USER_EMAIL] }
    val languageFlow: Flow<String?> = context.dataStore.data.map { it[Keys.LANGUAGE] }
    val darkModeFlow: Flow<Boolean?> = context.dataStore.data.map { it[Keys.DARK_MODE] }
    val guestModeFlow: Flow<Boolean?> = context.dataStore.data.map { it[Keys.GUEST_MODE] }

    suspend fun saveSession(accessToken: String, refreshToken: String, userId: String, email: String?) {
        context.dataStore.edit {
            it[Keys.ACCESS_TOKEN] = accessToken
            it[Keys.REFRESH_TOKEN] = refreshToken
            it[Keys.USER_ID] = userId
            if (email != null) it[Keys.USER_EMAIL] = email
            it[Keys.GUEST_MODE] = false
        }
    }

    suspend fun saveUsername(username: String) {
        context.dataStore.edit { it[Keys.USERNAME] = username }
    }

    suspend fun setLanguage(lang: String) {
        context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_MODE] = enabled }
    }

    suspend fun setGuestMode(enabled: Boolean) {
        context.dataStore.edit { it[Keys.GUEST_MODE] = enabled }
    }

    suspend fun readRefreshToken(): String? = context.dataStore.data.first()[Keys.REFRESH_TOKEN]

    suspend fun clearSession() {
        context.dataStore.edit {
            it.remove(Keys.ACCESS_TOKEN)
            it.remove(Keys.REFRESH_TOKEN)
            it.remove(Keys.USER_ID)
            it.remove(Keys.USER_EMAIL)
            it.remove(Keys.USERNAME)
        }
    }
}
