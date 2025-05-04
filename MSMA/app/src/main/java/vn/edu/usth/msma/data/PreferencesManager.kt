package vn.edu.usth.msma.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import vn.edu.usth.msma.utils.constants.DataStoreKeys

class PreferencesManager(val context: Context) {
    companion object {
        private const val APP_PREFS = "app_prefs"
        private const val CURRENT_USER_KEY = "current_user_email"
    }

    private val appPrefs: SharedPreferences =
        context.getSharedPreferences(APP_PREFS, Context.MODE_PRIVATE)
    private val _currentUserEmailFlow = MutableStateFlow(getCurrentUserEmail())
    val currentUserEmailFlow: Flow<String?> = _currentUserEmailFlow.asStateFlow()

    private fun getUserPrefs(email: String): SharedPreferences {
        val safeEmail = email.replace("[^A-Za-z0-9]".toRegex(), "_")
        return context.getSharedPreferences("prefs_$safeEmail", Context.MODE_PRIVATE)
    }

    private fun getCurrentUserPrefs(): SharedPreferences? {
        val email = getCurrentUserEmail() ?: return null
        return getUserPrefs(email)
    }

    private fun getCurrentUserEmail(): String? {
        return appPrefs.getString(CURRENT_USER_KEY, null)
    }

    // Set current user
    fun setCurrentUser(email: String) {
        appPrefs.edit { putString(CURRENT_USER_KEY, email) }
        _currentUserEmailFlow.value = email
    }

    // Clear current user
    fun clearCurrentUser() {
        appPrefs.edit { remove(CURRENT_USER_KEY) }
        _currentUserEmailFlow.value = null
    }

    // Get Email
    val email: Flow<String?>
        get() = _currentUserEmailFlow

    fun saveEmail(email: String) {
        setCurrentUser(email)
        getUserPrefs(email).edit { putString(DataStoreKeys.AUTH_EMAIL.name, email) }
    }

    // Get Access Token
    val accessToken: Flow<String?>
        get() = MutableStateFlow(
            getCurrentUserPrefs()?.getString(DataStoreKeys.AUTH_ACCESS_TOKEN.name, null)
        ).asStateFlow()

    fun saveAccessToken(token: String) {
        getCurrentUserPrefs()?.edit { putString(DataStoreKeys.AUTH_ACCESS_TOKEN.name, token) }
    }

    fun removeAccessToken() {
        getCurrentUserPrefs()?.edit { remove(DataStoreKeys.AUTH_ACCESS_TOKEN.name) }
    }

    // Get Refresh Token
    val refreshToken: Flow<String?>
        get() = MutableStateFlow(
            getCurrentUserPrefs()?.getString(DataStoreKeys.AUTH_REFRESH_TOKEN.name, null)
        ).asStateFlow()

    fun saveRefreshToken(token: String) {
        getCurrentUserPrefs()?.edit { putString(DataStoreKeys.AUTH_REFRESH_TOKEN.name, token) }
    }

    // Get Login State
    val isLoggedIn: Flow<Boolean>
        get() = MutableStateFlow(
            getCurrentUserPrefs()?.getBoolean(DataStoreKeys.AUTH_IS_LOGGED_IN.name, false) == true
        ).asStateFlow()

    fun saveIsLoggedIn(value: Boolean) {
        getCurrentUserPrefs()?.edit { putBoolean(DataStoreKeys.AUTH_IS_LOGGED_IN.name, value) }
    }

    fun clearAll() {
        getCurrentUserPrefs()?.edit { clear() }
        clearCurrentUser()
    }
}