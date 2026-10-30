package com.vectorassault.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.util.Random
import kotlin.math.cos
import kotlin.math.sin

class Particle(
    val position: Vector2D = Vector2D(),
    val velocity: Vector2D = Vector2D(),
    var color: Int = Color.WHITE,
    var size: Float = 4f,
    var life: Float = 1f,
    val maxLife: Float = 1f,
    val lengthScale: Float = 1.8f
) {
    fun update(dt: Float): Boolean {
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        velocity.multiply(0.96f) // drag
        life -= dt
        return life > 0f
    }

    fun render(canvas: Canvas, paint: Paint) {
        val alpha = (life / maxLife).coerceIn(0f, 1f)
        val alphaInt = (alpha * 255).toInt()
        paint.color = color
        paint.alpha = alphaInt
        paint.strokeWidth = size * alpha

        // Spark line along velocity direction
        val endX = position.x - velocity.x * 0.05f * lengthScale
        val endY = position.y - velocity.y * 0.05f * lengthScale
        canvas.drawLine(position.x, position.y, endX, endY, paint)
    }
}

class ParticleSystem {
    private val particles = ArrayList<Particle>(400)
    private val random = Random()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
    }

    var screenShakeAmount = 0f

    fun triggerExplosion(x: Float, y: Float, color: Int, count: Int = 45, speedRange: Float = 450f) {
        screenShakeAmount = (screenShakeAmount + 14f).coerceAtMost(28f)
        for (i in 0 until count) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 80f + random.nextFloat() * speedRange
            val vel = Vector2D(cos(angle) * speed, sin(angle) * speed)
            val maxLife = 0.4f + random.nextFloat() * 0.5f
            val pSize = 3f + random.nextFloat() * 3.5f
            particles.add(
                Particle(
                    position = Vector2D(x, y),
                    velocity = vel,
                    color = color,
                    size = pSize,
                    life = maxLife,
                    maxLife = maxLife
                )
            )
        }
    }

    fun triggerShipHit(x: Float, y: Float, count: Int = 12) {
        screenShakeAmount = (screenShakeAmount + 8f).coerceAtMost(20f)
        for (i in 0 until count) {
            val angle = random.nextFloat() * Math.PI.toFloat() * 2f
            val speed = 120f + random.nextFloat() * 220f
            val vel = Vector2D(cos(angle) * speed, sin(angle) * speed)
            val maxLife = 0.3f + random.nextFloat() * 0.3f
            particles.add(
                Particle(
                    position = Vector2D(x, y),
                    velocity = vel,
                    color = Color.CYAN,
                    size = 3.5f,
                    life = maxLife,
                    maxLife = maxLife
                )
            )
        }
    }

    fun update(dt: Float) {
        if (screenShakeAmount > 0f) {
            screenShakeAmount -= dt * 40f
            if (screenShakeAmount < 0f) screenShakeAmount = 0f
        }

        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            if (!p.update(dt)) {
                it.remove()
            }
        }
    }

    fun render(canvas: Canvas) {
        for (p in particles) {
            p.render(canvas, paint)
        }
    }

    fun clear() {
        particles.clear()
        screenShakeAmount = 0f
    }
}
