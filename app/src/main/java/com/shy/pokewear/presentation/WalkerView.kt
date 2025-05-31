package com.shy.pokewear.presentation

import android.content.Context
import android.content.res.AssetManager
import android.graphics.*
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import kotlin.concurrent.thread
import com.shy.pokewear.engine.EmulatorEngine

class WalkerView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

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
        CircleButton(" ", 0b00000100),
        CircleButton(" ", 0b00000001),
        CircleButton(" ", 0b00010000)
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

        EmulatorEngine.init(context)

        post {
            EmulatorEngine.start(context)
            start()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        EmulatorEngine.shutdown()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    private fun startGameLoop() {
        thread(start = true) {
            val targetFps = 4
            val tickIntervalMs = 1000L / targetFps

            while (running) {
                val startTime = System.currentTimeMillis()

                val currentFrameBitmap = EmulatorEngine.getCurrentFrame()

                val canvas = holder.lockCanvas()
                if (canvas != null) {
                    try {
                        // Always draw background
                        canvas.drawColor(Color.BLACK)

                        backgroundBitmap?.let {
                            val bgRect = Rect(0, 0, width, height)
                            canvas.drawBitmap(it, null, bgRect, paint)
                        }

                        // Draw emulator frame if available
                        currentFrameBitmap?.let {
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

                            val opaqueBitmap = it.copy(Bitmap.Config.ARGB_8888, true)
                            val canvasTemp = Canvas(opaqueBitmap)
                            canvasTemp.drawColor(Color.BLACK)
                            canvasTemp.drawBitmap(it, 0f, 0f, null)

                            canvas.drawBitmap(opaqueBitmap, null, dstRect, paint)
                        }

                        // Always draw buttons
                        val shellScreenTop = 128
                        val shellScreenHeight = 155
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

                    } catch (e: Exception) {
                        Log.e("WalkerView", "Canvas draw error", e)
                    } finally {
                        holder.unlockCanvasAndPost(canvas)
                    }
                }

                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = tickIntervalMs - elapsed
                if (sleepTime > 0) {
                    Thread.sleep(sleepTime)
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
        if (!running && holder.surface?.isValid == true) {
            running = true
            startGameLoop()
        } else {
            Log.w("WalkerView", "Surface is not valid yet")
        }
    }

}
