package vn.edu.usth.msma.utils.constants

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object DataStoreKeys {
    val AUTH_EMAIL = stringPreferencesKey("auth_email")
    val AUTH_ACCESS_TOKEN = stringPreferencesKey("auth_access_token")
    val AUTH_REFRESH_TOKEN = stringPreferencesKey("auth_refresh_token")
    val AUTH_IS_LOGGED_IN = booleanPreferencesKey("auth_is_logged_in")
}
