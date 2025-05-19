package vn.edu.usth.msma.utils.eventbus

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object EventBus {
    private val _events = MutableSharedFlow<Event>()
    val events: SharedFlow<Event> = _events.asSharedFlow()

    suspend fun publish(event: Event) {
        _events.emit(event)
    }
}

sealed class Event {
    object ProfileUpdatedEvent : Event()
    object SessionExpiredEvent : Event()
    object InitializeDataLibrary: Event()
    object SongPlayingUpdateEvent: Event()
    object SongPauseUpdateEvent: Event()
    object SongFavouriteUpdateEvent : Event()
    object SongShuffleUpdateEvent: Event()
    object SongUnShuffleUpdateEvent: Event()
    object SongLoopUpdateEvent: Event()
    object SongUnLoopUpdateEvent: Event()
    object MediaNotificationCancelSongEvent: Event()
    object HistoryListenUpdateEvent: Event()
    object FollowArtistUpdateEvent: Event()
    object UnFollowArtistUpdateEvent: Event()
    object SavingPlaylistEvent: Event()
    object UnSavingPlaylistEvent: Event()
    object SavingAlbumEvent: Event()
    object UnSavingAlbumEvent: Event()
}