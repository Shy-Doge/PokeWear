package com.shy.pokewear.presentation

import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class WalkerView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    /* I am pretty sure pulling this fast isnt what the actual device does lmao
    I should probably limit this to like 25hz or whatever pulling rate it actually uses
    for now, I have no clue what it is so I am keeping it like this, plus, it worked
    like 1 out of 100 times on the android studio emulator lol */

    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            //Log.d("SensorTiming", "Sensor timestamp: ${event.timestamp} | X: ${event.values[0]}, Y: ${event.values[1]}, Z: ${event.values[2]}")

            val gravity = 9.81f // Earth's gravity in m/s²
            val scale = 512f    // BMA150 10-bit resolution, ±2g = ±512

            // Convert Android values (in m/s²) to g
            val x_g = event.values[0] / gravity
            val y_g = event.values[1] / gravity
            val z_g = event.values[2] / gravity

            // Convert to BMA150 raw 10-bit signed values
            val x_raw = (x_g * scale).toInt().coerceIn(-512, 511)
            val y_raw = (y_g * scale).toInt().coerceIn(-512, 511)
            val z_raw = (z_g * scale).toInt().coerceIn(-512, 511)

            // If NativeBridge only accepts 8-bit, scale down proportionally
            val x_byte = (x_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
            val y_byte = (y_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()
            val z_byte = (z_raw / 4).coerceIn(Byte.MIN_VALUE.toInt(), Byte.MAX_VALUE.toInt()).toByte()

            NativeBridge.setAccelerometer(x_byte, y_byte, z_byte)

            // Log the raw data with timestamp
            //Log.d("AccelerometerData", " | X: $x_raw, Y: $y_raw, Z: $z_raw")
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    private var backgroundBitmap: Bitmap? = null

    private val nativeWidth = 96
    private val nativeHeight = 64
    private val scale = 3
    private var running = false

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG)
    //
    //  This would make the pixels sharp but since they arent scaling well they look ugly
    //  so, because of that, we keep the filtering to hide that.
    //
    //private val paint = Paint().apply {
    //    isFilterBitmap = false
    //}

    private data class CircleButton(
        val label: String,
        val keyMask: Int,
        var centerX: Float = 0f,
        var centerY: Float = 0f,
        var radius: Float = 0f
    )

    private val buttons = listOf(
        CircleButton("◀\uFE0F", 0b00000100),
        CircleButton(" ", 0b00000001),
        CircleButton("▶\uFE0F", 0b00010000)
    )

    private val buttonPaint = Paint().apply {
        color = Color.DKGRAY
        isAntiAlias = true
        setShadowLayer(8f, 2f, 2f, Color.BLACK)
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val assetManager: AssetManager = context.assets

        try {
            val inputStream = assetManager.open("background.png")
            backgroundBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
        } catch (e: Exception) {
            Log.e("WalkerView", "Failed to load background.png", e)
        }

        // ✅ FIRST: Set up the sensor listener
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(
                sensorListener,
                accelerometer,
                SensorManager.SENSOR_DELAY_FASTEST/*,
                0 // maxReportLatencyUs = 0 disables batching, I am pretty sure it
                worked once with this and again without it, I think the problem is
                inside the walker.c */
            )

        }

        // ✅ THEN: Call into native code
        NativeBridge.init(assetManager, nativeWidth, nativeHeight)

        running = true
        startGameLoop()
    }


    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        sensorManager.unregisterListener(sensorListener)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    private fun startGameLoop() {
        // I am sorry, I know this looks really bad but this was to get the emulator working
        // before making it look good and I just kept adding stuff...
        // BUT HEY, IT WORKS (badly...)

        thread(start = true) {
            val targetFps = 4
            val tickIntervalMs = 1000L / targetFps

            while (running) {
                val startTime = System.currentTimeMillis()

                if (!NativeBridge.step()) {
                    running = false
                    break
                }

                val currentFrameBitmap = NativeBridge.getFrame(nativeWidth, nativeHeight)
                //Log.d("WalkerView", "Frame is null: ${currentFrameBitmap == null}")

                if (currentFrameBitmap != null) {
                    val canvas = holder.lockCanvas()
                    if (canvas != null) {
                        try {
                            canvas.drawColor(Color.BLACK)

                            backgroundBitmap?.let {
                                val bgRect = Rect(0, 0, width, height)
                                canvas.drawBitmap(it, null, bgRect, paint)
                            }

                            val shellScreenWidth = 235
                            val shellScreenHeight = 155
                            val shellScreenTop = 128
                            val screenLeft = (width - shellScreenWidth) / 2
                            val dstRect = Rect(
                                screenLeft,
                                shellScreenTop,
                                screenLeft + shellScreenWidth,
                                shellScreenTop + shellScreenHeight
                            )

                            val opaqueBitmap = currentFrameBitmap.copy(Bitmap.Config.ARGB_8888, true)
                            val canvasTemp = Canvas(opaqueBitmap)
                            canvasTemp.drawColor(Color.BLACK)
                            canvasTemp.drawBitmap(currentFrameBitmap, 0f, 0f, null)

                            canvas.drawBitmap(opaqueBitmap, null, dstRect, paint)

                            val buttonAreaTop = shellScreenTop + shellScreenHeight + 60f
                            val buttonRadius = width / 12f
                            val spacing = width / 4f

                            for ((i, button) in buttons.withIndex()) {
                                button.centerX = (width / 2f - spacing) + spacing * i
                                button.centerY = buttonAreaTop
                                button.radius = buttonRadius

                                canvas.drawCircle(button.centerX, button.centerY, button.radius, buttonPaint)
                                canvas.drawText(
                                    button.label,
                                    button.centerX,
                                    button.centerY + (textPaint.textSize / 3),
                                    textPaint
                                )
                            }
                        } finally {
                            holder.unlockCanvasAndPost(canvas)
                        }
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = tickIntervalMs - elapsed
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        running = false
                        break
                    }
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                for (button in buttons) {
                    val dx = event.x - button.centerX
                    val dy = event.y - button.centerY
                    if (dx * dx + dy * dy <= button.radius * button.radius) {
                        NativeBridge.setKeys(button.keyMask)
                        break
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                NativeBridge.setKeys(0)
            }
        }
        return true
    }

    fun start() {
        if (!running && holder.surface.isValid) {
            running = true
            startGameLoop()
        }
    }
}
