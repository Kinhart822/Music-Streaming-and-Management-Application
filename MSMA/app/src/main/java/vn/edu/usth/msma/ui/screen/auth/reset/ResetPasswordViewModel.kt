package vn.edu.usth.msma.ui.screen.auth.reset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import vn.edu.usth.msma.ui.screen.auth.otp.OtpViewModel

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

class ResetPasswordViewModel : ViewModel() {
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

        // Simulate password reset (replace with actual API call)
        MainScope().launch {
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(1000)

                // Simulate successful password reset
                _resetPasswordState.update {
                    it.copy(
                        isLoading = false,
                        isReset = true,
                        error = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _resetPasswordState.update {
                    it.copy(
                        isLoading = false,
                        error = "Password reset failed: ${e.message}"
                    )
                }
            }
        }
    }
}

class ResetPasswordViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ResetPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ResetPasswordViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}