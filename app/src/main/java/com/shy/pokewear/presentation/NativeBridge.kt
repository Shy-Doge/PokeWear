package com.shy.pokewear.presentation

import android.content.res.AssetManager
import android.graphics.Bitmap

object NativeBridge {
    init {
        System.loadLibrary("walker")
    }

    external fun init(assetManager: AssetManager, width: Int, height: Int)

    external fun step(): Boolean // Returns whether to keep running

    external fun setKeys(input: Int) // C expects a bitmask of inputs
    external fun quarterRTCInterrupt() // RTC logic

    external fun getFrame(width: Int, height: Int): Bitmap? // Make Bitmap nullable if it can fail
    external fun setAccelerometer(x: Byte, y: Byte, z: Byte)

}