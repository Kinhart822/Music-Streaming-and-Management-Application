package vn.edu.usth.msma.ui.screen.settings.history_listen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.dto.request.management.HistoryListenResponse
import vn.edu.usth.msma.network.ApiService
import javax.inject.Inject

data class ViewHistoryListenState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val history: List<HistoryListenResponse> = emptyList()
)

@HiltViewModel
class ViewHistoryListenViewModel @Inject constructor(
    private val apiService: ApiService
) : ViewModel() {
    private val _state = MutableStateFlow(ViewHistoryListenState())
    val state: StateFlow<ViewHistoryListenState> = _state.asStateFlow()

    init {
        fetchHistoryListen()
    }

    private fun fetchHistoryListen() {
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getAccountApi().viewHistoryListen()
                if (response.isSuccessful && response.body() != null) {
                    val history = response.body()!!
                    _state.update {
                        it.copy(
                            isLoading = false,
                            history = history,
                            error = null
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to fetch history: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error fetching history: ${e.message}"
                    )
                }
            }
        }
    }
}