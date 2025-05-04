package vn.edu.usth.msma.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.network.ApiService

data class SettingState(
    val isLoading: Boolean = false,
    val signOutError: String? = null,
    val isSignedOut: Boolean = false
)

class SettingViewModel(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(SettingState())
    val settingState: StateFlow<SettingState> = _state.asStateFlow()

    fun signOut() {
        _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAuthApi(preferencesManager.context).signOut()
                if (response.isSuccessful) {
                    preferencesManager.clearAll()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        signOutError = null,
                        isSignedOut = true
                    )
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        signOutError = "Sign-out failed: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    signOutError = "Sign-out error: ${e.message}"
                )
            }
        }
    }
}

class SettingViewModelFactory(
    private val preferencesManager: PreferencesManager,
    private val apiService: ApiService = ApiService
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SettingViewModel(preferencesManager, apiService) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}