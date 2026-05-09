package com.annoyingline.app.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import kotlin.random.Random

class LineOverlayView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#555555") // Dark gray
        strokeWidth = 2f * resources.displayMetrics.density // 2dp width
        style = Paint.Style.STROKE
        alpha = 128 // ~50%
    }

    private var xOffset: Float = 0.45f
    private var mode: LineMode = LineMode.VERTICAL

    enum class LineMode {
        VERTICAL, DIAGONAL_FORWARD, DIAGONAL_BACKWARD
    }

    init {
        // Randomize mode on start
        mode = LineMode.values()[Random.nextInt(LineMode.values().size)]
    }

    fun setLinePosition(offset: Float) {
        xOffset = offset.coerceIn(0.1f, 0.9f)
        invalidate()
    }

    fun setLineAlpha(newAlpha: Int) {
        paint.alpha = newAlpha.coerceIn(0, 255)
        invalidate()
    }

    fun randomizeMode() {
        mode = LineMode.values()[Random.nextInt(LineMode.values().size)]
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val startX = w * xOffset

        when (mode) {
            LineMode.VERTICAL -> {
                canvas.drawLine(startX, 0f, startX, h, paint)
            }
            LineMode.DIAGONAL_FORWARD -> {
                // Top-Left to Bottom-Right
                val spread = w * 0.5f 
                canvas.drawLine(startX - spread, 0f, startX + spread, h, paint)
            }
            LineMode.DIAGONAL_BACKWARD -> {
                // Top-Right to Bottom-Left
                val spread = w * 0.5f
                canvas.drawLine(startX + spread, 0f, startX - spread, h, paint)
            }
        }
    }
}
