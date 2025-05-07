//package vn.edu.usth.msma.ui.screen.library
//
//import android.content.Context
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.StateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import vn.edu.usth.msma.repository.SongRepository
//import vn.edu.usth.msma.ui.screen.search.songs.Song
//
//class LibraryViewModel(private val repository: SongRepository) : ViewModel() {
//
//    private val _favoriteSongs = MutableStateFlow<List<Song>>(emptyList())
//    val favoriteSongs: StateFlow<List<Song>> = _favoriteSongs.asStateFlow()
//
//    private val _isLoading = MutableStateFlow(true)
//    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
//
//    fun loadFavoriteSongs(context: Context) {
//        viewModelScope.launch(Dispatchers.IO) {
//            _isLoading.value = true
//            val songs = repository.getFavoriteSongs(context)
//            _favoriteSongs.value = songs
//            _isLoading.value = false
//        }
//    }
//}
//
//class LibraryViewModelFactory(private val repository: SongRepository) : ViewModelProvider.Factory {
//    @Suppress("UNCHECKED_CAST")
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(LibraryViewModel::class.java)) {
//            return LibraryViewModel(repository) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}