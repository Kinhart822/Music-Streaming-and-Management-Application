package vn.edu.usth.msma.ui.screen.auth.otp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

data class OtpState(
    val otp: String = "",
    val otpError: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isVerified: Boolean = false
)

class OtpViewModel() : ViewModel() {
    private val _otpState = MutableStateFlow(OtpState())
    val otpState: StateFlow<OtpState> = _otpState.asStateFlow()

//    init {
//        // Simulate sending OTP to email
//        println("OTP sent to $email")
//    }

    fun updateOtp(otp: String) {
        _otpState.update { it.copy(otp = otp, otpError = null) }
    }

    fun verifyOtp(onSuccess: () -> Unit) {
        _otpState.update { it.copy(isLoading = true) }

        MainScope().launch {
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(1000)

                _otpState.update {
                    it.copy(
                        isLoading = false,
                        isVerified = true,
                        error = null
                    )
                }
                onSuccess()
            } catch (e: Exception) {
                _otpState.update {
                    it.copy(
                        isLoading = false,
                        error = "Verification failed: ${e.message}"
                    )
                }
            }
        }
    }
}

class OtpViewModelFactory() : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(OtpViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return OtpViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}