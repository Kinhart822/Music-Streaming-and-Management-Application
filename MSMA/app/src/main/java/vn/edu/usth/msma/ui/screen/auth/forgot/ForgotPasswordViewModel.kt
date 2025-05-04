package vn.edu.usth.msma.ui.screen.auth.forgot

import android.util.Log
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import android.os.SystemClock
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.data.dto.request.auth.SendOtpRequest
import vn.edu.usth.msma.network.ApiService

data class ForgotPasswordState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSubmitted: Boolean = false,
    val sessionId: String = "",
    val otpDueDate: String? = null
)

class ForgotPasswordViewModel(
    private val apiService: ApiService
) : ViewModel() {
    private val _forgotPasswordState = MutableStateFlow(ForgotPasswordState())
    val forgotPasswordState: StateFlow<ForgotPasswordState> = _forgotPasswordState.asStateFlow()

    fun updateEmail(email: String) {
        _forgotPasswordState.update { it.copy(email = email, emailError = null) }
    }

    fun submitEmail(onSuccess: (String, String, String) -> Unit) {
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

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val sessionId = UUID.randomUUID().toString() + "-" + SystemClock.elapsedRealtime()
                val request = SendOtpRequest(sessionId = sessionId, email = currentState.email)
                val response = apiService.getUnAccountApi().forgotPasswordBegin(request)
                if (response.isSuccessful && response.body() != null) {
                    val otpDueDateResponse = response.body()!!
                    Log.d("ForgotPasswordViewModel", "API Success - otpDueDate: ${otpDueDateResponse.otpDueDate}")
                    withContext(Dispatchers.Main) {
                        _forgotPasswordState.update {
                            it.copy(
                                isLoading = false,
                                isSubmitted = true,
                                error = null,
                                sessionId = sessionId,
                                otpDueDate = otpDueDateResponse.otpDueDate
                            )
                        }
                        onSuccess(currentState.email, sessionId, otpDueDateResponse.otpDueDate)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _forgotPasswordState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to send OTP: ${response.message()}"
                            )
                        }
                    }
                    Log.e("ForgotPasswordViewModel", "API Failure: ${response.message()}")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _forgotPasswordState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to send OTP: ${e.message}"
                        )
                    }
                }
                Log.e("ForgotPasswordViewModel", "Exception: ${e.message}")
            }
        }
    }
}

class ForgotPasswordViewModelFactory(
    private val apiService: ApiService = ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ForgotPasswordViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ForgotPasswordViewModel(apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}