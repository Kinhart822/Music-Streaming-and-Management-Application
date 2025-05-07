package vn.edu.usth.msma.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import coil.Coil
import coil.request.ImageRequest
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.edu.usth.msma.MainActivity
import vn.edu.usth.msma.R
import vn.edu.usth.msma.repository.SongRepository
import vn.edu.usth.msma.ui.screen.search.songs.Song
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : Service() {
    private val NOTIFICATION_ID = 1
    private val CHANNEL_ID = "music_channel"
    private val binder = LocalBinder()

    @Inject lateinit var songRepository: SongRepository

    private var exoPlayer: ExoPlayer? = null
    private var currentSongTitle: String? = null
    private var currentSongArtist: String? = null
    private var currentSongImage: String? = null
    private var currentSongId: Long? = null
    private var isPlaying = false
    private var isLoopEnabled = false
    private var isShuffleEnabled = false

    inner class LocalBinder : Binder() {
        fun getService(): MusicService = this@MusicService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        // Register broadcast receiver for notification actions
        val intentFilter = IntentFilter().apply {
            addAction("PLAY")
            addAction("PAUSE")
            addAction("PREVIOUS")
            addAction("NEXT")
            addAction("CLOSE")
        }
        registerReceiver(notificationActionReceiver, intentFilter, RECEIVER_NOT_EXPORTED)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!::songRepository.isInitialized) {
            Log.e("MusicService", "SongRepository not initialized")
            return START_NOT_STICKY
        }

        when (intent?.action) {
            "PLAY" -> {
                currentSongId = intent.getLongExtra("SONG_ID", 0L)
                currentSongTitle = intent.getStringExtra("SONG_TITLE")
                currentSongArtist = intent.getStringExtra("SONG_ARTIST")
                currentSongImage = intent.getStringExtra("SONG_IMAGE")
                val songPath = intent.getStringExtra("SONG_PATH")
                songPath?.let { playSong(it) }
            }
            "PAUSE" -> pauseSong()
            "RESUME" -> resumeSong()
            "SEEK" -> {
                val position = intent.getLongExtra("POSITION", 0L)
                exoPlayer?.seekTo(position)
                updateNotification()
            }
            "NEXT" -> playNextSong()
            "PREVIOUS" -> playPreviousSong()
            "LOOP_ON" -> {
                isLoopEnabled = true
                exoPlayer?.repeatMode = Player.REPEAT_MODE_ONE
                broadcastEvent("LOOP_ON")
            }
            "LOOP_OFF" -> {
                isLoopEnabled = false
                exoPlayer?.repeatMode = Player.REPEAT_MODE_OFF
                broadcastEvent("LOOP_OFF")
            }
            "SHUFFLE_ON" -> {
                isShuffleEnabled = true
                broadcastEvent("SHUFFLE_ON")
            }
            "SHUFFLE_OFF" -> {
                isShuffleEnabled = false
                broadcastEvent("SHUFFLE_OFF")
            }
            "DOWNLOAD_SONG" -> {
                val songId = intent.getLongExtra("SONG_ID", 0L)
                CoroutineScope(Dispatchers.IO).launch {
                    val song = songRepository.getSongById(songId)
                    song?.let { downloadSong(it) }
                }
            }
            "GET_CURRENT_SONG" -> {
                if (currentSongId != null) {
                    val broadcastIntent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "CURRENT_SONG")
                        putExtra("SONG_ID", currentSongId)
                        putExtra("SONG_TITLE", currentSongTitle)
                        putExtra("SONG_ARTIST", currentSongArtist)
                        putExtra("SONG_IMAGE", currentSongImage)
                        putExtra("IS_PLAYING", isPlaying)
                    }
                    sendBroadcast(broadcastIntent)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun playNextSong() {
        CoroutineScope(Dispatchers.IO).launch {
            val nextSong = when {
                isShuffleEnabled -> songRepository.playRandomSong()
                isLoopEnabled -> currentSongId?.let { songRepository.getSongById(it) }
                else -> songRepository.getNextSong(currentSongId)
            }
            nextSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.mp3Url ?: return@withContext)
                    currentSongTitle = it.title
                    currentSongArtist = it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                    currentSongImage = it.imageUrl
                    currentSongId = it.id
                    val intent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "NEXT")
                        putExtra("SONG_ID", it.id)
                        putExtra("SONG_TITLE", it.title)
                        putExtra("SONG_ARTIST", it.artistNameList?.joinToString(", ") ?: "Unknown Artist")
                        putExtra("SONG_IMAGE", it.imageUrl)
                    }
                    sendBroadcast(intent)
                    updateNotification()
                }
            }
        }
    }

    private fun playPreviousSong() {
        CoroutineScope(Dispatchers.IO).launch {
            val previousSong = when {
                isShuffleEnabled -> songRepository.playRandomSong()
                isLoopEnabled -> currentSongId?.let { songRepository.getSongById(it) }
                else -> songRepository.getPreviousSong(currentSongId)
            }
            previousSong?.let {
                withContext(Dispatchers.Main) {
                    playSong(it.mp3Url ?: return@withContext)
                    currentSongTitle = it.title
                    currentSongArtist = it.artistNameList?.joinToString(", ") ?: "Unknown Artist"
                    currentSongImage = it.imageUrl
                    currentSongId = it.id
                    val intent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "PREVIOUS")
                        putExtra("SONG_ID", it.id)
                        putExtra("SONG_TITLE", it.title)
                        putExtra("SONG_ARTIST", it.artistNameList?.joinToString(", ") ?: "Unknown Artist")
                        putExtra("SONG_IMAGE", it.imageUrl)
                    }
                    sendBroadcast(intent)
                    updateNotification()
                }
            }
        }
    }

    private fun closeService() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        stopForeground(true)
        stopSelf()
    }

    private fun broadcastEvent(action: String) {
        val intent = Intent("MUSIC_EVENT").apply {
            putExtra("ACTION", action)
            putExtra("LOOP_ENABLED", isLoopEnabled)
            putExtra("SHUFFLE_ENABLED", isShuffleEnabled)
        }
        sendBroadcast(intent)
    }

    private fun playSong(path: String) {
        // Release old player (if any)
        exoPlayer?.release()

        // Broadcast loading event
        broadcastEvent("LOADING")

        // Initialize ExoPlayer
        exoPlayer = ExoPlayer.Builder(this).build().apply {
            val mediaItem = MediaItem.fromUri(path)
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            this@MusicService.isPlaying = true
                            startPositionUpdates()
                            updateNotification()
                            broadcastEvent("LOADED")
                        }
                        Player.STATE_ENDED -> {
                            if (isLoopEnabled) {
                                seekTo(0)
                                playWhenReady = true
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
        exoPlayer?.playWhenReady = false
        isPlaying = false
        updateNotification()
        broadcastEvent("PAUSE")
    }

    private fun resumeSong() {
        exoPlayer?.playWhenReady = true
        isPlaying = true
        updateNotification()
        broadcastEvent("PLAY")
    }

    private var isUpdating = false

    private fun startPositionUpdates() {
        if (isUpdating) return
        isUpdating = true

        CoroutineScope(Dispatchers.IO).launch {
            while (isUpdating && exoPlayer != null) {
                try {
                    val position = withContext(Dispatchers.Main) { exoPlayer?.currentPosition ?: 0L }
                    val duration = withContext(Dispatchers.Main) { exoPlayer?.duration ?: 0L }
                    val intent = Intent("MUSIC_EVENT").apply {
                        putExtra("ACTION", "POSITION_UPDATE")
                        putExtra("POSITION", position)
                        putExtra("DURATION", duration)
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

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Music Player",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Music player controls"
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification() {
        CoroutineScope(Dispatchers.Main).launch {
            val art = currentSongImage?.let { loadSongImage(it) }
            val notification = createNotification(art)
            startForeground(NOTIFICATION_ID, notification)
            if (!isPlaying) {
                stopForeground(STOP_FOREGROUND_DETACH)
                NotificationManagerCompat.from(this@MusicService).notify(NOTIFICATION_ID, notification)
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

    private fun createNotification(albumArt: Bitmap?): Notification {
        val contentIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("FROM_NOTIFICATION", true)
            putExtra("SONG_ID", currentSongId)
        }
        val pendingContentIntent = PendingIntent.getActivity(
            this, 0, contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val playPauseAction = if (isPlaying) {
            NotificationCompat.Action(
                R.drawable.pause_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Pause",
                createPendingIntent("PAUSE")
            )
        } else {
            NotificationCompat.Action(
                R.drawable.play_arrow_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
                "Play",
                createPendingIntent("PLAY")
            )
        }

        val previousAction = NotificationCompat.Action(
            R.drawable.skip_previous_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Previous",
            createPendingIntent("PREVIOUS")
        )

        val nextAction = NotificationCompat.Action(
            R.drawable.skip_next_24dp_e3e3e3_fill0_wght400_grad0_opsz24,
            "Next",
            createPendingIntent("NEXT")
        )

        val closeAction = NotificationCompat.Action(
            R.drawable.close48,
            "Close",
            createPendingIntent("CLOSE")
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle(currentSongTitle ?: "Unknown title")
            .setContentText(currentSongArtist ?: "Unknown artist")
            .setLargeIcon(albumArt)
            .setContentIntent(pendingContentIntent)
            .setStyle(androidx.media.app.NotificationCompat.MediaStyle()
                .setShowActionsInCompactView(0, 1, 2))
            .addAction(previousAction)
            .addAction(playPauseAction)
            .addAction(nextAction)
            .addAction(closeAction)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val intent = Intent(action).setPackage(packageName)
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
                "PLAY" -> resumeSong()
                "PAUSE" -> pauseSong()
                "PREVIOUS" -> playPreviousSong()
                "NEXT" -> playNextSong()
                "CLOSE" -> closeService()
            }
        }
    }

    override fun onDestroy() {
        stopPositionUpdates()
        exoPlayer?.release()
        exoPlayer = null
        unregisterReceiver(notificationActionReceiver)
        super.onDestroy()
    }

    private fun downloadSong(song: Song) {
        val workRequest = OneTimeWorkRequestBuilder<DownloadSongWorker>()
            .setInputData(
                workDataOf(
                    "song_url" to song.mp3Url,
                    "song_title" to song.title,
                    "song_id" to song.id.toString()
                )
            )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)
    }
}