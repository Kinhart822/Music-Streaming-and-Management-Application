package vn.edu.usth.msma.ui.screen.auth.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.dto.request.auth.SignUpRequest
import vn.edu.usth.msma.network.ApiService
import javax.inject.Inject

data class RegisterState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val registerError: String? = null,
    val isRegistered: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _registerState = MutableStateFlow(RegisterState())
    val registerState: StateFlow<RegisterState> = _registerState.asStateFlow()

    fun updateEmail(email: String) {
        _registerState.update { it.copy(email = email, emailError = null) }
    }

    fun updatePassword(password: String) {
        _registerState.update { it.copy(password = password, passwordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _registerState.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _registerState.update { it.copy(passwordVisible = !it.passwordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _registerState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun register() {
        val currentState = _registerState.value

        // Validate email
        val emailError = when {
            currentState.email.isBlank() -> "Email cannot be empty"
            !android.util.Patterns.EMAIL_ADDRESS.matcher(currentState.email)
                .matches() -> "Invalid email format"
            else -> null
        }

        // Validate password
        val passwordError = when {
            currentState.password.isBlank() -> "Password cannot be empty"
            currentState.password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }

        // Validate confirm password
        val confirmPasswordError = when {
            currentState.confirmPassword.isBlank() -> "Confirm password cannot be empty"
            currentState.confirmPassword != currentState.password -> "Passwords do not match"
            else -> null
        }

        if (emailError != null || passwordError != null || confirmPasswordError != null) {
            _registerState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        _registerState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Step 1: Check if email exists
                val emailCheckResponse =
                    apiService.getUnAccountApi().checkEmailExistence(currentState.email)
                if (!emailCheckResponse.isSuccessful || emailCheckResponse.body() == null) {
                    _registerState.update {
                        it.copy(
                            isLoading = false,
                            registerError = "Email check failed: ${emailCheckResponse.message()}"
                        )
                    }
                    return@launch
                }

                val emailExistence = emailCheckResponse.body()!!
                if (emailExistence.emailExisted) {
                    _registerState.update {
                        it.copy(
                            isLoading = false,
                            emailError = "Email already exists",
                            registerError = "Email already exists"
                        )
                    }
                    return@launch
                }

                // Step 2: Proceed with registration
                val signUpRequest = SignUpRequest(
                    email = currentState.email,
                    password = currentState.password
                )
                val signUpResponse = apiService.getUnAccountApi().signUpFinish(signUpRequest)
                if (signUpResponse.isSuccessful && signUpResponse.body() != null) {
                    val apiResponse = signUpResponse.body()!!
                    if (apiResponse.status == "200") {
                        _registerState.update {
                            it.copy(
                                isLoading = false,
                                isRegistered = true,
                                registerError = null
                            )
                        }
                    } else {
                        _registerState.update {
                            it.copy(
                                isLoading = false,
                                registerError = "Registration failed: ${apiResponse.message}"
                            )
                        }
                    }
                } else {
                    _registerState.update {
                        it.copy(
                            isLoading = false,
                            registerError = "Registration failed: ${signUpResponse.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _registerState.update {
                    it.copy(
                        isLoading = false,
                        registerError = "Registration failed: ${e.message}"
                    )
                }
            }
        }
    }
}