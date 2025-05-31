package com.shy.pokewear.engine

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.res.AssetManager
import com.shy.pokewear.presentation.NativeBridge
import android.graphics.*
import kotlin.concurrent.thread

object EmulatorEngine {

    @Volatile
    private var currentFrame: Bitmap? = null

    fun getCurrentFrame(): Bitmap? = currentFrame

    private var isInitialized = false

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    // I assume the original accelerometer pulls at 25hz so this way we simulate it.
    // Its not the best idea but it works for now since it isnt working that well
    // on the emulator side of things.

    private val SENSOR_TARGET_HZ = 25
    private val SENSOR_INTERVAL_NS = 1_000_000_000L / SENSOR_TARGET_HZ
    private var lastSensorTimestamp = 0L

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!isInitialized) return
            if (event.timestamp - lastSensorTimestamp < SENSOR_INTERVAL_NS) return
            lastSensorTimestamp = event.timestamp

            val gravity = 9.81f
            val scale = 512f

            val x_g = event.values[0] / gravity
            val y_g = event.values[1] / gravity
            val z_g = event.values[2] / gravity

            val x_raw = (x_g * scale).toInt().coerceIn(-512, 511)
            val y_raw = (y_g * scale).toInt().coerceIn(-512, 511)
            val z_raw = (z_g * scale).toInt().coerceIn(-512, 511)

            val x_byte = (x_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
            val y_byte = (y_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
            val z_byte = (z_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()

            NativeBridge.setAccelerometer(x_byte, y_byte, z_byte)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    fun init(context: Context) {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(sensorListener, it, SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun shutdown() {
        sensorManager.unregisterListener(sensorListener)
    }

    private var running = false
    private val nativeWidth = 96
    private val nativeHeight = 64

    fun start(context: Context) {
        if (running) return
        running = true

        val assets: AssetManager = context.assets
        NativeBridge.init(assets, nativeWidth, nativeHeight)
        isInitialized = true

        thread(start = true) {
            val targetFps = 4
            val tickIntervalMs = 1000L / targetFps

            while (running) {
                val startTime = System.currentTimeMillis()

                if (NativeBridge.step()) {
                    currentFrame = NativeBridge.getFrame(nativeWidth, nativeHeight)
                } else {
                    running = false
                    break
                }

                val sleepTime = tickIntervalMs - (System.currentTimeMillis() - startTime)
                if (sleepTime > 0) Thread.sleep(sleepTime)
            }
        }
    }

        fun stop() {
        running = false
    }

    fun isRunning() = running
}
