package vn.edu.usth.msma.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class MusicActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return

        // Forward the action to MusicService
        val serviceIntent = Intent(context, MusicService::class.java)
        serviceIntent.action = intent.action
        context.startService(serviceIntent)
    }
}