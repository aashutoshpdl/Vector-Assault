package com.vectorassault.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Player(
    var x: Float,
    var y: Float
) {
    val position = Vector2D(x, y)
    val velocity = Vector2D(0f, 0f)
    var facingAngle: Float = 0f // Radians

    val radius = 22f
    var health = 100f
    val maxHealth = 100f
    var shield = 100f
    val maxShield = 100f

    var multiShotTimer: Float = 0f
    var fireCooldown: Float = 0f
    val fireRate: Float = 0.12f // Seconds per shot

    private val shipPath = Path()
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.argb(190, 0, 255, 255)
    }

    fun update(dt: Float, moveVector: Vector2D, boundsW: Float, boundsH: Float) {
        // Inertial acceleration based on joystick input
        val accel = 1800f
        val friction = 0.90f

        velocity.x += moveVector.x * accel * dt
        velocity.y += moveVector.y * accel * dt
        velocity.multiply(friction)

        position.x += velocity.x * dt
        position.y += velocity.y * dt

        // Bound player to visible screen borders
        if (position.x < radius) {
            position.x = radius
            velocity.x = 0f
        }
        if (position.x > boundsW - radius) {
            position.x = boundsW - radius
            velocity.x = 0f
        }
        if (position.y < radius) {
            position.y = radius
            velocity.y = 0f
        }
        if (position.y > boundsH - radius) {
            position.y = boundsH - radius
            velocity.y = 0f
        }

        // Shield slow auto-recovery
        if (shield < maxShield) {
            shield = (shield + dt * 4f).coerceAtMost(maxShield)
        }

        if (fireCooldown > 0f) fireCooldown -= dt
        if (multiShotTimer > 0f) multiShotTimer -= dt
    }

    fun takeDamage(amount: Float): Boolean {
        var remaining = amount
        if (shield > 0f) {
            val absorb = shield.coerceAtMost(remaining)
            shield -= absorb
            remaining -= absorb
        }
        health = (health - remaining).coerceAtLeast(0f)
        return health <= 0f
    }

    fun tryFire(aimAngle: Float): List<Bullet>? {
        if (fireCooldown > 0f) return null
        fireCooldown = fireRate
        facingAngle = aimAngle

        val bullets = mutableListOf<Bullet>()
        val speed = 950f

        if (multiShotTimer > 0f) {
            // Triple spread shot
            for (spread in listOf(-0.25f, 0f, 0.25f)) {
                val angle = aimAngle + spread
                val vel = Vector2D(cos(angle) * speed, sin(angle) * speed)
                val tip = Vector2D(
                    position.x + cos(angle) * (radius + 8f),
                    position.y + sin(angle) * (radius + 8f)
                )
                bullets.add(Bullet(tip, vel, isPlayerBullet = true, damage = 35f, color = Color.CYAN))
            }
        } else {
            val vel = Vector2D(cos(aimAngle) * speed, sin(aimAngle) * speed)
            val tip = Vector2D(
                position.x + cos(aimAngle) * (radius + 8f),
                position.y + sin(aimAngle) * (radius + 8f)
            )
            bullets.add(Bullet(tip, vel, isPlayerBullet = true, damage = 35f, color = Color.CYAN))
        }
        return bullets
    }

    fun render(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.translate(position.x, position.y)
        canvas.rotate(Math.toDegrees(facingAngle.toDouble()).toFloat())

        // Draw Vector Fighter Silhouette
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3.5f
        paint.color = Color.CYAN

        shipPath.reset()
        // Sharp arrowhead vector design
        shipPath.moveTo(radius + 4f, 0f)
        shipPath.lineTo(-radius * 0.8f, -radius * 0.75f)
        shipPath.lineTo(-radius * 0.4f, 0f)
        shipPath.lineTo(-radius * 0.8f, radius * 0.75f)
        shipPath.close()
        canvas.drawPath(shipPath, paint)

        // Engine vector flame
        val engineSpeed = velocity.length()
        if (engineSpeed > 40f) {
            paint.color = Color.YELLOW
            val flameLen = (radius * 0.6f + (engineSpeed / 20f)).coerceAtMost(radius * 1.8f)
            canvas.drawLine(-radius * 0.4f, 0f, -radius * 0.4f - flameLen, 0f, paint)
        }

        canvas.restore()

        // Render glowing energy shield if active
        if (shield > 5f) {
            shieldPaint.alpha = (shield / maxShield * 180f).toInt()
            canvas.drawCircle(position.x, position.y, radius + 8f, shieldPaint)
        }
    }
}
