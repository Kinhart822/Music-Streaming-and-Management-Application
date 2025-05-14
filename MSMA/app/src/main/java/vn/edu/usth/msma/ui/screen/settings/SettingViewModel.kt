package vn.edu.usth.msma.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.utils.constants.UserType
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.Event.ProfileUpdatedEvent
import vn.edu.usth.msma.utils.eventbus.Event.SessionExpiredEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

data class SettingState(
    val isDeleteLoading: Boolean = false,
    val isSignOutLoading: Boolean = false,
    val signOutError: String? = null,
    val isSignedOut: Boolean = false,
    val deleteAccountError: String? = null,
    val isAccountDeleted: Boolean = false,
    val id: Long? = null,
    val avatar: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val userName: String? = null,
    val email: String? = null,
    val gender: String? = null,
    val birthDay: String? = null,
    val phone: String? = null,
    val status: Int? = null,
    val createdBy: Long? = null,
    val lastModifiedBy: Long? = null,
    val createdDate: String? = null,
    val lastModifiedDate: String? = null,
    val userType: UserType? = null
)

@HiltViewModel
class SettingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(SettingState())
    val settingState: StateFlow<SettingState> = _state.asStateFlow()

    private var isInitialized = false // Biến cờ để kiểm soát khởi tạo

    init {
        // Chỉ gọi fetchUserDetails lần đầu nếu chưa khởi tạo
        if (!isInitialized) {
            fetchUserDetails()
            isInitialized = true
        }

        // Lắng nghe sự kiện từ EventBus
        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is ProfileUpdatedEvent -> {
                        fetchUserDetails() // Gọi lại khi có sự kiện ProfileUpdatedEvent
                    }
                    else -> {}
                }
            }
        }
    }

    private fun fetchUserDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val email = preferencesManager.currentUserEmailFlow.first() ?: return@launch
                val response = apiService.getAccountApi().getUserDetailByUser()
                if (response.isSuccessful && response.body() != null) {
                    val user = response.body()!!
                    _state.update {
                        it.copy(
                            id = user.id,
                            avatar = user.avatar,
                            firstName = user.firstName,
                            lastName = user.lastName,
                            email = user.email,
                            gender = user.gender,
                            birthDay = user.birthDay,
                            phone = user.phone,
                            status = user.status,
                            createdBy = user.createdBy,
                            lastModifiedBy = user.lastModifiedBy,
                            createdDate = user.createdDate,
                            lastModifiedDate = user.lastModifiedDate,
                            userType = user.userType
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            userName = "user",
                            email = "No email"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        userName = "user",
                        email = "No email"
                    )
                }
            }
        }
    }

    fun signOut() {
        _state.update { it.copy(isSignOutLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAuthApi().signOut()
                if (response.isSuccessful) {
                    preferencesManager.logout()
                    _state.update {
                        it.copy(
                            isSignOutLoading = false,
                            signOutError = null,
                            isSignedOut = true
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isSignOutLoading = false,
                            signOutError = "Sign-out failed: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSignOutLoading = false,
                        signOutError = "Sign-out error: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteAccount() {
        _state.update { it.copy(isDeleteLoading = true) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAccountApi().deleteAccount()
                if (response.isSuccessful && response.body() != null) {
                    val apiResponse = response.body()!!
                    if (apiResponse.status == "200") {
                        preferencesManager.delete()
                        _state.update {
                            it.copy(
                                isDeleteLoading = false,
                                deleteAccountError = null,
                                isAccountDeleted = true
                            )
                        }
                        // Fetch user details again to update the UI
                        fetchUserDetails()
                    } else {
                        _state.update {
                            it.copy(
                                isDeleteLoading = false,
                                deleteAccountError = "Delete account failed: ${apiResponse.message}"
                            )
                        }
                    }
                } else {
                    _state.update {
                        it.copy(
                            isDeleteLoading = false,
                            deleteAccountError = "Delete account failed: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isDeleteLoading = false,
                        deleteAccountError = "Delete account error: ${e.message}"
                    )
                }
            }
        }
    }
}