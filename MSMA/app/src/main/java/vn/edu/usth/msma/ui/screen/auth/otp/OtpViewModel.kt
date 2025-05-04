package vn.edu.usth.msma.ui.screen.auth.otp

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
import vn.edu.usth.msma.data.dto.request.auth.CheckOtpRequest
import vn.edu.usth.msma.network.ApiService

data class OtpState(
    val otp: String = "",
    val otpError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false,
    val otpDueDate: String? = null
)

class OtpViewModel(
    private val apiService: ApiService,
    private val initialEmail: String,
    private val initialSessionId: String,
    initialOtpDueDate: String
) : ViewModel() {
    private val _otpState = MutableStateFlow(OtpState(otpDueDate = initialOtpDueDate))
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

    init {
        _otpState.update { it.copy(otp = "") }
    }

    fun updateOtp(otp: String) {
        _otpState.update { it.copy(otp = otp, otpError = null) }
    }

    fun verifyOtp(onSuccess: (String) -> Unit) {
        val currentState = _otpState.value

        // Validate OTP
        val otpError = when {
            currentState.otp.isBlank() -> "OTP cannot be empty"
            currentState.otp.length != 6 -> "OTP must be 6 digits"
            else -> null
        }

        if (otpError != null) {
            _otpState.update { it.copy(otpError = otpError) }
            return
        }

        _otpState.update { it.copy(isLoading = true) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = CheckOtpRequest(sessionId = initialSessionId, otp = currentState.otp)
                val response = apiService.getUnAccountApi().forgotPasswordCheckOtp(request)
                if (response.isSuccessful && response.body() != null) {
                    val otpCheckResult = response.body()!!
                    withContext(Dispatchers.Main) {
                        if (otpCheckResult.isValid) {
                            _otpState.update {
                                it.copy(
                                    isLoading = false,
                                    isVerified = true,
                                    error = null
                                )
                            }
                            onSuccess(initialSessionId)
                        } else {
                            _otpState.update {
                                it.copy(
                                    isLoading = false,
                                    otpError = "Invalid OTP",
                                    error = "Invalid OTP"
                                )
                            }
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _otpState.update {
                            it.copy(
                                isLoading = false,
                                otpError = "OTP verification failed: ${response.message()}",
                                error = "OTP verification failed: ${response.message()}"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _otpState.update {
                        it.copy(
                            isLoading = false,
                            otpError = "OTP verification failed: ${e.message}",
                            error = "OTP verification failed: ${e.message}"
                        )
                    }
                }
                Log.e("OtpViewModel", "Exception: ${e.message}")
            }
        }
    }
}

class OtpViewModelFactory(
    private val apiService: ApiService = ApiService,
    private val initialEmail: String,
    private val initialSessionId: String,
    private val initialOtpDueDate: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtpViewModel(apiService, initialEmail, initialSessionId, initialOtpDueDate) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}