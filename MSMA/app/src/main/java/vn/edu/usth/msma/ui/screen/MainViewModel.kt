//package vn.edu.usth.msma.ui.screen
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.content.IntentFilter
//import androidx.core.content.ContextCompat
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//import kotlinx.coroutines.withContext
//import vn.edu.usth.msma.service.MusicService
//import vn.edu.usth.msma.ui.screen.search.songs.Song
//
//class MainViewModel : ViewModel() {
//    private val _currentSong = MutableStateFlow<Song?>(null)
//    val currentSong = _currentSong.asStateFlow()
//
//    private val _isPlaying = MutableStateFlow(false)
//    val isPlaying = _isPlaying.asStateFlow()
//
//    private var musicEventReceiver: BroadcastReceiver? = null
//
//    fun updateCurrentSong(song: Song) {
//        _currentSong.value = song
//    }
//
//    fun updatePlaybackState(isPlaying: Boolean) {
//        _isPlaying.value = isPlaying
//    }
//
//    fun registerMusicEventReceiver(context: Context) {
//        if (musicEventReceiver != null) return
//
//        musicEventReceiver = object : BroadcastReceiver() {
//            override fun onReceive(context: Context, intent: Intent) {
//                if (intent.action == "MUSIC_EVENT") {
//                    val action = intent.getStringExtra("ACTION")
//
//                    when (action) {
//                        "PAUSED" -> _isPlaying.value = false
//                        "RESUMED" -> _isPlaying.value = true
//                        "LOADED" -> _isPlaying.value = true
//                        "COMPLETED" -> _isPlaying.value = false
//                        "NEXT", "PREVIOUS" -> {
//                            // Update song information when changed to next/previous
//                            val songId = intent.getStringExtra("SONG_ID") ?: return
//                            val songTitle = intent.getStringExtra("SONG_TITLE") ?: return
//                            val songArtist = intent.getStringExtra("SONG_ARTIST") ?: return
//                            val songImage = intent.getStringExtra("SONG_IMAGE") ?: return
//                            val songSource = intent.getStringExtra("SONG_SOURCE") ?: return
//                            val songAlbum = intent.getStringExtra("SONG_ALBUM") ?: return
//
//                            // Create updated Song object
//                            val updatedSong = Song(
//                                id = songId,
//                                title = songTitle,
//                                artist = songArtist,
//                                image = songImage,
//                                source = songSource,
//                                album = songAlbum,
//                                duration = _currentSong.value?.duration ?: 0,
//                                favorite = _currentSong.value?.favorite ?: false,
//                                counter = _currentSong.value?.counter ?: 0,
//                                replay = _currentSong.value?.replay ?: 0
//                            )
//
//                            // Update the current song
//                            _currentSong.value = updatedSong
//                            _isPlaying.value = true
//                        }
//                        "CURRENT_SONG" -> {
//                            val songId = intent.getStringExtra("SONG_ID")
//                            // Update with current song data
//                            if (songId != null) {
//                                val songTitle = intent.getStringExtra("SONG_TITLE") ?: ""
//                                val songArtist = intent.getStringExtra("SONG_ARTIST") ?: ""
//                                val songImage = intent.getStringExtra("SONG_IMAGE") ?: ""
//                                val isPlaying = intent.getBooleanExtra("IS_PLAYING", false)
//
//                                // Launch coroutine to fetch the full song object
//                                viewModelScope.launch(Dispatchers.IO) {
//                                    val songRepository = SongRepository()
//                                    val song = songRepository.getSongById(songId)
//                                    if (song != null) {
//                                        withContext(Dispatchers.Main) {
//                                            _currentSong.value = song
//                                            _isPlaying.value = isPlaying
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        val filter = IntentFilter("MUSIC_EVENT")
//        ContextCompat.registerReceiver(
//            context,
//            musicEventReceiver,
//            filter,
//            ContextCompat.RECEIVER_EXPORTED
//        )
//    }
//
//    fun unregisterMusicEventReceiver(context: Context) {
//        musicEventReceiver?.let {
//            context.unregisterReceiver(it)
//            musicEventReceiver = null
//        }
//    }
//
//    fun refreshCurrentSongData(context: Context) {
//        // Create an Intent to send a "GET_CURRENT_SONG" action to the service
//        val intent = Intent(context, MusicService::class.java).apply {
//            Intent.setAction = "GET_CURRENT_SONG"
//        }
//        context.startService(intent)
//        // The service will respond by broadcasting the current song info
//        // which will be picked up by the existing registerMusicEventReceiver
//    }
//
//
//
//    override fun onCleared() {
//        super.onCleared()
//        // Make sure we clean up resources
//    }
//}