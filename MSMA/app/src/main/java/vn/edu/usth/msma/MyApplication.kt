package vn.edu.usth.msma

import android.app.Application
import android.content.Intent
import dagger.hilt.android.HiltAndroidApp
import vn.edu.usth.msma.service.MusicService

@HiltAndroidApp
class MyApplication : Application() {
    override fun onTerminate() {
        super.onTerminate()
        // Close music service when app is terminated
        val intent = Intent(this, MusicService::class.java).apply {
            action = "CLOSE"
        }
        startService(intent)
    }
}