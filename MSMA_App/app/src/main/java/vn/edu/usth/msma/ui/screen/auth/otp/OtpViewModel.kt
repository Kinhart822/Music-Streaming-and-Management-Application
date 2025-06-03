package vn.edu.usth.msma.ui.screen.auth.otp

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.data.dto.request.auth.CheckOtpRequest
import vn.edu.usth.msma.network.ApiService
import javax.inject.Inject

data class OtpState(
    val otp: String = "",
    val otpError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false,
    val otpDueDate: String? = null
)

@HiltViewModel
class OtpViewModel @Inject constructor(
    private val apiService: ApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val initialEmail: String = savedStateHandle.get<String>("email") ?: ""
    private val initialSessionId: String = savedStateHandle.get<String>("sessionId") ?: ""
    private val initialOtpDueDate: String = savedStateHandle.get<String>("otpDueDate") ?: ""

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