package vn.edu.usth.msma.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.edu.usth.msma.R
import vn.edu.usth.msma.data.PreferencesManager
import vn.edu.usth.msma.utils.eventbus.Event.NotificationsUpdateEvent
import vn.edu.usth.msma.utils.eventbus.EventBus
import javax.inject.Inject
import kotlin.random.Random

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var preferencesManager: PreferencesManager

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")
        // Save the token to PreferencesManager
        CoroutineScope(Dispatchers.IO).launch {
            preferencesManager.saveFcmToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle notification payload
        remoteMessage.notification?.let {
            Log.d("FCM", "Message received: ${it.title}, ${it.body}")
            showNotification(it.title ?: "New Notification", it.body ?: "You have a new message")

            CoroutineScope(Dispatchers.IO).launch {
                EventBus.publish(NotificationsUpdateEvent)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "default_channel_id"

        // Create notification channel for Android O and above
        val channel = NotificationChannel(
            channelId,
            "Default Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.logo))
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}