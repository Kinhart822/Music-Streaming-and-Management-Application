package vn.edu.usth.msma.ui.screen.auth.forgot

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ForgotPasswordState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false
)

class ForgotPasswordViewModel : ViewModel() {
    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    fun updateEmail(email: String) {
        _forgotPasswordState.update { it.copy(email = email, emailError = null) }
    }

    fun submitEmail(onSuccess: (String) -> Unit) {
        val currentState = _forgotPasswordState.value

        // Validate email
        val emailError = when {
            currentState.email.isBlank() -> "Email cannot be empty"
            !Patterns.EMAIL_ADDRESS.matcher(currentState.email).matches() -> "Invalid email format"
            else -> null
        }

        if (emailError != null) {
            _forgotPasswordState.update { it.copy(emailError = emailError) }
            return
        }

        _forgotPasswordState.update { it.copy(isLoading = true) }

        // Simulate sending OTP (replace with actual API call)
        MainScope().launch {
            try {
                // Simulate network delay
                delay(1000)

                // Simulate email check (e.g., email exists in system)
                if (currentState.email == "test@example.com") {
                    _forgotPasswordState.update {
                        it.copy(
                            isLoading = false,
                            isSubmitted = true,
                            error = null
                        )
                    }
                    onSuccess(currentState.email)
                } else {
                    _forgotPasswordState.update {
                        it.copy(
                            isLoading = false,
                            error = "Email not found"
                        )
                    }
                }
            } catch (e: Exception) {
                _forgotPasswordState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to send OTP: ${e.message}"
                    )
                }
            }
        }
    }
}

class ForgotPasswordViewModelFactory : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForgotPasswordViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}