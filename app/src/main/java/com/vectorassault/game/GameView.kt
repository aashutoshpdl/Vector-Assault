package com.vectorassault.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.util.Random
import kotlin.math.atan2
import kotlin.math.hypot

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameLoop: GameLoop? = null
    val soundManager = SoundManager(context)
    private val particleSystem = ParticleSystem()
    private val random = Random()

    // Screen Dimensions
    var screenWidth: Float = 1920f
    var screenHeight: Float = 1080f

    // Game entities
    lateinit var player: Player
    private val enemies = ArrayList<Enemy>()
    private val bullets = ArrayList<Bullet>()
    private val pickups = ArrayList<Pickup>()
    private val stars = ArrayList<Vector2D>()

    // Touch Joystick State
    private var moveTouchPointerId = -1
    private val joyBase = Vector2D()
    private val joyCurrent = Vector2D()
    private var joyActive = false
    private val joyRadius = 130f
    private val moveVector = Vector2D(0f, 0f)

    // Aiming State
    private var aimTouchPointerId = -1
    private var isAiming = false
    private var currentAimAngle = 0f

    // Game Progression
    var wave = 1
    var score = 0
    var multiplier = 1
    var isGameOver = false
    private var waveSpawnTimer = 0f
    private var enemiesToSpawnInWave = 0
    private var waveDirectorTimer = 0f

    // Rendering Paints
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.MONOSPACE
        textSize = 42f
        color = Color.WHITE
    }
    private val hudPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val joyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(140, 0, 255, 255)
    }
    private val starPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.argb(160, 200, 220, 255)
        strokeWidth = 2.5f
    }

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    private fun initGame() {
        player = Player(screenWidth / 2f, screenHeight / 2f)
        enemies.clear()
        bullets.clear()
        pickups.clear()
        particleSystem.clear()
        wave = 1
        score = 0
        multiplier = 1
        isGameOver = false
        prepareWave(wave)

        // Init Starfield background
        stars.clear()
        for (i in 0 until 120) {
            stars.add(Vector2D(random.nextFloat() * screenWidth, random.nextFloat() * screenHeight))
        }
    }

    private fun prepareWave(waveNumber: Int) {
        enemiesToSpawnInWave = 6 + (waveNumber * 4)
        waveSpawnTimer = 0.5f
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        screenWidth = width.toFloat()
        screenHeight = height.toFloat()
        initGame()

        gameLoop = GameLoop(this, holder)
        gameLoop?.startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
        screenWidth = w.toFloat()
        screenHeight = h.toFloat()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        gameLoop?.stopLoop()
        while (retry) {
            try {
                gameLoop?.join()
                retry = false
            } catch (_: InterruptedException) {}
        }
        gameLoop = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        val index = event.actionIndex
        val pointerId = event.getPointerId(index)
        val touchX = event.getX(index)
        val touchY = event.getY(index)

        if (isGameOver) {
            if (action == MotionEvent.ACTION_DOWN) {
                initGame()
            }
            return true
        }

        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (touchX < screenWidth * 0.5f) {
                    // Left half: Virtual floating joystick
                    if (moveTouchPointerId == -1) {
                        moveTouchPointerId = pointerId
                        joyBase.set(touchX, touchY)
                        joyCurrent.set(touchX, touchY)
                        joyActive = true
                        moveVector.set(0f, 0f)
                    }
                } else {
                    // Right half: Aiming & continuous shooting
                    if (aimTouchPointerId == -1) {
                        aimTouchPointerId = pointerId
                        isAiming = true
                        currentAimAngle = atan2(touchY - player.position.y, touchX - player.position.x)
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val pId = event.getPointerId(i)
                    val px = event.getX(i)
                    val py = event.getY(i)
                    if (pId == moveTouchPointerId) {
                        joyCurrent.set(px, py)
                        val dx = px - joyBase.x
                        val dy = py - joyBase.y
                        val dist = hypot(dx, dy)
                        if (dist > 0.001f) {
                            val clampedDist = dist.coerceAtMost(joyRadius)
                            moveVector.set(dx / dist * (clampedDist / joyRadius), dy / dist * (clampedDist / joyRadius))
                        }
                    } else if (pId == aimTouchPointerId) {
                        isAiming = true
                        currentAimAngle = atan2(py - player.position.y, px - player.position.x)
                    }
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (pointerId == moveTouchPointerId) {
                    moveTouchPointerId = -1
                    joyActive = false
                    moveVector.set(0f, 0f)
                }
                if (pointerId == aimTouchPointerId) {
                    aimTouchPointerId = -1
                    isAiming = false
                }
            }
        }
        return true
    }

    fun updatePhysics(dt: Float) {
        if (isGameOver) {
            particleSystem.update(dt)
            return
        }

        // Player movement & update
        player.update(dt, moveVector, screenWidth, screenHeight)

        // Firing weapon
        if (isAiming) {
            val fired = player.tryFire(currentAimAngle)
            if (fired != null) {
                bullets.addAll(fired)
                soundManager.playLaserSound()
            }
        }

        // Wave spawner director
        waveDirectorTimer += dt
        if (enemiesToSpawnInWave > 0) {
            waveSpawnTimer -= dt
            if (waveSpawnTimer <= 0f) {
                waveSpawnTimer = 0.8f.coerceAtLeast(1.8f - (wave * 0.1f))
                spawnEnemyForWave(wave)
                enemiesToSpawnInWave--
            }
        } else if (enemies.isEmpty()) {
            // Wave completed: Level up!
            wave++
            multiplier++
            soundManager.playPickupSound()
            prepareWave(wave)
        }

        // Update Bullets
        val bIt = bullets.iterator()
        while (bIt.hasNext()) {
            val bullet = bIt.next()
            bullet.update(dt, screenWidth, screenHeight)
            if (!bullet.isAlive) {
                bIt.remove()
            }
        }

        // Update Pickups
        val pIt = pickups.iterator()
        while (pIt.hasNext()) {
            val pickup = pIt.next()
            pickup.update(dt)
            if (pickup.position.distanceTo(player.position) < player.radius + pickup.radius) {
                when (pickup.type) {
                    PickupType.HEALTH -> player.health = (player.health + 35f).coerceAtMost(player.maxHealth)
                    PickupType.SHIELD -> player.shield = player.maxShield
                    PickupType.MULTI_SHOT -> player.multiShotTimer = 10f
                }
                soundManager.playPickupSound()
                pickup.isAlive = false
            }
            if (!pickup.isAlive) {
                pIt.remove()
            }
        }

        // Update Enemies
        val newBullets = mutableListOf<Bullet>()
        val newMines = mutableListOf<Enemy>()
        val eIt = enemies.iterator()
        while (eIt.hasNext()) {
            val enemy = eIt.next()
            enemy.update(dt, player.position, enemies, screenWidth, screenHeight,
                onSpawnBullet = { newBullets.add(it) },
                onSpawnMine = { newMines.add(it) }
            )

            // Check collision with Player
            if (enemy.position.distanceTo(player.position) < enemy.radius + player.radius) {
                val damage = if (enemy.type == EnemyType.MINE) 40f else 25f
                val dead = player.takeDamage(damage)
                particleSystem.triggerShipHit(player.position.x, player.position.y)
                soundManager.playPlayerHitSound()
                enemy.isAlive = false
                if (dead) {
                    isGameOver = true
                    particleSystem.triggerExplosion(player.position.x, player.position.y, Color.CYAN, 90, 600f)
                    soundManager.playExplosionSound()
                }
            }

            // Check Bullet Collisions
            for (bullet in bullets) {
                if (bullet.isPlayerBullet && bullet.isAlive) {
                    if (bullet.position.distanceTo(enemy.position) < enemy.radius + bullet.radius) {
                        bullet.isAlive = false
                        val killed = enemy.takeDamage(bullet.damage)
                        if (killed) {
                            onEnemyDestroyed(enemy)
                        } else {
                            particleSystem.triggerShipHit(bullet.position.x, bullet.position.y, 6)
                        }
                        break
                    }
                } else if (!bullet.isPlayerBullet && bullet.isAlive) {
                    if (bullet.position.distanceTo(player.position) < player.radius + bullet.radius) {
                        bullet.isAlive = false
                        val dead = player.takeDamage(bullet.damage)
                        particleSystem.triggerShipHit(bullet.position.x, bullet.position.y, 10)
                        soundManager.playPlayerHitSound()
                        if (dead) {
                            isGameOver = true
                            particleSystem.triggerExplosion(player.position.x, player.position.y, Color.CYAN, 90, 600f)
                            soundManager.playExplosionSound()
                        }
                    }
                }
            }

            if (!enemy.isAlive) {
                eIt.remove()
            }
        }

        bullets.addAll(newBullets)
        enemies.addAll(newMines)

        // Update Particles
        particleSystem.update(dt)
    }

    private fun onEnemyDestroyed(enemy: Enemy) {
        val explosionColor = when (enemy.type) {
            EnemyType.DRONE -> Color.RED
            EnemyType.INTERCEPTOR -> Color.MAGENTA
            EnemyType.DREADNOUGHT -> Color.rgb(255, 140, 0)
            EnemyType.MINE -> Color.YELLOW
        }
        val particleCount = if (enemy.type == EnemyType.DREADNOUGHT) 85 else 45
        particleSystem.triggerExplosion(enemy.position.x, enemy.position.y, explosionColor, particleCount)
        soundManager.playExplosionSound()

        val points = when (enemy.type) {
            EnemyType.DRONE -> 100
            EnemyType.INTERCEPTOR -> 250
            EnemyType.DREADNOUGHT -> 1000
            EnemyType.MINE -> 50
        }
        score += points * multiplier

        // Chance to spawn pickups
        val dropRoll = random.nextFloat()
        if (dropRoll < 0.18f) {
            val pickupType = when (random.nextInt(3)) {
                0 -> PickupType.HEALTH
                1 -> PickupType.SHIELD
                else -> PickupType.MULTI_SHOT
            }
            pickups.add(Pickup(enemy.position.copy(), pickupType))
        }
    }

    private fun spawnEnemyForWave(waveNum: Int) {
        // Spawn just outside the screen border
        val side = random.nextInt(4)
        val spawnPos = when (side) {
            0 -> Vector2D(random.nextFloat() * screenWidth, -30f) // Top
            1 -> Vector2D(screenWidth + 30f, random.nextFloat() * screenHeight) // Right
            2 -> Vector2D(random.nextFloat() * screenWidth, screenHeight + 30f) // Bottom
            else -> Vector2D(-30f, random.nextFloat() * screenHeight) // Left
        }

        val roll = random.nextFloat()
        val type = when {
            waveNum >= 3 && roll < 0.2f -> EnemyType.DREADNOUGHT
            waveNum >= 2 && roll < 0.5f -> EnemyType.INTERCEPTOR
            else -> EnemyType.DRONE
        }

        enemies.add(Enemy(spawnPos, type))
    }

    fun renderFrame(canvas: Canvas) {
        // Background fill
        canvas.drawColor(Color.rgb(8, 8, 16))

        // Apply camera shake if any
        if (particleSystem.screenShakeAmount > 0.01f) {
            val shakeX = (random.nextFloat() * 2f - 1f) * particleSystem.screenShakeAmount
            val shakeY = (random.nextFloat() * 2f - 1f) * particleSystem.screenShakeAmount
            canvas.translate(shakeX, shakeY)
        }

        // Render Stars
        for (star in stars) {
            canvas.drawPoint(star.x, star.y, starPaint)
        }

        // Render Pickups
        for (p in pickups) {
            p.render(canvas, hudPaint)
        }

        // Render Bullets
        for (b in bullets) {
            b.render(canvas, hudPaint)
        }

        // Render Enemies
        for (e in enemies) {
            e.render(canvas, hudPaint)
        }

        // Render Particles
        particleSystem.render(canvas)

        // Render Player
        if (!isGameOver) {
            player.render(canvas, hudPaint)
        }

        // Render Floating Joystick Controls
        if (joyActive) {
            joyPaint.color = Color.argb(80, 0, 255, 255)
            canvas.drawCircle(joyBase.x, joyBase.y, joyRadius, joyPaint)
            joyPaint.color = Color.argb(180, 0, 255, 255)
            val knobDist = joyBase.distanceTo(joyCurrent).coerceAtMost(joyRadius)
            val dir = Vector2D(joyCurrent.x - joyBase.x, joyCurrent.y - joyBase.y)
            if (dir.length() > 0.001f) dir.normalize().multiply(knobDist)
            canvas.drawCircle(joyBase.x + dir.x, joyBase.y + dir.y, 35f, joyPaint)
        }

        // Render HUD (Health, Shield, Multiplier, Wave, Score)
        renderHUD(canvas)
    }

    private fun renderHUD(canvas: Canvas) {
        // Health Bar
        hudPaint.style = Paint.Style.STROKE
        hudPaint.strokeWidth = 3f
        hudPaint.color = Color.DKGRAY
        val barW = 240f
        val barH = 18f
        val left = 40f
        var top = 40f

        // Health
        canvas.drawRect(left, top, left + barW, top + barH, hudPaint)
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.GREEN
        val currentHealthW = (player.health / player.maxHealth).coerceIn(0f, 1f) * barW
        canvas.drawRect(left, top, left + currentHealthW, top + barH, hudPaint)

        // Shield
        top += 28f
        hudPaint.style = Paint.Style.STROKE
        hudPaint.color = Color.DKGRAY
        canvas.drawRect(left, top, left + barW, top + barH, hudPaint)
        hudPaint.style = Paint.Style.FILL
        hudPaint.color = Color.CYAN
        val currentShieldW = (player.shield / player.maxShield).coerceIn(0f, 1f) * barW
        canvas.drawRect(left, top, left + currentShieldW, top + barH, hudPaint)

        // Text Stats
        textPaint.textSize = 34f
        textPaint.color = Color.CYAN
        canvas.drawText("SCORE: $score (x$multiplier)", left, top + 52f, textPaint)
        canvas.drawText("WAVE $wave", screenWidth - 220f, 60f, textPaint)

        if (player.multiShotTimer > 0f) {
            textPaint.color = Color.YELLOW
            textPaint.textSize = 28f
            canvas.drawText("MULTI-SHOT: ${String.format("%.1f", player.multiShotTimer)}s", left, top + 90f, textPaint)
        }

        if (isGameOver) {
            textPaint.textSize = 68f
            textPaint.color = Color.RED
            val goText = "SYSTEM COMPROMISED"
            val subText = "FINAL SCORE: $score - TAP TO REBOOT"
            val w1 = textPaint.measureText(goText)
            canvas.drawText(goText, (screenWidth - w1) / 2f, screenHeight / 2f - 40f, textPaint)

            textPaint.textSize = 36f
            textPaint.color = Color.WHITE
            val w2 = textPaint.measureText(subText)
            canvas.drawText(subText, (screenWidth - w2) / 2f, screenHeight / 2f + 40f, textPaint)
        }
    }
}
