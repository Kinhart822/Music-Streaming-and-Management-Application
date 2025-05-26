package vn.edu.usth.msma.ui.screen.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.dto.response.management.NotificationResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.utils.eventbus.Event
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

data class NotificationState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val notifications: List<NotificationResponse> = emptyList(),
    val unreadCount: Int = 0
)

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(NotificationState())
    val state: StateFlow<NotificationState> = _state.asStateFlow()

    private var isInitialized = false

    init {
        if (!isInitialized) {
            fetchNotifications()
            isInitialized = true
        }

        viewModelScope.launch {
            EventBus.events.collect { event ->
                when (event) {
                    is Event.NotificationsUpdateEvent -> {
                        // Increment unreadCount when a new notification arrives
                        _state.update { it.copy(unreadCount = it.unreadCount + 1) }

                        fetchNotifications() // Fetch updated notifications
                    }
                    else -> {}
                }
            }
        }
    }

    fun fetchNotifications() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAccountApi().getAllUserNotifications()
                if (response.isSuccessful && response.body() != null) {
                    val notifications = response.body()!!
                    _state.update {
                        it.copy(
                            isLoading = false,
                            notifications = notifications,
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to fetch notifications: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error fetching notifications: ${e.message}"
                    )
                }
            }
        }
    }

    // New method to reset unread count when notifications are viewed
    fun resetUnreadCount() {
        _state.update { it.copy(unreadCount = 0) }
    }
}