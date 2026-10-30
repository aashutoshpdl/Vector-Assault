package com.vectorassault.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Bullet(
    val position: Vector2D,
    val velocity: Vector2D,
    val isPlayerBullet: Boolean,
    val damage: Float = 25f,
    val radius: Float = 5f,
    var maxLifetime: Float = 2.2f,
    val color: Int = if (isPlayerBullet) Color.CYAN else Color.RED
) {
    var isAlive: Boolean = true
    private var lifetime: Float = maxLifetime

    fun update(dt: Float, boundsW: Float, boundsH: Float) {
        position.x += velocity.x * dt
        position.y += velocity.y * dt
        lifetime -= dt
        if (lifetime <= 0f || position.x < -40 || position.x > boundsW + 40 || position.y < -40 || position.y > boundsH + 40) {
            isAlive = false
        }
    }

    fun render(canvas: Canvas, paint: Paint) {
        paint.style = Paint.Style.FILL
        paint.color = color
        canvas.drawCircle(position.x, position.y, radius, paint)

        // Beam trail effect
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f
        paint.color = Color.WHITE
        val tailX = position.x - velocity.x * 0.035f
        val tailY = position.y - velocity.y * 0.035f
        canvas.drawLine(position.x, position.y, tailX, tailY, paint)
    }
}
