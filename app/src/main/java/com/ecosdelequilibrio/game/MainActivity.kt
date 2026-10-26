
package com.ecosdelequilibrio.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class MainActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

        setContentView(GameView(this))
    }
}

private class GameView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var playerX = 120f
    private var jumpY = 0f
    private var velocityY = 0f
    private var worldX = 0f
    private var facing = 1f

    private var playerHealth = 5
    private var bossHealth = 10
    private var bossX = 700f

    private var lightMode = true
    private var grounded = true
    private var crouching = false
    private var jumpCount = 0

    private var won = false
    private var lost = false

    private var invulnerability = 0
    private var bossAttackTimer = 0
    private var attackFlash = 0
    private var shieldTimer = 0
    private var projectileFlash = 0

    // Joystick analógico de 360 grados.
    private var joystickPointerId = -1
    private var joystickDX = 0f
    private var joystickDY = 0f

    // Un identificador independiente por botón.
    private var jumpPointerId = -1
    private var attackPointerId = -1
    private var formPointerId = -1
    private var skillPointerId = -1

    private val scale: Float
        get() = width / 960f

    private val ground: Float
        get() = height * 0.78f

    private fun platformX(index: Int): Float {
        return 220f + index * 210f
    }

    private fun overlapsPlatform(index: Int): Boolean {
        val start = platformX(index)
        return playerX + 15f > start &&
            playerX - 15f < start + 100f
    }

    private fun standingOnPlatform(): Boolean {
        if (jumpY >= 0f) return false

        for (i in 0..7) {
            if (overlapsPlatform(i)) return true
        }

        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        updateGame()
        drawGame(canvas)
        postInvalidateDelayed(16)
    }

    private fun resetGame() {
        playerX = 120f
        jumpY = 0f
        velocityY = 0f
        worldX = 0f
        facing = 1f

        playerHealth = 5
        bossHealth = 10
        bossX = 700f

        lightMode = true
        grounded = true
        crouching = false
        jumpCount = 0

        won = false
        lost = false

        invulnerability = 0
        bossAttackTimer = 0
        attackFlash = 0
        shieldTimer = 0
        projectileFlash = 0

        joystickPointerId = -1
        joystickDX = 0f
        joystickDY = 0f

        jumpPointerId = -1
        attackPointerId = -1
        formPointerId = -1
        skillPointerId = -1
    }

    private fun jump() {
        if (won || lost) return

        if (jumpCount < 2) {
            velocityY = -13f
            grounded = false
            jumpCount++
            crouching = false
        }
    }

    private fun attack() {
        if (won || lost) return

        attackFlash = 9

        val distance = bossX - playerX
        val facingBoss = distance * facing > 0f

        if (bossHealth > 0 &&
            abs(distance) < 115f &&
            facingBoss
        ) {
            bossHealth--

            if (bossHealth <= 0) {
                bossHealth = 0
                won = true
            }
        }
    }

    private fun useSkill() {
        if (won || lost) return

        if (lightMode) {
            // Proyectil de luz.
            projectileFlash = 12

            val distance = bossX - playerX
            val facingBoss = distance * facing > 0f

            if (bossHealth > 0 &&
                abs(distance) < 350f &&
                facingBoss
            ) {
                bossHealth -= 2

                if (bossHealth <= 0) {
                    bossHealth = 0
                    won = true
                }
            }
        } else {
            // Escudo temporal de oscuridad.
            shieldTimer = 100
        }
    }

    private fun updateGame() {
        if (width == 0 || won || lost) return

        if (invulnerability > 0) invulnerability--
        if (attackFlash > 0) attackFlash--
        if (shieldTimer > 0) shieldTimer--
        if (projectileFlash > 0) projectileFlash--

        // Movimiento en horizontal.
        val movementSpeed = if (crouching) 1.8f else 4f

        playerX += joystickDX * movementSpeed
        playerX = playerX.coerceIn(20f, 1800f)

        if (joystickDX < -0.15f) facing = -1f
        if (joystickDX > 0.15f) facing = 1f

        crouching = grounded && joystickDY > 0.65f

        // Caer cuando se abandona una plataforma.
        if (grounded && jumpY < 0f && !standingOnPlatform()) {
            grounded = false
            jumpCount = 1
        }

        val previousY = jumpY

        if (!grounded) {
            velocityY += 0.58f
            jumpY += velocityY

            if (velocityY > 0f) {
                for (i in 0..7) {
                    val platformTop = -100f

                    if (previousY <= platformTop &&
                        jumpY >= platformTop &&
                        overlapsPlatform(i)
                    ) {
                        jumpY = platformTop
                        velocityY = 0f
                        grounded = true
                        jumpCount = 0
                        break
                    }
                }
            }

            if (jumpY >= 0f) {
                jumpY = 0f
                velocityY = 0f
                grounded = true
                jumpCount = 0
            }
        }

        worldX = (playerX - 300f).coerceIn(0f, 900f)

        // Ataque del Guardián.
        if (bossHealth > 0 &&
            abs(playerX - bossX) < 95f
        ) {
            bossAttackTimer++

            if (bossAttackTimer >= 75) {
                bossAttackTimer = 0

                if (invulnerability == 0 && shieldTimer == 0) {
                    playerHealth--
                    invulnerability = 65

                    if (playerHealth <= 0) {
                        playerHealth = 0
                        lost = true
                    }
                }
            }
        } else {
            bossAttackTimer = 0
        }
    }

    private fun drawGame(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = scale

        canvas.drawColor(
            if (lightMode) Color.rgb(17, 27, 53)
            else Color.rgb(43, 17, 53)
        )

        // Estrellas.
        paint.color = Color.rgb(190, 210, 255)

        for (i in 0..35) {
            val raw = (i * 137f - worldX * 0.12f) % (w + 30f)
            val starX = if (raw < 0f) raw + w + 30f else raw
            val starY = i * 47f % (h * 0.60f)

            canvas.drawCircle(starX, starY, 1.5f * s, paint)
        }

        // Montañas.
        paint.color = Color.rgb(42, 51, 83)

        for (i in 0..9) {
            val bx = i * 170f * s -
                (worldX * s * 0.2f % (170f * s))

            val path = Path()
            path.moveTo(bx, ground - 20f * s)
            path.lineTo(bx + 85f * s, h * 0.25f)
            path.lineTo(bx + 170f * s, ground - 20f * s)
            path.close()

            canvas.drawPath(path, paint)
        }

        // Suelo.
        paint.color = Color.rgb(43, 61, 67)
        canvas.drawRect(0f, ground, w, h, paint)

        paint.color = Color.rgb(93, 200, 165)
        canvas.drawRect(0f, ground, w, ground + 6f * s, paint)

        // Plataformas.
        for (i in 0..7) {
            val px = (platformX(i) - worldX) * s
            val py = ground - 100f * s

            if (px > -130f * s && px < w) {
                paint.color = Color.rgb(80, 105, 117)

                canvas.drawRoundRect(
                    px, py, px + 100f * s, py + 15f * s,
                    3f * s, 3f * s, paint
                )

                paint.color = Color.rgb(93, 200, 165)
                canvas.drawRect(
                    px, py, px + 100f * s, py + 3f * s, paint
                )
            }
        }

        // Cristales.
        paint.color = if (lightMode) Color.CYAN else Color.MAGENTA

        for (i in 0..9) {
            val cx = (100f + i * 190f - worldX) * s

            if (cx in 0f..w) {
                val cy = ground - 35f * s
                val crystal = Path()

                crystal.moveTo(cx, cy - 8f * s)
                crystal.lineTo(cx + 5f * s, cy)
                crystal.lineTo(cx, cy + 8f * s)
                crystal.lineTo(cx - 5f * s, cy)
                crystal.close()

                canvas.drawPath(crystal, paint)
            }
        }

        // Personaje.
        val px = (playerX - worldX) * s
        val py = ground + jumpY * s

        if (invulnerability % 8 < 5 || invulnerability == 0) {
            paint.color = if (lightMode) {
                Color.rgb(91, 211, 255)
            } else {
                Color.rgb(212, 111, 255)
            }

            val bodyHeight = if (crouching) 30f else 48f

            canvas.drawRoundRect(
                px - 15f * s,
                py - bodyHeight * s,
                px + 15f * s,
                py,
                7f * s, 7f * s, paint
            )

            paint.color = Color.WHITE
            canvas.drawCircle(
                px + facing * 6f * s,
                py - bodyHeight * 0.7f * s,
                3f * s, paint
            )

            // Espada.
            if (attackFlash > 0) {
                paint.color = Color.rgb(255, 245, 170)
                val swordX = px + facing * 32f * s

                canvas.drawRoundRect(
                    min(px, swordX), py - 35f * s,
                    max(px, swordX) + 5f * s, py - 29f * s,
                    2f * s, 2f * s, paint
                )
            }

            // Proyectil.
            if (projectileFlash > 0 && lightMode) {
                paint.color = Color.CYAN

                canvas.drawCircle(
                    px + facing * (45f + (12 - projectileFlash) * 12f) * s,
                    py - 30f * s,
                    8f * s, paint
                )
            }

            // Escudo.
            if (shieldTimer > 0 && !lightMode) {
                paint.color = Color.argb(170, 190, 70, 255)
                canvas.drawCircle(px, py - 25f * s, 38f * s, paint)
            }
        }

        // Guardián.
        if (bossHealth > 0) {
            val bx = (bossX - worldX) * s

            paint.color = Color.rgb(111, 55, 132)

            canvas.drawRoundRect(
                bx - 32f * s, ground - 90f * s,
                bx + 32f * s, ground,
                14f * s, 14f * s, paint
            )

            paint.color = Color.rgb(255, 74, 130)
            canvas.drawCircle(bx - 10f * s, ground - 65f * s, 4f * s, paint)
            canvas.drawCircle(bx + 10f * s, ground - 65f * s, 4f * s, paint)

            val barLeft = w - 190f * s
            val barTop = 20f * s
            val barWidth = 170f * s

            paint.color = Color.DKGRAY
            canvas.drawRoundRect(
                barLeft, barTop, barLeft + barWidth,
                barTop + 13f * s, 3f * s, 3f * s, paint
            )

            paint.color = Color.RED
            canvas.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth * bossHealth / 10f,
                barTop + 13f * s, 3f * s, 3f * s, paint
            )
        }

        // HUD.
        paint.color = Color.WHITE
        paint.textSize = 16f * s
        canvas.drawText("ECOS DEL EQUILIBRIO", 16f * s, 25f * s, paint)
        canvas.drawText("VIDA: $playerHealth / 5", 16f * s, 48f * s, paint)

        canvas.drawText(
            if (lightMode) "FORMA: LUZ" else "FORMA: OSCURIDAD",
            16f * s, 70f * s, paint
        )

        // Joystick.
        val joyX = 90f * s
        val joyY = h - 60f * s

        paint.color = Color.argb(155, 22, 36, 55)
        canvas.drawCircle(joyX, joyY, 48f * s, paint)

        paint.color = Color.argb(210, 100, 145, 175)
        canvas.drawCircle(
            joyX + joystickDX * 30f * s,
            joyY + joystickDY * 30f * s,
            20f * s, paint
        )

        paint.color = Color.WHITE
        paint.textSize = 10f * s
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText("MOVER", joyX, joyY + 62f * s, paint)
        paint.textAlign = Paint.Align.LEFT

        // Botones alineados abajo a la derecha.
        val top = h - 82f * s
        val bottom = h - 14f * s
        val bw = 72f * s
        val gap = 8f * s
        val edge = w - 12f * s

        val skill = RectF(edge - bw, top, edge, bottom)
        val form = RectF(skill.left - gap - bw, top, skill.left - gap, bottom)
        val attack = RectF(form.left - gap - bw, top, form.left - gap, bottom)
        val jump = RectF(attack.left - gap - bw, top, attack.left - gap, bottom)

        drawButton(canvas, jump, "SALTO", Color.rgb(60, 180, 230))
        drawButton(canvas, attack, "ATAQUE", Color.rgb(230, 75, 120))
        drawButton(canvas, form, if (lightMode) "OSCUR." else "LUZ", Color.rgb(160, 90, 220))
        drawButton(canvas, skill, if (lightMode) "PROY." else "ESCUDO", Color.rgb(70, 160, 140))

        if (won || lost) {
            paint.color = Color.argb(225, 8, 12, 25)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.color = if (won) Color.rgb(100, 255, 190) else Color.rgb(255, 100, 120)
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 29f * s

            canvas.drawText(
                if (won) "¡GUARDIÁN DERROTADO!" else "HAS CAÍDO",
                w / 2f, h * 0.43f, paint
            )

            paint.color = Color.WHITE
            paint.textSize = 17f * s
            canvas.drawText("TOCA PARA VOLVER A JUGAR", w / 2f, h * 0.53f, paint)
            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, color: Int) {
        paint.color = Color.argb(210, 22, 36, 55)
        canvas.drawRoundRect(rect, 12f * scale, 12f * scale, paint)

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        canvas.drawRoundRect(rect, 12f * scale, 12f * scale, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.WHITE
        paint.textSize = 10f * scale
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, rect.centerX(), rect.centerY() + 4f * scale, paint)
        paint.textAlign = Paint.Align.LEFT
    }

    private fun updateJoystick(tx: Float, ty: Float) {
        val radius = 48f * scale
        val joyX = 90f * scale
        val joyY = height - 60f * scale

        var dx = tx - joyX
        var dy = ty - joyY

        val distance = sqrt(dx * dx + dy * dy)

        if (distance > radius && distance > 0f) {
            dx = dx / distance * radius
            dy = dy / distance * radius
        }

        joystickDX = (dx / radius).coerceIn(-1f, 1f)
        joystickDY = (dy / radius).coerceIn(-1f, 1f)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = scale
        val w = width.toFloat()
        val h = height.toFloat()

        val joyX = 90f * s
        val joyY = h - 60f * s
        val joyRadius = 65f * s

        val top = h - 82f * s
        val bottom = h - 14f * s
        val bw = 72f * s
        val gap = 8f * s
        val edge = w - 12f * s

        val skill = RectF(edge - bw, top, edge, bottom)
        val form = RectF(skill.left - gap - bw, top, skill.left - gap, bottom)
        val attack = RectF(form.left - gap - bw, top, form.left - gap, bottom)
        val jump = RectF(attack.left - gap - bw, top, attack.left - gap, bottom)

        fun handleDown(index: Int) {
            val tx = event.getX(index)
            val ty = event.getY(index)
            val id = event.getPointerId(index)

            if (won || lost) {
                resetGame()
                return
            }

            val dx = tx - joyX
            val dy = ty - joyY

            if (joystickPointerId == -1 &&
                dx * dx + dy * dy <= joyRadius * joyRadius
            ) {
                joystickPointerId = id
                updateJoystick(tx, ty)
                return
            }

            if (jump.contains(tx, ty)) {
                jumpPointerId = id
                jump()
                return
            }

            if (attack.contains(tx, ty)) {
                attackPointerId = id
                attack()
                return
            }

            if (form.contains(tx, ty)) {
                formPointerId = id
                lightMode = !lightMode
                return
            }

            if (skill.contains(tx, ty)) {
                skillPointerId = id
                useSkill()
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                handleDown(event.actionIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                if (joystickPointerId != -1) {
                    val index = event.findPointerIndex(joystickPointerId)
                    if (index >= 0) {
                        updateJoystick(event.getX(index), event.getY(index))
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP -> {
                val id = event.getPointerId(event.actionIndex)

                if (id == joystickPointerId) {
                    joystickPointerId = -1
                    joystickDX = 0f
                    joystickDY = 0f
                }

                if (id == jumpPointerId) jumpPointerId = -1
                if (id == attackPointerId) attackPointerId = -1
                if (id == formPointerId) formPointerId = -1
                if (id == skillPointerId) skillPointerId = -1

                if (event.actionMasked == MotionEvent.ACTION_UP) {
                    joystickPointerId = -1
                    joystickDX = 0f
                    joystickDY = 0f

                    jumpPointerId = -1
                    attackPointerId = -1
                    formPointerId = -1
                    skillPointerId = -1
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                joystickPointerId = -1
                joystickDX = 0f
                joystickDY = 0f

                jumpPointerId = -1
                attackPointerId = -1
                formPointerId = -1
                skillPointerId = -1
            }
        }

        invalidate()
        return true
    }
}
