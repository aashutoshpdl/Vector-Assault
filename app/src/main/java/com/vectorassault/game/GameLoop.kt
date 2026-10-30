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
        if (isRunning) return
        isRunning = true
        try {
            start()
        } catch (_: IllegalThreadStateException) {}
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
                try {
                    gameView.updatePhysics(targetTimeStep.toFloat())
                } catch (_: Throwable) {}
                accumulator -= targetTimeStep
            }

            // Verify surface is valid before attempting any canvas operations
            if (!surfaceHolder.surface.isValid) {
                try {
                    sleep(10)
                } catch (_: InterruptedException) {}
                continue
            }

            // Render phase using standard, rock-solid SurfaceView lockCanvas
            // (Avoids lockHardwareCanvas which triggers native SIGSEGV in Mali GPU drivers on Samsung devices)
            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockCanvas()
                if (canvas != null) {
                    synchronized(surfaceHolder) {
                        gameView.renderFrame(canvas)
                    }
                }
            } catch (_: Throwable) {
                // Prevent unhandled exceptions from terminating the thread
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Throwable) {}
                }
            }
        }
    }
}
