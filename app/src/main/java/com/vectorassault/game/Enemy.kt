package com.vectorassault.game

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

enum class EnemyType {
    DRONE,       // Flocking swarm fighter
    INTERCEPTOR, // High-speed hit-and-run strafer
    DREADNOUGHT, // Heavy orbital fortress deploying homing mines
    MINE         // Explosive mini-mine deployed by dreadnought
}

class Enemy(
    val position: Vector2D,
    val type: EnemyType,
    var health: Float = when (type) {
        EnemyType.DRONE -> 30f
        EnemyType.INTERCEPTOR -> 50f
        EnemyType.DREADNOUGHT -> 250f
        EnemyType.MINE -> 15f
    },
    val radius: Float = when (type) {
        EnemyType.DRONE -> 16f
        EnemyType.INTERCEPTOR -> 22f
        EnemyType.DREADNOUGHT -> 42f
        EnemyType.MINE -> 10f
    }
) {
    val velocity = Vector2D()
    var isAlive = true
    var angle: Float = 0f
    var shootCooldown: Float = when (type) {
        EnemyType.DRONE -> 2.5f
        EnemyType.INTERCEPTOR -> 1.5f
        EnemyType.DREADNOUGHT -> 2.0f
        EnemyType.MINE -> 999f
    }

    // AI state variables
    private var strafeTimer: Float = 0f
    private var orbitalAngle: Float = 0f
    private val enemyPath = Path()

    fun update(
        dt: Float,
        playerPos: Vector2D,
        swarm: List<Enemy>,
        boundsW: Float,
        boundsH: Float,
        onSpawnBullet: (Bullet) -> Unit,
        onSpawnMine: (Enemy) -> Unit
    ) {
        shootCooldown -= dt

        when (type) {
            EnemyType.DRONE -> updateDroneAI(dt, playerPos, swarm, onSpawnBullet)
            EnemyType.INTERCEPTOR -> updateInterceptorAI(dt, playerPos, boundsW, boundsH, onSpawnBullet)
            EnemyType.DREADNOUGHT -> updateDreadnoughtAI(dt, playerPos, onSpawnMine)
            EnemyType.MINE -> updateMineAI(dt, playerPos)
        }

        position.add(velocity.copy().multiply(dt))
        angle = atan2(velocity.y, velocity.x)
    }

    private fun updateDroneAI(
        dt: Float,
        playerPos: Vector2D,
        swarm: List<Enemy>,
        onSpawnBullet: (Bullet) -> Unit
    ) {
        // Reynolds Boids: Separation + Player Attraction Cohesion
        val sep = Vector2D()
        var sepCount = 0
        for (other in swarm) {
            if (other === this || other.type != EnemyType.DRONE) continue
            val d = position.distanceTo(other.position)
            if (d < 50f && d > 0.001f) {
                val push = Vector2D(position.x - other.position.x, position.y - other.position.y)
                push.normalize().multiply(1f / d)
                sep.add(push)
                sepCount++
            }
        }
        if (sepCount > 0) {
            sep.multiply(300f)
        }

        // Direction to player
        val toPlayer = Vector2D(playerPos.x - position.x, playerPos.y - position.y).normalize().multiply(180f)
        val targetVel = Vector2D(toPlayer.x + sep.x, toPlayer.y + sep.y)

        // Smooth velocity adjustment
        velocity.x += (targetVel.x - velocity.x) * 4f * dt
        velocity.y += (targetVel.y - velocity.y) * 4f * dt

        // Occasional drone pulse shot
        if (shootCooldown <= 0f && position.distanceTo(playerPos) < 550f) {
            shootCooldown = 2.4f + (Math.random().toFloat() * 1.2f)
            val shotDir = Vector2D(playerPos.x - position.x, playerPos.y - position.y).normalize().multiply(450f)
            onSpawnBullet(Bullet(position.copy(), shotDir, isPlayerBullet = false, damage = 12f, color = Color.RED))
        }
    }

    private fun updateInterceptorAI(
        dt: Float,
        playerPos: Vector2D,
        boundsW: Float,
        boundsH: Float,
        onSpawnBullet: (Bullet) -> Unit
    ) {
        strafeTimer += dt
        val dist = position.distanceTo(playerPos)

        // State 1: Dive-bomb strafe towards player
        if (dist > 200f) {
            val dir = Vector2D(playerPos.x - position.x, playerPos.y - position.y).normalize().multiply(380f)
            velocity.x += (dir.x - velocity.x) * 3f * dt
            velocity.y += (dir.y - velocity.y) * 3f * dt
        } else {
            // State 2: Rapid evasive swing away
            val away = Vector2D(position.x - playerPos.x, position.y - playerPos.y).normalize().multiply(420f)
            val perpendicular = Vector2D(-away.y, away.x)
            velocity.x += (perpendicular.x - velocity.x) * 5f * dt
            velocity.y += (perpendicular.y - velocity.y) * 5f * dt
        }

        // Rapid dual laser fire
        if (shootCooldown <= 0f && dist < 650f) {
            shootCooldown = 1.6f
            val aim = Vector2D(playerPos.x - position.x, playerPos.y - position.y).normalize().multiply(520f)
            onSpawnBullet(Bullet(position.copy(), aim, isPlayerBullet = false, damage = 15f, color = Color.MAGENTA))
        }
    }

    private fun updateDreadnoughtAI(
        dt: Float,
        playerPos: Vector2D,
        onSpawnMine: (Enemy) -> Unit
    ) {
        orbitalAngle += dt * 0.4f
        // Slow menacing patrol towards center/player vicinity
        val targetX = playerPos.x + cos(orbitalAngle) * 320f
        val targetY = playerPos.y + sin(orbitalAngle) * 320f
        val dir = Vector2D(targetX - position.x, targetY - position.y)
        if (dir.length() > 20f) {
            dir.normalize().multiply(80f)
            velocity.x += (dir.x - velocity.x) * 2f * dt
            velocity.y += (dir.y - velocity.y) * 2f * dt
        }

        // Deploys proximity homing mines
        if (shootCooldown <= 0f) {
            shootCooldown = 3.5f
            val mine = Enemy(position.copy(), EnemyType.MINE)
            onSpawnMine(mine)
        }
    }

    private fun updateMineAI(dt: Float, playerPos: Vector2D) {
        // Homing mine slowly drifts towards player and accelerates
        val dir = Vector2D(playerPos.x - position.x, playerPos.y - position.y).normalize().multiply(130f)
        velocity.x += (dir.x - velocity.x) * 2f * dt
        velocity.y += (dir.y - velocity.y) * 2f * dt
    }

    fun takeDamage(amount: Float): Boolean {
        health -= amount
        if (health <= 0f) {
            isAlive = false
            return true
        }
        return false
    }

    fun render(canvas: Canvas, paint: Paint) {
        canvas.save()
        canvas.translate(position.x, position.y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f

        when (type) {
            EnemyType.DRONE -> {
                paint.color = Color.rgb(255, 60, 60) // Neon Red
                enemyPath.reset()
                enemyPath.moveTo(radius, 0f)
                enemyPath.lineTo(-radius, -radius * 0.75f)
                enemyPath.lineTo(-radius * 0.4f, 0f)
                enemyPath.lineTo(-radius, radius * 0.75f)
                enemyPath.close()
                canvas.drawPath(enemyPath, paint)
            }
            EnemyType.INTERCEPTOR -> {
                paint.color = Color.rgb(255, 0, 200) // Neon Magenta
                enemyPath.reset()
                enemyPath.moveTo(radius * 1.2f, 0f)
                enemyPath.lineTo(-radius, -radius)
                enemyPath.lineTo(0f, 0f)
                enemyPath.lineTo(-radius, radius)
                enemyPath.close()
                canvas.drawPath(enemyPath, paint)
                canvas.drawCircle(0f, 0f, 4f, paint)
            }
            EnemyType.DREADNOUGHT -> {
                paint.color = Color.rgb(255, 140, 0) // Neon Orange
                // Heavy geometric octagon
                val r = radius
                enemyPath.reset()
                for (i in 0 until 8) {
                    val a = (i * Math.PI / 4.0)
                    val px = (cos(a) * r).toFloat()
                    val py = (sin(a) * r).toFloat()
                    if (i == 0) enemyPath.moveTo(px, py) else enemyPath.lineTo(px, py)
                }
                enemyPath.close()
                canvas.drawPath(enemyPath, paint)
                // Inner core
                paint.color = Color.RED
                canvas.drawCircle(0f, 0f, radius * 0.4f, paint)
            }
            EnemyType.MINE -> {
                paint.color = Color.YELLOW
                canvas.drawCircle(0f, 0f, radius, paint)
                canvas.drawLine(-radius * 1.4f, 0f, radius * 1.4f, 0f, paint)
                canvas.drawLine(0f, -radius * 1.4f, 0f, radius * 1.4f, paint)
            }
        }

        canvas.restore()
    }
}
