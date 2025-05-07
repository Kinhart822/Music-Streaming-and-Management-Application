package vn.edu.usth.msma.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.ConcurrentHashMap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelChildren
import javax.inject.Inject
import javax.inject.Singleton


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

    // Logout: Clear all data except LAST_LOGIN_EMAIL
    suspend fun logout() {
        val email = appDataStore.data.first()[CURRENT_USER_EMAIL_PREF]
        email?.let { 
            // Clear user data
            clearUserData(it)
            // Clear current user
            clearCurrentUser()
            // Clear tokens and login state
            appDataStore.edit { preferences ->
                preferences.remove(AUTH_ACCESS_TOKEN)
                preferences.remove(AUTH_REFRESH_TOKEN)
                preferences.remove(AUTH_IS_LOGGED_IN)
                preferences.remove(CURRENT_USER_EMAIL_PREF)
            }
        }
        // Cancel all scopes and clear the map
        userDataStores.values.forEach { it.scope.cancel() }
        userDataStores.clear()
    }

    // Delete: Clear everything including LAST_LOGIN_EMAIL
    suspend fun delete() {
        val email = appDataStore.data.first()[CURRENT_USER_EMAIL_PREF]
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
}
