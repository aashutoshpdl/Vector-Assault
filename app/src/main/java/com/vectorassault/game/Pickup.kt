package com.vectorassault.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.cos
import kotlin.math.sin

enum class PickupType {
    HEALTH, SHIELD, MULTI_SHOT
}

class Pickup(
    val position: Vector2D,
    val type: PickupType,
    val radius: Float = 16f
) {
    var isAlive: Boolean = true
    private var age: Float = 0f
    private var maxLifetime: Float = 12f

    fun update(dt: Float) {
        age += dt
        if (age >= maxLifetime) {
            isAlive = false
        }
    }

    fun render(canvas: Canvas, paint: Paint) {
        val pulse = 1f + 0.2f * sin(age * 6f)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        when (type) {
            PickupType.HEALTH -> {
                paint.color = Color.GREEN
                canvas.drawCircle(position.x, position.y, radius * pulse, paint)
                // Draw Cross
                val arm = 7f
                canvas.drawLine(position.x - arm, position.y, position.x + arm, position.y, paint)
                canvas.drawLine(position.x, position.y - arm, position.x, position.y + arm, paint)
            }
            PickupType.SHIELD -> {
                paint.color = Color.CYAN
                canvas.drawCircle(position.x, position.y, radius * pulse, paint)
                // Draw Diamond
                val d = 8f
                canvas.drawLine(position.x, position.y - d, position.x + d, position.y, paint)
                canvas.drawLine(position.x + d, position.y, position.x, position.y + d, paint)
                canvas.drawLine(position.x, position.y + d, position.x - d, position.y, paint)
                canvas.drawLine(position.x - d, position.y, position.x, position.y - d, paint)
            }
            PickupType.MULTI_SHOT -> {
                paint.color = Color.YELLOW
                canvas.drawCircle(position.x, position.y, radius * pulse, paint)
                // Draw Triple Points
                canvas.drawPoint(position.x - 5f, position.y, paint)
                canvas.drawPoint(position.x, position.y - 5f, paint)
                canvas.drawPoint(position.x + 5f, position.y, paint)
            }
        }
    }
}
