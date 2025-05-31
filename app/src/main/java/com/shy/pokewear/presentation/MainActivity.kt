package com.shy.pokewear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.shy.pokewear.engine.EmulatorService

class MainActivity : ComponentActivity() {
    private lateinit var walkerView: WalkerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        EmulatorService.createNotificationChannel(this)
        EmulatorService.start(this)

        walkerView = WalkerView(this)
        setContentView(walkerView)
    }
}
