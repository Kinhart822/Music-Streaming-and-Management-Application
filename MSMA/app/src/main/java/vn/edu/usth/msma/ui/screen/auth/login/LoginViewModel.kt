package vn.edu.usth.msma.ui.screen.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.request.auth.SignInRequest
import vn.edu.usth.msma.network.ApiService
import java.util.regex.Pattern

data class LoginState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginError: String? = null,
    val isLoggedIn: Boolean = false,
    val passwordVisible: Boolean = false
)

class LoginViewModel(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(LoginState())
    val loginState: StateFlow<LoginState> = _state.asStateFlow()

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email, emailError = null)
    }

    fun updatePassword(password: String) {
        _state.value = _state.value.copy(password = password, passwordError = null)
    }

    fun togglePasswordVisibility() {
        _state.value = _state.value.copy(passwordVisible = !_state.value.passwordVisible)
    }

    fun login() {
        val email = _state.value.email
        val password = _state.value.password
        var isValid = true

        if (!isValidEmail(email)) {
            _state.value = _state.value.copy(emailError = "Invalid email format")
            isValid = false
        }
        if (password.isEmpty()) {
            _state.value = _state.value.copy(passwordError = "Password cannot be empty")
            isValid = false
        }

        if (!isValid) return

        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getUnAuthApi().signIn(SignInRequest(email, password))
                if (response.isSuccessful && response.body() != null) {
                    val signInResponse = response.body()!!
                    preferencesManager.saveEmail(email)
                    preferencesManager.saveAccessToken(signInResponse.accessToken)
                    preferencesManager.saveRefreshToken(signInResponse.refreshToken)
                    preferencesManager.saveIsLoggedIn(true)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        isLoggedIn = true,
                        loginError = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        loginError = "Login failed: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    loginError = "Login failed: ${e.message}"
                )
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

class LoginViewModelFactory(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService = ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(preferencesManager, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}