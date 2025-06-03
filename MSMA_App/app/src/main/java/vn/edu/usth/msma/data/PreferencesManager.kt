package vn.edu.usth.msma.data

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import vn.edu.usth.msma.service.MusicService
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val APP_PREFS = "app_prefs"
        private const val CURRENT_USER_KEY = "current_user_email"
        private val CURRENT_USER_EMAIL_PREF = stringPreferencesKey(CURRENT_USER_KEY)
        private val AUTH_EMAIL = stringPreferencesKey("auth_email")
        private val AUTH_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
        private val AUTH_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
        private val AUTH_IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")
        private val LAST_LOGIN_EMAIL = stringPreferencesKey("last_login_email")
        private val MINI_PLAYER_VISIBLE = booleanPreferencesKey("mini_player_visible")
        private val FAVORITE_SONGS = stringPreferencesKey("favorite_songs")
        private val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
    }

    // Cache for user-specific DataStores and their scopes
    private data class DataStoreWithScope(
        val dataStore: DataStore<Preferences>,
        val scope: CoroutineScope
    )
    private val userDataStores = ConcurrentHashMap<String, DataStoreWithScope>()

    // App-wide shared DataStore to keep track of the current user
    private val appDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(
        produceFile = { context.preferencesDataStoreFile(APP_PREFS) }
    )

    // Flow to observe current user email
    val currentUserEmailFlow: Flow<String?> = appDataStore.data
        .map { it[CURRENT_USER_EMAIL_PREF] }

    // Function to get the DataStore for a specific user
    private fun getUserDataStore(email: String): DataStore<Preferences> {
        val safeEmail = email.replace("[^A-Za-z0-9]".toRegex(), "_")
        return userDataStores.getOrPut(safeEmail) {
            val scope = CoroutineScope(Dispatchers.IO)
            val dataStore = PreferenceDataStoreFactory.create(
                produceFile = { context.preferencesDataStoreFile("prefs_$safeEmail") },
                corruptionHandler = null,
                scope = scope
            )
            DataStoreWithScope(dataStore, scope)
        }.dataStore
    }

    // Save current user email in appDataStore
    suspend fun setCurrentUser(email: String) {
        appDataStore.edit { it[CURRENT_USER_EMAIL_PREF] = email }
    }

    suspend fun clearCurrentUser() {
        appDataStore.edit { it.remove(CURRENT_USER_EMAIL_PREF) }
    }

    // User-specific flows
    fun getEmailFlow(email: String): Flow<String?> =
        getUserDataStore(email).data.map { it[AUTH_EMAIL] }

    fun getAccessTokenFlow(email: String): Flow<String?> =
        getUserDataStore(email).data.map { it[AUTH_ACCESS_TOKEN] }

    fun getRefreshTokenFlow(email: String): Flow<String?> =
        getUserDataStore(email).data.map { it[AUTH_REFRESH_TOKEN] }

    fun getIsLoggedInFlow(email: String): Flow<Boolean> =
        getUserDataStore(email).data.map { it[AUTH_IS_LOGGED_IN] == true }

    // Save functions
    suspend fun saveEmail(email: String) {
        getUserDataStore(email).edit {
            it[AUTH_EMAIL] = email
        }
        setCurrentUser(email)
    }

    suspend fun saveAccessToken(email: String, token: String) {
        getUserDataStore(email).edit { it[AUTH_ACCESS_TOKEN] = token }
    }

    suspend fun saveRefreshToken(email: String, token: String) {
        getUserDataStore(email).edit { it[AUTH_REFRESH_TOKEN] = token }
    }

    suspend fun saveIsLoggedIn(email: String, value: Boolean) {
        getUserDataStore(email).edit { it[AUTH_IS_LOGGED_IN] = value }
    }

    // Clear user-specific data
    suspend fun clearUserData(email: String) {
        val safeEmail = email.replace("[^A-Za-z0-9]".toRegex(), "_")
        getUserDataStore(email).edit { it.clear() }
        userDataStores[safeEmail]?.scope?.cancel()
        userDataStores.remove(safeEmail)
    }

    suspend fun saveLastLoginEmail(email: String) {
        appDataStore.edit { preferences ->
            preferences[LAST_LOGIN_EMAIL] = email
        }
    }

    suspend fun clearLastLoginEmail() {
        appDataStore.edit { preferences ->
            preferences.remove(LAST_LOGIN_EMAIL)
        }
    }

    fun getLastLoginEmail(): Flow<String?> {
        return appDataStore.data.map { preferences ->
            preferences[LAST_LOGIN_EMAIL]
        }
    }

    // Additional functions
    val isMiniPlayerVisibleFlow: Flow<Boolean> = appDataStore.data.map { preferences ->
        preferences[MINI_PLAYER_VISIBLE] == true
    }

    suspend fun setMiniPlayerVisible(visible: Boolean) {
        appDataStore.edit { preferences ->
            preferences[MINI_PLAYER_VISIBLE] = visible
        }
    }

    suspend fun saveFcmToken(token: String) {
        appDataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    fun getFcmToken(): Flow<String?> {
        return appDataStore.data.map { preferences ->
            preferences[FCM_TOKEN_KEY]
        }
    }

    suspend fun addToFavorites(songId: Long) {
        appDataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITE_SONGS]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            val newFavorites = currentFavorites + songId.toString()
            preferences[FAVORITE_SONGS] = newFavorites.joinToString(",")
        }
        broadcastFavoriteChange(songId, true)
    }

    suspend fun removeFromFavorites(songId: Long) {
        appDataStore.edit { preferences ->
            val currentFavorites = preferences[FAVORITE_SONGS]?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
            val newFavorites = currentFavorites - songId.toString()
            preferences[FAVORITE_SONGS] = newFavorites.joinToString(",")
        }
        broadcastFavoriteChange(songId, false)
    }

    private suspend fun broadcastFavoriteChange(songId: Long, isFavorite: Boolean) {
        val intent = Intent("MUSIC_EVENT").apply {
            putExtra("ACTION", if (isFavorite) "ADDED_TO_FAVORITES" else "REMOVED_FROM_FAVORITES")
            putExtra("SONG_ID", songId)
        }
        context.sendBroadcast(intent)
    }

    suspend fun clearAll() {
        val email = appDataStore.data.first()[CURRENT_USER_EMAIL_PREF]
        email?.let { clearUserData(it) }
        clearCurrentUser()
    }

    // Logout: Clear all data except LAST_LOGIN_EMAIL
    @SuppressLint("ObsoleteSdkInt")
    suspend fun logout() {
        val email = appDataStore.data.first()[CURRENT_USER_EMAIL_PREF]
        email?.let {
            // Stop music playback and close notification
            val intent = Intent(context, MusicService::class.java).apply {
                action = "CLOSE"
            }
            context.startService(intent)

            // Close notification channel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.getNotificationChannel("music_channel")?.let { channel ->
                    channel.enableLights(false)
                    channel.enableVibration(false)
                    channel.setSound(null, null)
                }
            }

            // Clear user data
            clearUserData(it)
            clearCurrentUser()
            appDataStore.edit { preferences ->
                preferences.remove(AUTH_ACCESS_TOKEN)
                preferences.remove(AUTH_REFRESH_TOKEN)
                preferences.remove(AUTH_IS_LOGGED_IN)
                preferences.remove(CURRENT_USER_EMAIL_PREF)
                preferences.remove(MINI_PLAYER_VISIBLE)
            }
        }
        // Cancel all scopes and clear the map
        userDataStores.values.forEach { it.scope.cancel() }
        userDataStores.clear()
    }

    // Delete: Clear everything including LAST_LOGIN_EMAIL
    @SuppressLint("ObsoleteSdkInt")
    suspend fun delete() {
        val email = appDataStore.data.first()[CURRENT_USER_EMAIL_PREF]

        // Stop music playback and close notification
        val intent = Intent(context, MusicService::class.java).apply {
            action = "CLOSE"
        }
        context.startService(intent)

        // Close notification channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.getNotificationChannel("music_channel")?.let { channel ->
                channel.enableLights(false)
                channel.enableVibration(false)
                channel.setSound(null, null)
            }
        }

        email?.let { clearUserData(it) }
        clearCurrentUser()
        // Clear everything including LAST_LOGIN_EMAIL
        appDataStore.edit { preferences ->
            preferences.clear()
        }
        // Cancel all scopes and clear the map
        userDataStores.values.forEach { it.scope.cancel() }
        userDataStores.clear()
    }
}