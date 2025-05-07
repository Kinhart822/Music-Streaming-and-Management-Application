package vn.edu.usth.msma.ui.screen.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import vn.edu.usth.msma.data.dto.response.management.ContentItem
import vn.edu.usth.msma.data.dto.response.management.ContentResponse
import vn.edu.usth.msma.data.dto.response.management.ContentItemDeserializer
import vn.edu.usth.msma.data.dto.response.management.ContentResponseDeserializer
import vn.edu.usth.msma.data.dto.response.management.GenreResponse
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import javax.inject.Inject

data class SearchState(
    val searchQuery: String = "",
    val genres: List<GenreResponse> = emptyList(),
    val contents: List<ContentItem> = emptyList(),
    val selectedTab: String = "All",
    val isLoadingGenres: Boolean = false,
    val isLoadingContents: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val apiService: ApiService,
    private val songRepository: SongRepository
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    init {
        fetchGenres()
        fetchContents(type = "all")
    }

    private fun fetchGenres() {
        _state.update { it.copy(isLoadingGenres = true, error = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getSearchApi().getAllGenres()
                if (response.isSuccessful && response.body() != null) {
                    _state.update {
                        it.copy(
                            genres = response.body()!!,
                            isLoadingGenres = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoadingGenres = false,
                            error = "Failed to load genres: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingGenres = false,
                        error = "Error loading genres: ${e.message}"
                    )
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _state.update {
            it.copy(
                searchQuery = query,
                selectedTab = "All",
                isLoadingContents = true,
                error = null
            )
        }
        if (query.isNotBlank()) {
            fetchContents(title = query, type = "all")
        } else {
            fetchContents(type = "all")
        }
    }

    fun selectTab(tab: String) {
        _state.update { it.copy(selectedTab = tab, isLoadingContents = true) }
        fetchContents(
            title = state.value.searchQuery.takeIf { it.isNotBlank() },
            type = when (tab) {
                "Songs" -> "Songs"
                "Playlists" -> "Playlists"
                "Albums" -> "Albums"
                "Artists" -> "Artists"
                else -> "all"
            }
        )
    }

    private fun fetchContents(title: String? = null, genreId: Long? = null, type: String = "all") {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val response = apiService.getSearchApi().getAllContents(
                    title = title?.takeIf { it.isNotBlank() },
                    genreId = genreId,
                    type = type.lowercase(),
                    limit = 10,
                    offset = 0
                )
                if (response.isSuccessful && response.body() != null) {
                    val gson = GsonBuilder()
                        .registerTypeAdapter(ContentResponse::class.java, ContentResponseDeserializer())
                        .registerTypeAdapter(ContentItem::class.java, ContentItemDeserializer())
                        .create()
                    val contentResponse = gson.fromJson(gson.toJson(response.body()), ContentResponse::class.java)

                    val songItems = contentResponse.content.filterIsInstance<ContentItem.SongItem>()
                    songRepository.updateSongs(songItems)

                    _state.update {
                        it.copy(
                            contents = contentResponse.content,
                            isLoadingContents = false
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isLoadingContents = false,
                            error = "Failed to load contents: ${response.message()}"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingContents = false,
                        error = "Error loading contents: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}