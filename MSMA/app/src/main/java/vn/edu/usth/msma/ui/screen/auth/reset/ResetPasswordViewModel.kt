package vn.edu.usth.msma.ui.screen.auth.reset

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.data.dto.request.auth.NewPasswordRequest
import vn.edu.usth.msma.network.ApiService

data class ResetPasswordState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isReset: Boolean = false,
    val newPasswordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false
)

class ResetPasswordViewModel(
    private val apiService: ApiService,
    private val sessionId: String
) : ViewModel() {
    private val _resetPasswordState = MutableStateFlow(ResetPasswordState())
    val resetPasswordState: StateFlow<ResetPasswordState> = _resetPasswordState.asStateFlow()

    fun updateNewPassword(newPassword: String) {
        _resetPasswordState.update { it.copy(newPassword = newPassword, newPasswordError = null) }
    }

    fun updateConfirmPassword(confirmPassword: String) {
        _resetPasswordState.update { it.copy(confirmPassword = confirmPassword, confirmPasswordError = null) }
    }

    fun toggleNewPasswordVisibility() {
        _resetPasswordState.update { it.copy(newPasswordVisible = !it.newPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _resetPasswordState.update { it.copy(confirmPasswordVisible = !it.confirmPasswordVisible) }
    }

    fun resetPassword(onSuccess: () -> Unit) {
        val currentState = _resetPasswordState.value

        // Validate new password
        val newPasswordError = when {
            currentState.newPassword.isBlank() -> "New password cannot be empty"
            currentState.newPassword.length < 6 -> "New password must be at least 6 characters"
            else -> null
        }

        // Validate confirm password
        val confirmPasswordError = when {
            currentState.confirmPassword.isBlank() -> "Confirm password cannot be empty"
            currentState.confirmPassword != currentState.newPassword -> "Passwords do not match"
            else -> null
        }

        if (newPasswordError != null || confirmPasswordError != null) {
            _resetPasswordState.update {
                it.copy(
                    newPasswordError = newPasswordError,
                    confirmPasswordError = confirmPasswordError
                )
            }
            return
        }

        _resetPasswordState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = NewPasswordRequest(sessionId = sessionId, password = currentState.newPassword)
                val response = apiService.getUnAccountApi().forgotPasswordFinish(request)
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    withContext(Dispatchers.Main) {
                        if (apiResponse.status == "200") {
                            _resetPasswordState.update {
                                it.copy(
                                    isLoading = false,
                                    isReset = true,
                                    error = null
                                )
                            }
                            onSuccess()
                        } else {
                            _resetPasswordState.update {
                                it.copy(
                                    isLoading = false,
                                    error = "Password reset failed: ${apiResponse.message}"
                                )
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _resetPasswordState.update {
                            it.copy(
                                isLoading = false,
                                error = "Password reset failed: ${response.message()}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _resetPasswordState.update {
                        it.copy(
                            isLoading = false,
                            error = "Password reset failed: ${e.message}"
                        )
                    }
                }
                Log.e("ResetPasswordViewModel", "Exception: ${e.message}")
            }
        }
    }
}

class ResetPasswordViewModelFactory(
    private val apiService: ApiService = ApiService,
    private val sessionId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResetPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResetPasswordViewModel(apiService, sessionId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}