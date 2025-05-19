package com.shy.pokewear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    private lateinit var walkerView: WalkerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        walkerView = WalkerView(this)
        setContentView(walkerView)

        walkerView.start()
    }
}