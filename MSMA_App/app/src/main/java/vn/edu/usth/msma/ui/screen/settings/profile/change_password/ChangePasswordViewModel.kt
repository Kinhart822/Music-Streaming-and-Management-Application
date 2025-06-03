package vn.edu.usth.msma.ui.screen.settings.profile.change_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.dto.request.profile.ChangePasswordRequest
import vn.edu.usth.msma.network.ApiService
import javax.inject.Inject

data class ChangePasswordState(
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
    val error: String? = null,
    val newPassword: String? = null,
    val newPasswordError: String? = null,
    val newPasswordVisible: Boolean = false,
    val confirmPassword: String? = null,
    val confirmPasswordError: String? = null,
    val confirmPasswordVisible: Boolean = false
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    fun onNewPasswordChanged(newPassword: String?) {
        _state.update {
            it.copy(
                newPassword = newPassword,
                newPasswordError = null,
                error = null
            )
        }
    }

    fun onConfirmPasswordChanged(confirmPassword: String?) {
        _state.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null,
                error = null
            )
        }
    }

    fun toggleNewPasswordVisibility() {
        _state.update {
            it.copy(newPasswordVisible = !it.newPasswordVisible)
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _state.update {
            it.copy(confirmPasswordVisible = !it.confirmPasswordVisible)
        }
    }

    fun changePassword() {
        // Validate inputs
        val newPassword = _state.value.newPassword
        val confirmPassword = _state.value.confirmPassword

        if (newPassword.isNullOrBlank()) {
            _state.update {
                it.copy(newPasswordError = "New password cannot be empty")
            }
            return
        }
        if (newPassword.length < 8) {
            _state.update {
                it.copy(newPasswordError = "Password must be at least 8 characters")
            }
            return
        }
        if (confirmPassword.isNullOrBlank()) {
            _state.update {
                it.copy(confirmPasswordError = "Confirm password cannot be empty")
            }
            return
        }
        if (newPassword != confirmPassword) {
            _state.update {
                it.copy(confirmPasswordError = "Passwords do not match")
            }
            return
        }

        _state.update {
            it.copy(
                isLoading = true,
                isSuccess = false,
                error = null,
                newPasswordError = null,
                confirmPasswordError = null
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val request = ChangePasswordRequest(
                    newPassword = newPassword,
                    confirmPassword = confirmPassword
                )
                val response = apiService.getAccountApi().updatePassword(request)

                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "200") {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                isSuccess = true,
                                error = null
                            )
                        }
                    } else {
                        _state.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to change password: ${apiResponse.message}"
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to change password: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error changing password: ${e.message}"
                    )
                }
            }
        }
    }
}