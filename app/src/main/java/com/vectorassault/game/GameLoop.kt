package com.vectorassault.game

import android.graphics.Canvas
import android.view.SurfaceHolder

class GameLoop(
    private val gameView: GameView,
    private val surfaceHolder: SurfaceHolder
) : Thread() {

    @Volatile
    private var isRunning = false

    fun startLoop() {
        isRunning = true
        start()
    }

    fun stopLoop() {
        isRunning = false
    }

    override fun run() {
        var previousTime = System.nanoTime()
        val targetTimeStep = 1.0 / 60.0 // 60 FPS fixed step physics
        var accumulator = 0.0

        while (isRunning) {
            val currentTime = System.nanoTime()
            var frameDuration = (currentTime - previousTime) / 1_000_000_000.0
            previousTime = currentTime

            // Cap max frame duration to avoid spiral of death on lag spikes
            if (frameDuration > 0.1) frameDuration = 0.1
            accumulator += frameDuration

            // Fixed timestep physics update
            while (accumulator >= targetTimeStep) {
                gameView.updatePhysics(targetTimeStep.toFloat())
                accumulator -= targetTimeStep
            }

            // Render phase
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockHardwareCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.renderFrame(canvas)
                    }
                }
            } catch (_: Exception) {
                // Lock may fail if surface is destroyed
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {}
                }
            }
        }
    }
}
