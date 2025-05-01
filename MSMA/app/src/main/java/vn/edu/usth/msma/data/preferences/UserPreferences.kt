package vn.edu.usth.msma.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

class UserPreferences(private val dataStore: DataStore<Preferences>) {
    companion object {
        private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    }

    suspend fun saveLoginState(isLoggedIn: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = isLoggedIn
        }
    }

    suspend fun isLoggedIn(): Boolean {
        return dataStore.data.first()[IS_LOGGED_IN] == true
    }
}