package com.vectorassault.game

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

data class Vector2D(
    var x: Float = 0f,
    var y: Float = 0f
) {
    fun set(newX: Float, newY: Float): Vector2D {
        x = newX
        y = newY
        return this
    }

    fun add(other: Vector2D): Vector2D {
        x += other.x
        y += other.y
        return this
    }

    fun subtract(other: Vector2D): Vector2D {
        x -= other.x
        y -= other.y
        return this
    }

    fun multiply(scalar: Float): Vector2D {
        x *= scalar
        y *= scalar
        return this
    }

    fun length(): Float = hypot(x, y)

    fun lengthSquared(): Float = x * x + y * y

    fun normalize(): Vector2D {
        val len = length()
        if (len > 0.0001f) {
            x /= len
            y /= len
        }
        return this
    }

    fun distanceTo(other: Vector2D): Float {
        val dx = other.x - x
        val dy = other.y - y
        return hypot(dx, dy)
    }

    fun copy(): Vector2D = Vector2D(x, y)

    companion object {
        fun fromAngle(angleRad: Float, length: Float = 1f): Vector2D {
            return Vector2D(cos(angleRad) * length, sin(angleRad) * length)
        }
    }
}
