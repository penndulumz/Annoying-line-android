package com.annoyingline.app.overlay

import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class DriftEngine(
    private val onPositionUpdate: (Float) -> Unit,
    private val onAlphaUpdate: (Int) -> Unit,
    private val onModeUpdate: () -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    var currentOffset: Float = 0.45f

    private val driftRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            
            // Large random jump across the screen (left to right, 10% to 90%)
            currentOffset = Random.nextFloat() * 0.8f + 0.1f
            onPositionUpdate(currentOffset)
            onModeUpdate() // Change mode explicitly on long drift
            
            // Schedule next drift (10 to 15 minutes)
            val nextDriftDelay = Random.nextLong(10 * 60 * 1000L, 15 * 60 * 1000L)
            handler.postDelayed(this, nextDriftDelay)
        }
    }

    private val flickerRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            
            val flicks = Random.nextInt(1, 5) // 1 to 4 flicks
            doFlickSequence(flicks)

            // Schedule next flicker event (aggressive: every 5 to 15 seconds)
            val nextFlickerDelay = Random.nextLong(5 * 1000L, 15 * 1000L)
            handler.postDelayed(this, nextFlickerDelay)
        }
    }

    private fun doFlickSequence(remainingFlicks: Int) {
        if (!isRunning || remainingFlicks <= 0) return
        
        // 1. Drop alpha, jitter position, change mode
        onAlphaUpdate(10)
        onModeUpdate()
        val jitter = (Random.nextFloat() - 0.5f) * 0.05f // +/- 2.5% micro-drift
        onPositionUpdate((currentOffset + jitter).coerceIn(0.1f, 0.9f))
        
        // 2. Spike bright
        handler.postDelayed({
            if (!isRunning) return@postDelayed
            onAlphaUpdate(180)
            
            // 3. Restore normal
            handler.postDelayed({
                if (!isRunning) return@postDelayed
                onAlphaUpdate(128)
                onPositionUpdate(currentOffset) // snap back to real offset
                
                // 4. Trigger next flick
                handler.postDelayed({
                    doFlickSequence(remainingFlicks - 1)
                }, Random.nextLong(50L, 150L))
                
            }, 100)
            
        }, Random.nextLong(100L, 250L))
    }

    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(driftRunnable)
        handler.post(flickerRunnable)
    }

    fun stop() {
        isRunning = false
        handler.removeCallbacks(driftRunnable)
        handler.removeCallbacks(flickerRunnable)
    }
}
