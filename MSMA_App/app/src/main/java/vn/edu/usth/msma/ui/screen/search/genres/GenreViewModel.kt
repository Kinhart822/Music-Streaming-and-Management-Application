package vn.edu.usth.msma.ui.screen.search.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.data.dto.response.management.SongResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import javax.inject.Inject

data class GenreState(
    val genre: GenreResponse,
    val songs: List<SongResponse> = emptyList(),
    val isLoadingSongs: Boolean = false,
    val isFullDescriptionVisible: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val apiService: ApiService,
    val songRepository: SongRepository
) : ViewModel() {
    private val _state = MutableStateFlow(GenreState(GenreResponse(
        id = 0,
        name = "",
        briefDescription = "",
        fullDescription = "",
        imageUrl = "",
        createdDate = "",
        lastModifiedDate = ""
    )))
    val state: StateFlow<GenreState> = _state.asStateFlow()

    fun init(genre: GenreResponse) {
        _state.update { it.copy(genre = genre) }
        fetchSongsByGenre(genre.id)
    }

    fun toggleFullDescription() {
        _state.update { it.copy(isFullDescriptionVisible = !it.isFullDescriptionVisible) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun fetchSongsByGenre(genreId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.update { it.copy(isLoadingSongs = true) }
                val response = apiService.getSearchApi().getSongsByGenre(genreId)
                if (response.isSuccessful && response.body() != null) {
                    val songs = response.body()!!
                    songRepository.updateSongResponseList(songs)
                    _state.update {
                        it.copy(
                            songs = songs,
                            isLoadingSongs = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            error = "Failed to load songs",
                            isLoadingSongs = false
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        error = e.message ?: "An error occurred",
                        isLoadingSongs = false
                    )
                }
            }
        }
    }
}