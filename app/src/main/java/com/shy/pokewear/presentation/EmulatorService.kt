package com.shy.pokewear.engine

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.shy.pokewear.R

class EmulatorService : Service() {

    // Keeping the emulator running on the background so it doesnt die and can still
    // count steps. If only that worked correctly :clueless:

    override fun onCreate() {
        super.onCreate()
        Log.d("EmulatorService", "onCreate called")

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PokéWear Emulator Running")
            .setContentText("Running in background...")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOngoing(true)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )

        EmulatorEngine.start(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        EmulatorEngine.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "emulator_channel"

        fun start(context: Context) {
            val intent = Intent(context, EmulatorService::class.java)
            context.startForegroundService(intent)
        }

        fun createNotificationChannel(context: Context) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Emulator Background Service",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = context.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }
}
