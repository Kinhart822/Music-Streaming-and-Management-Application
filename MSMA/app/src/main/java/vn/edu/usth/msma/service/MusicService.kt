package vn.edu.usth.msma.service

import android.annotation.SuppressLint
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_LOW
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import coil.Coil
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.MainActivity
import vn.edu.usth.msma.R
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.network.ApiService
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.utils.eventbus.Event.*
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service() {
    @Inject
    lateinit var songRepository: SongRepository

    @Inject
    lateinit var apiService: ApiService

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "music_channel"
    private val ACTION_PLAY = "vn.edu.usth.msma.ACTION_PLAY"
    private val ACTION_PAUSE = "vn.edu.usth.msma.ACTION_PAUSE"
    private val ACTION_PREVIOUS = "vn.edu.usth.msma.ACTION_PREVIOUS"
    private val ACTION_NEXT = "vn.edu.usth.msma.ACTION_NEXT"
    private val ACTION_CLOSE = "vn.edu.usth.msma.ACTION_CLOSE"

    private var mediaSession: MediaSessionCompat? = null
    private val binder = MusicBinder()

    private var currentSongTitle: String? = null
    private var currentSongArtist: String? = null
    private var currentSongImage: String? = null
    private var currentSongId: Long? = null

    private var isPlaying = false
    private var isLoopEnabled = false
    private var isShuffleEnabled = false

    private var exoPlayer: ExoPlayer? = null

    inner class MusicBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "MusicHubSession")
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_PLAY)
            addAction(ACTION_PAUSE)
            addAction(ACTION_PREVIOUS)
            addAction(ACTION_NEXT)
            addAction(ACTION_CLOSE)
        }
        registerReceiver(notificationActionReceiver, intentFilter)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PLAY" -> {
                val songPath = intent.getStringExtra("SONG_PATH")
                currentSongId = intent.getLongExtra("SONG_ID", 0L)
                currentSongTitle = intent.getStringExtra("SONG_TITLE")
                currentSongArtist = intent.getStringExtra("SONG_ARTIST")
                currentSongImage = intent.getStringExtra("SONG_IMAGE")

                songPath?.let { playSong(it) }
            }

            "PAUSE", ACTION_PAUSE -> pauseSong()
            "RESUME", ACTION_PLAY -> resumeSong()
            "SEEK_POSITION" -> {
                val position = intent.getLongExtra("POSITION", 0L)
                exoPlayer?.seekTo(position)
                if (!isPlaying) {
                    isPlaying = true
                    exoPlayer?.playWhenReady = true
                }
                updateNotification()
                broadcastEvent("POSITION_UPDATE")
                // Broadcast the current position to all components
                val seekIntent = Intent("MUSIC_EVENT").apply {
                    putExtra("ACTION", "POSITION_UPDATE")
                    putExtra("POSITION", position)
                    putExtra("DURATION", exoPlayer?.duration ?: 0L)
                }
                sendBroadcast(seekIntent)
            }

            "NEXT", ACTION_NEXT -> playNextSong()
            "PREVIOUS", ACTION_PREVIOUS -> playPreviousSong()
            "CLOSE", ACTION_CLOSE -> closeService()
            "LOOP_ON" -> {
                isLoopEnabled = true
                broadcastEvent("LOOP_ON")
                exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.publish(SongLoopUpdateEvent)
                }
            }

            "LOOP_OFF" -> {
                isLoopEnabled = false
                broadcastEvent("LOOP_OFF")
                exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.publish(SongUnLoopUpdateEvent)
                }
            }

            "SHUFFLE_ON" -> {
                isShuffleEnabled = true
                broadcastEvent("SHUFFLE_ON")
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.publish(SongShuffleUpdateEvent)
                }
            }

            "SHUFFLE_OFF" -> {
                isShuffleEnabled = false
                broadcastEvent("SHUFFLE_OFF")
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.publish(SongUnShuffleUpdateEvent)
                }
            }

            "DOWNLOAD_SONG" -> {
                val songId = intent.getLongExtra("SONG_ID", 0L)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = apiService.getSongApi().userDownloadSong(songId)
                        if (response.isSuccessful) {
                            val broadcastIntent = Intent("MUSIC_EVENT").apply {
                                putExtra("ACTION", "DOWNLOAD_COMPLETE")
                                putExtra("SONG_ID", songId)
                                putExtra("IS_DOWNLOADED", true)
                            }
                            sendBroadcast(broadcastIntent)
                        } else {
                            Log.e("MusicService", "Failed to download song: ${response.code()}")
                        }
                    } catch (e: Exception) {
                        Log.e("MusicService", "Error downloading song", e)
                    }
                }
            }

            "ADD_TO_FAVORITES" -> {
                val songId = intent.getLongExtra("SONG_ID", 0L)
                if (songId != 0L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = apiService.getSongApi().userLikeSong(songId)
                            if (response.isSuccessful) {
                                preferencesManager.addToFavorites(songId)
                                broadcastEvent("ADDED_TO_FAVORITES")
                            } else {
                                Log.e("MusicService", "Failed to like song: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error adding to favorites", e)
                        }
                    }
                }
            }

            "REMOVE_FROM_FAVORITES" -> {
                val songId = intent.getLongExtra("SONG_ID", 0L)
                if (songId != 0L) {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val response = apiService.getSongApi().userUnlikeSong(songId)
                            if (response.isSuccessful) {
                                preferencesManager.removeFromFavorites(songId)
                                broadcastEvent("REMOVED_FROM_FAVORITES")
                            } else {
                                Log.e("MusicService", "Failed to unlike song: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error removing from favorites", e)
                        }
                    }
                }
            }

            "GET_CURRENT_SONG" -> {
                if (currentSongId != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val isFavorite = try {
                            val likedSongs = apiService.getSongApi().getLikedSongs()
                            likedSongs.isSuccessful && likedSongs.body()
                                ?.any { it.id == currentSongId } == true
                        } catch (e: Exception) {
                            Log.e("MusicService", "Error checking favorite status", e)
                            false
                        }
                        val broadcastIntent = Intent("MUSIC_EVENT").apply {
                            putExtra("ACTION", "CURRENT_SONG")
                            putExtra("SONG_ID", currentSongId)
                            putExtra("SONG_TITLE", currentSongTitle)
                            putExtra("SONG_ARTIST", currentSongArtist)
                            putExtra("SONG_IMAGE", currentSongImage)
                            putExtra("IS_PLAYING", isPlaying)
                            putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                            putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                            putExtra("IS_FAVORITE", isFavorite)
                        }
                        sendBroadcast(broadcastIntent)
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun playNextSong() {
        if (isLoopEnabled) {
            isLoopEnabled = false
            exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
            broadcastEvent("LOOP_OFF")
        }
        CoroutineScope(Dispatchers.IO).launch {
            val nextSong = when {
                isShuffleEnabled -> {
                    Log.d("MusicService", "Playing random song due to shuffle being enabled")
                    songRepository.playRandomSong()
                }

                isLoopEnabled -> {
                    Log.d("MusicService", "Repeating current song due to loop being enabled")
                    currentSongId?.let { songRepository.getSongById(it) }
                }

                else -> songRepository.getNextSong(currentSongId)
            }
            nextSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.mp3Url ?: return@withContext)
                    Log.d("nextSong", "🎵 Bài hát tiếp theo: ${it.title}")

                    currentSongTitle = it.title
                    currentSongArtist = it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                    currentSongImage = it.imageUrl
                    currentSongId = it.id

                    val intent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "NEXT")
                        putExtra("SONG_ID", it.id)
                        putExtra("SONG_TITLE", it.title)
                        putExtra(
                            "SONG_ARTIST",
                            it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", it.imageUrl)
                        putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                        putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                        putExtra("IS_PLAYING", true)
                        putExtra("POSITION", 0L)
                        putExtra("DURATION", exoPlayer?.duration ?: 0L)
                    }
                    sendBroadcast(intent)
                    updateNotification()
                }
            } ?: run {
                Log.e("MusicService", "No next song available")
                pauseSong()
                broadcastEvent("COMPLETED")
            }
        }
    }

    private fun playPreviousSong() {
        if (isLoopEnabled) {
            isLoopEnabled = false
            exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
            broadcastEvent("LOOP_OFF")
        }
        CoroutineScope(Dispatchers.IO).launch {
            val previousSong = when {
                isShuffleEnabled -> {
                    Log.d("MusicService", "Playing random song due to shuffle being enabled")
                    songRepository.playRandomSong()
                }

                isLoopEnabled -> {
                    Log.d("MusicService", "Repeating current song due to loop being enabled")
                    currentSongId?.let { songRepository.getSongById(it) }
                }

                else -> songRepository.getPreviousSong(currentSongId)
            }
            previousSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.mp3Url ?: return@withContext)
                    Log.d("previousSong", "🎵 Bài hát trước: ${it.title}")

                    currentSongTitle = it.title
                    currentSongArtist = it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                    currentSongImage = it.imageUrl
                    currentSongId = it.id

                    val intent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "PREVIOUS")
                        putExtra("SONG_ID", it.id)
                        putExtra("SONG_TITLE", it.title)
                        putExtra(
                            "SONG_ARTIST",
                            it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                        )
                        putExtra("SONG_IMAGE", it.imageUrl)
                        putExtra("IS_LOOP_ENABLED", isLoopEnabled)
                        putExtra("IS_SHUFFLE_ENABLED", isShuffleEnabled)
                        putExtra("IS_PLAYING", true)
                        putExtra("POSITION", 0L)
                        putExtra("DURATION", exoPlayer?.duration ?: 0L)
                    }
                    sendBroadcast(intent)
                    updateNotification()
                }
            }
        }
    }

    private fun closeService() {
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.setMiniPlayerVisible(false)
        }
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        stopForeground(true)
        stopSelf()
    }

    private fun broadcastEvent(action: String) {
        val intent = Intent("MUSIC_EVENT").apply {
            putExtra("ACTION", action)
        }
        sendBroadcast(intent)
    }

    private fun playSong(path: String) {
        // Release existing player if different song or not playing
        exoPlayer?.release()
        broadcastEvent("LOADING")
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(path)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true

            repeatMode = if (isLoopEnabled) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            this@MusicService.isPlaying = true
                            startPositionUpdates()
                            updateNotification()
                            broadcastEvent("LOADED")
                            CoroutineScope(Dispatchers.IO).launch {
                                EventBus.publish(SongPlayingUpdateEvent)
                                // Record song listen history
                                try {
                                    currentSongId?.let { id ->
                                        val response = apiService.getSongApi().recordSongListen(id)
                                        if (response.isSuccessful) {
                                            EventBus.publish(HistoryListenUpdateEvent)
                                        }
                                    }
                                } catch (e: Exception) {
                                    Log.e("MusicService", "Error recording song listen", e)
                                }
                            }
                        }

                        Player.STATE_ENDED -> {
                            if (isLoopEnabled) {
                                exoPlayer?.seekTo(0)
                                exoPlayer?.playWhenReady = true
                            } else {
                                playNextSong()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun pauseSong() {
        exoPlayer?.let { player ->
            player.playWhenReady = false
            isPlaying = false
            updateNotification()
            broadcastEvent("PAUSED")
            CoroutineScope(Dispatchers.IO).launch {
                EventBus.publish(SongPauseUpdateEvent)
            }
        }
    }

    private fun resumeSong() {
        exoPlayer?.let { player ->
            if (!player.isPlaying) {
                player.playWhenReady = true
                isPlaying = true
                startPositionUpdates()
                updateNotification()
                broadcastEvent("RESUMED")
                CoroutineScope(Dispatchers.IO).launch {
                    EventBus.publish(SongPlayingUpdateEvent)
                }
            }
        }
    }

    private var isUpdating = false

    private fun startPositionUpdates() {
        if (isUpdating) return
        isUpdating = true
        CoroutineScope(Dispatchers.IO).launch {
            while (isUpdating && exoPlayer != null) {
                try {
                    val position: Long
                    val playerDuration: Long

                    withContext(Dispatchers.Main) { // Chuyển về Main Thread để lấy dữ liệu
                        position = exoPlayer?.currentPosition ?: 0L
                        playerDuration = exoPlayer?.duration ?: 0L
                    }

                    val intent = Intent("POSITION_UPDATE").apply {
                        putExtra("POSITION", position)
                        putExtra("DURATION", playerDuration)
                    }
                    sendBroadcast(intent)

                    delay(500)
                } catch (e: Exception) {
                    Log.e("MusicService", "Error: ${e.message}")
                }
            }
        }
    }

    private fun stopPositionUpdates() {
        isUpdating = false
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Music Player"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = "Music player controls"
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                var art: Bitmap? = null
                currentSongImage?.let { imageUrl ->
                    art = loadSongImage(imageUrl)
                }
                val notification = createNotification(art)
                startForeground(NOTIFICATION_ID, notification)
                if (!isPlaying) {
                    stopForeground(STOP_FOREGROUND_DETACH)
                    NotificationManagerCompat.from(this@MusicService)
                        .notify(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                Log.e("MusicService", "Error updating notification: ${e.message}")
            }
        }
    }

    private suspend fun loadSongImage(imageUrl: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val request = ImageRequest.Builder(this@MusicService)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = Coil.imageLoader(this@MusicService).execute(request)
            return@withContext result.drawable?.toBitmap()
        } catch (e: Exception) {
            Log.e("MusicService", "Error loading album art: ${e.message}")
            null
        }
    }

    private fun createNotification(songArt: Bitmap?): Notification {
        mediaSession ?: run {
            Log.e("MusicService", "MediaSession is null, cannot create notification")
            return NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.logo)
                .setContentTitle(currentSongTitle ?: "Unknown title")
                .setContentText(currentSongArtist ?: "Unknown artist")
                .setLargeIcon(songArt)
                .setPriority(PRIORITY_LOW)
                .setOngoing(isPlaying)
                .build()
        }
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FROM_NOTIFICATION", true)
            putExtra("SONG_ID", currentSongId)
            putExtra("SONG_TITLE", currentSongTitle)
            putExtra("SONG_ARTIST", currentSongArtist)
            putExtra("SONG_IMAGE", currentSongImage)
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Pause",
                createPendingIntent(ACTION_PAUSE)
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Play",
                createPendingIntent(ACTION_PLAY)
            )
        }
        val previousAction = NotificationCompat.Action(
            R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Previous",
            createPendingIntent(ACTION_PREVIOUS)
        )
        val nextAction = NotificationCompat.Action(
            R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Next",
            createPendingIntent(ACTION_NEXT)
        )
        val closeAction = NotificationCompat.Action(
            R.drawable.close48,
            "Close",
            createPendingIntent(ACTION_CLOSE)
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(currentSongTitle ?: "Unknown title")
            .setContentText(currentSongArtist ?: "Unknown artist")
            .setLargeIcon(songArt)
            .setContentIntent(pendingContentIntent)
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setShowActionsInCompactView(0, 1, 2)
                    .setMediaSession(mediaSession!!.sessionToken)
            )
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(closeAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(PRIORITY_LOW)
            .setOngoing(isPlaying)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action)
        intent.setPackage(packageName)
        return PendingIntent.getBroadcast(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private val notificationActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_PLAY -> resumeSong()
                ACTION_PAUSE -> pauseSong()
                ACTION_PREVIOUS -> playPreviousSong()
                ACTION_NEXT -> playNextSong()
                ACTION_CLOSE -> {
                    CoroutineScope(Dispatchers.IO).launch {
                        EventBus.publish(MediaNotificationCancelSongEvent)
                    }
                    closeService()
                }
            }
        }
    }

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    override fun onDestroy() {
        super.onDestroy()
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        try {
            unregisterReceiver(notificationActionReceiver)
        } catch (e: Exception) {
            Log.e("MusicService", "Error unregistering receiver", e)
        }

    }
}