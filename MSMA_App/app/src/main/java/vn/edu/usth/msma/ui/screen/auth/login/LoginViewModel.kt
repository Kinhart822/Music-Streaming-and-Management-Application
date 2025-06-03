package vn.edu.usth.msma.ui.screen.auth.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.request.auth.SignInRequest
import vn.edu.usth.msma.network.ApiService
import java.util.regex.Pattern
import javax.inject.Inject

data class LoginState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isLoggedIn: Boolean = false,
    val passwordVisible: Boolean = false,
    val rememberMe: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val state: StateFlow<LoginState> = _state.asStateFlow()

    init {
        // Load saved email if exists
        viewModelScope.launch {
            preferencesManager.getLastLoginEmail().collect { savedEmail ->
                if (savedEmail != null) {
                    _state.update { it.copy(email = savedEmail, rememberMe = true) }
                }
            }
        }
    }

    fun updateEmail(email: String) {
        _state.update { it.copy(email = email, emailError = null) }
    }

    fun updatePassword(password: String) {
        _state.update { it.copy(password = password, passwordError = null) }
    }

    fun togglePasswordVisibility() {
        _state.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun onRememberMeChanged(rememberMe: Boolean) {
        _state.update { it.copy(rememberMe = rememberMe) }
    }

    fun login() {
        val email = _state.value.email
        val password = _state.value.password
        var isValid = true

        if (!isValidEmail(email)) {
            _state.update { it.copy(emailError = "Invalid email format") }
            isValid = false
        }
        if (password.isEmpty()) {
            _state.update { it.copy(passwordError = "Password cannot be empty") }
            isValid = false
        }

        if (!isValid) return

        _state.update { it.copy(isLoading = true, loginError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch FCM token
                val fcmToken = preferencesManager.getFcmToken().first() ?: run {
                    Log.w("LoginViewModel", "FCM token not available")
                    null
                }

                val response = apiService.getUnAuthApi().signIn(
                    SignInRequest(
                        email, password,
                        fcmToken.toString()
                    )
                )
                if (response.isSuccessful && response.body() != null) {
                    val loginResponse = response.body()!!

                    // Save email if remember me is checked
                    if (_state.value.rememberMe) {
                        preferencesManager.saveLastLoginEmail(email)
                    } else {
                        preferencesManager.clearLastLoginEmail()
                    }

                    // Save tokens and user info
                    preferencesManager.saveEmail(email)
                    preferencesManager.saveAccessToken(email, loginResponse.accessToken)
                    preferencesManager.saveRefreshToken(email, loginResponse.refreshToken)
                    preferencesManager.saveIsLoggedIn(email, true)
                    preferencesManager.setCurrentUser(email)

                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoggedIn = true,
                            loginError = null
                        )
                    }
                } else {
                    val errorMessage = response.errorBody()?.string()?.let {
                        try {
                            val json = org.json.JSONObject(it)
                            json.getString("message")
                        } catch (e: Exception) {
                            "Login failed"
                        }
                    } ?: "Login failed"

                    _state.update {
                        it.copy(
                            isLoading = false,
                            loginError = errorMessage
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("LoginViewModel", "Login error: ${e.message}", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        loginError = "Login error: ${e.message}"
                    )
                }
            }
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        )
        return emailPattern.matcher(email).matches()
    }
}