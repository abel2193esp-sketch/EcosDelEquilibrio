
package com.ecosdelequilibrio.game

import android.app.Activity
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Path
import android.content.Context
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

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)

    // Jugador y mundo.
    private var x = 120f
    private var jumpY = 0f
    private var velocity = 0f
    private var world = 0f
    private var facing = 1f

    private var health = 5
    private var bossHealth = 10
    private var bossX = 700f

    private var light = true
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
    private var joystickXValue = 0f
    private var joystickYValue = 0f

    // Identificadores de cada dedo.
    private var jumpPointerId = -1
    private var attackPointerId = -1
    private var formPointerId = -1
    private var skillPointerId = -1

    private val scale: Float
        get() = width / 960f

    private val ground: Float
        get() = height * 0.78f

    private fun platformX(i: Int) = 220f + i * 210f

    private fun overlapsPlatform(i: Int): Boolean {
        val start = platformX(i)
        return x + 15f > start && x - 15f < start + 100f
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
        x = 120f
        jumpY = 0f
        velocity = 0f
        world = 0f
        facing = 1f

        health = 5
        bossHealth = 10
        bossX = 700f

        light = true
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
        joystickXValue = 0f
        joystickYValue = 0f

        jumpPointerId = -1
        attackPointerId = -1
        formPointerId = -1
        skillPointerId = -1
    }

    private fun jump() {
        if (won || lost) return

        if (jumpCount < 2) {
            velocity = -13f
            grounded = false
            jumpCount++
            crouching = false
        }
    }

    private fun attack() {
        if (won || lost) return

        attackFlash = 9

        val distance = bossX - x
        val facingBoss = distance * facing > 0f

        if (
            bossHealth > 0 &&
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

    // Habilidad contextual: luz = proyectil, oscuridad = escudo.
    private fun useSkill() {
        if (won || lost) return

        if (light) {
            projectileFlash = 12

            val distance = bossX - x
            val facingBoss = distance * facing > 0f

            if (
                bossHealth > 0 &&
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
            shieldTimer = 100
        }
    }

    private fun updateGame() {
        if (width == 0 || won || lost) return

        if (invulnerability > 0) invulnerability--
        if (attackFlash > 0) attackFlash--
        if (shieldTimer > 0) shieldTimer--
        if (projectileFlash > 0) projectileFlash--

        // Movimiento horizontal del joystick.
        val speed = if (crouching) 1.8f else 4f
        x += joystickXValue * speed

        if (joystickXValue < -0.15f) facing = -1f
        if (joystickXValue > 0.15f) facing = 1f

        x = x.coerceIn(20f, 1800f)

        crouching = grounded && joystickYValue > 0.55f

        // Si abandonamos una plataforma, empezamos a caer.
        if (grounded && jumpY < 0f && !standingOnPlatform()) {
            grounded = false
            jumpCount = 1
        }

        val previousY = jumpY

        if (!grounded) {
            velocity += 0.58f
            jumpY += velocity

            // Aterrizaje sobre plataformas.
            if (velocity > 0f) {
                for (i in 0..7) {
                    val platformTop = -100f

                    if (
                        previousY <= platformTop &&
                        jumpY >= platformTop &&
                        overlapsPlatform(i)
                    ) {
                        jumpY = platformTop
                        velocity = 0f
                        grounded = true
                        jumpCount = 0
                        break
                    }
                }
            }

            // Aterrizaje en el suelo.
            if (jumpY >= 0f) {
                jumpY = 0f
                velocity = 0f
                grounded = true
                jumpCount = 0
            }
        }

        world = (x - 300f).coerceIn(0f, 900f)

        // El Guardián ataca cuando estás cerca.
        if (bossHealth > 0 && abs(x - bossX) < 95f) {
            bossAttackTimer++

            if (bossAttackTimer >= 75) {
                bossAttackTimer = 0

                if (invulnerability == 0 && shieldTimer == 0) {
                    health--
                    invulnerability = 65

                    if (health <= 0) {
                        health = 0
                        lost = true
                    }
                }
            }
        } else {
            bossAttackTimer = 0
        }
    }

    private fun drawGame(c: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = scale

        c.drawColor(
            if (light) Color.rgb(17, 27, 53)
            else Color.rgb(43, 17, 53)
        )

        // Estrellas.
        p.color = Color.rgb(190, 210, 255)

        for (i in 0..35) {
            val raw = (i * 137f - world * 0.12f) % (w + 30f)
            val sx = if (raw < 0f) raw + w + 30f else raw
            val sy = i * 47f % (h * 0.60f)

            c.drawCircle(sx, sy, 1.5f * s, p)
        }

        // Montañas.
        p.color = Color.rgb(42, 51, 83)

        for (i in 0..9) {
            val bx = i * 170f * s -
                (world * s * 0.2f % (170f * s))

            val path = Path()
            path.moveTo(bx, ground - 20f * s)
            path.lineTo(bx + 85f * s, h * 0.25f)
            path.lineTo(bx + 170f * s, ground - 20f * s)
            path.close()

            c.drawPath(path, p)
        }

        // Suelo.
        p.color = Color.rgb(43, 61, 67)
        c.drawRect(0f, ground, w, h, p)

        p.color = Color.rgb(93, 200, 165)
        c.drawRect(0f, ground, w, ground + 6f * s, p)

        // Plataformas.
        for (i in 0..7) {
            val px = (platformX(i) - world) * s
            val py = ground - 100f * s

            if (px > -130f * s && px < w) {
                p.color = Color.rgb(80, 105, 117)

                c.drawRoundRect(
                    px, py, px + 100f * s, py + 15f * s,
                    3f * s, 3f * s, p
                )

                p.color = Color.rgb(93, 200, 165)
                c.drawRect(
                    px, py, px + 100f * s, py + 3f * s, p
                )
            }
        }

        // Cristales.
        p.color = if (light) Color.CYAN else Color.MAGENTA

        for (i in 0..9) {
            val cx = (100f + i * 190f - world) * s

            if (cx in 0f..w) {
                val cy = ground - 35f * s
                val crystal = Path()

                crystal.moveTo(cx, cy - 8f * s)
                crystal.lineTo(cx + 5f * s, cy)
                crystal.lineTo(cx, cy + 8f * s)
                crystal.lineTo(cx - 5f * s, cy)
                crystal.close()

                c.drawPath(crystal, p)
            }
        }

        // Jugador.
        val px = (x - world) * s
        val py = ground + jumpY * s

        if (invulnerability % 8 < 5 || invulnerability == 0) {
            p.color = if (light) {
                Color.rgb(91, 211, 255)
            } else {
                Color.rgb(212, 111, 255)
            }

            val bodyHeight = if (crouching) 30f else 48f

            c.drawRoundRect(
                px - 15f * s,
                py - bodyHeight * s,
                px + 15f * s,
                py,
                7f * s, 7f * s, p
            )

            p.color = Color.WHITE
            c.drawCircle(
                px + facing * 6f * s,
                py - bodyHeight * 0.7f * s,
                3f * s, p
            )

            // Golpe de espada.
            if (attackFlash > 0) {
                p.color = Color.rgb(255, 245, 170)

                val swordX = px + facing * 32f * s

                c.drawRoundRect(
                    min(px, swordX), py - 35f * s,
                    max(px, swordX) + 5f * s, py - 29f * s,
                    2f * s, 2f * s, p
                )
            }

            // Efecto del proyectil de luz.
            if (projectileFlash > 0 && light) {
                p.color = Color.CYAN

                c.drawCircle(
                    px + facing * (45f + (12 - projectileFlash) * 12f) * s,
                    py - 30f * s,
                    8f * s, p
                )
            }

            // Escudo de oscuridad.
            if (shieldTimer > 0 && !light) {
                p.color = Color.argb(170, 190, 70, 255)
                c.drawCircle(px, py - 25f * s, 38f * s, p)

                p.color = Color.WHITE
                c.drawCircle(px, py - 25f * s, 40f * s, p)
            }
        }

        // Guardián.
        if (bossHealth > 0) {
            val bx = (bossX - world) * s

            p.color = Color.rgb(111, 55, 132)

            c.drawRoundRect(
                bx - 32f * s, ground - 90f * s,
                bx + 32f * s, ground,
                14f * s, 14f * s, p
            )

            p.color = Color.rgb(255, 74, 130)
            c.drawCircle(bx - 10f * s, ground - 65f * s, 4f * s, p)
            c.drawCircle(bx + 10f * s, ground - 65f * s, 4f * s, p)

            // Barra de vida del jefe.
            val barLeft = w - 190f * s
            val barTop = 20f * s
            val barWidth = 170f * s

            p.color = Color.DKGRAY
            c.drawRoundRect(
                barLeft, barTop, barLeft + barWidth,
                barTop + 13f * s, 3f * s, 3f * s, p
            )

            p.color = Color.RED
            c.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth * bossHealth / 10f,
                barTop + 13f * s, 3f * s, 3f * s, p
            )
        }

        // Información.
        p.color = Color.WHITE
        p.textSize = 16f * s
        c.drawText("ECOS DEL EQUILIBRIO", 16f * s, 25f * s, p)
        c.drawText("VIDA: $health / 5", 16f * s, 48f * s, p)

        c.drawText(
            if (light) "FORMA: LUZ" else "FORMA: OSCURIDAD",
            16f * s, 70f * s, p
        )

        // Joystick de 360 grados.
        val joyX = 90f * s
        val joyY = h - 60f * s
        val joyRadius = 48f * s

        p.color = Color.argb(155, 22, 36, 55)
        c.drawCircle(joyX, joyY, joyRadius, p)

        p.color = Color.argb(210, 100, 145, 175)
        c.drawCircle(
            joyX + joystickXValue * 30f * s,
            joyY + joystickYValue * 30f * s,
            20f * s, p
        )

        p.color = Color.WHITE
        p.textSize = 10f * s
        p.textAlign = Paint.Align.CENTER
        c.drawText("MOVER", joyX, joyY + 62f * s, p)
        p.textAlign = Paint.Align.LEFT

        // Cuatro botones juntos a la derecha.
        val buttonY1 = h - 82f * s
        val buttonY2 = h - 14f * s
        val buttonWidth = 72f * s
        val gap = 8f * s
        val rightEdge = w - 12f * s

        val skillRect = RectF(
            rightEdge - buttonWidth,
            buttonY1,
            rightEdge,
            buttonY2
        )

        val formRect = RectF(
            skillRect.left - gap - buttonWidth,
            buttonY1,
            skillRect.left - gap,
            buttonY2
        )

        val attackRect = RectF(
            formRect.left - gap - buttonWidth,
            buttonY1,
            formRect.left - gap,
            buttonY2
        )

        val jumpRect = RectF(
            attackRect.left - gap - buttonWidth,
            buttonY1,
            attackRect.left - gap,
            buttonY2
        )

        drawButton(c, jumpRect, "SALTO", Color.rgb(60, 180, 230))
        drawButton(c, attackRect, "ATAQUE", Color.rgb(230, 75, 120))
        drawButton(
            c, formRect,
            if (light) "OSCUR." else "LUZ",
            Color.rgb(160, 90, 220)
        )
        drawButton(
            c, skillRect,
            if (light) "PROY." else "ESCUDO",
            Color.rgb(70, 160, 140)
        )

        // Pantalla final.
        if (won || lost) {
            p.color = Color.argb(225, 8, 12, 25)
            c.drawRect(0f, 0f, w, h, p)

            p.color = if (won) {
                Color.rgb(100, 255, 190)
            } else {
                Color.rgb(255, 100, 120)
            }

            p.textAlign = Paint.Align.CENTER
            p.textSize = 29f * s

            c.drawText(
                if (won) "¡GUARDIÁN DERROTADO!" else "HAS CAÍDO",
                w / 2f, h * 0.43f, p
            )

            p.color = Color.WHITE
            p.textSize = 17f * s
            c.drawText(
                "TOCA PARA VOLVER A JUGAR",
                w / 2f, h * 0.53f, p
            )

            p.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawButton(
        c: Canvas,
        r: RectF,
        label: String,
        color: Int
    ) {
        p.color = Color.argb(210, 22, 36, 55)
        c.drawRoundRect(r, 12f * scale, 12f * scale, p)

        p.color = color
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f * scale
        c.drawRoundRect(r, 12f * scale, 12f * scale, p)
        p.style = Paint.Style.FILL

        p.color = Color.WHITE
        p.textSize = 10f * scale
        p.textAlign = Paint.Align.CENTER

        c.drawText(
            label,
            r.centerX(),
            r.centerY() + 4f * scale,
            p
        )

        p.textAlign = Paint.Align.LEFT
    }

    private fun updateJoystick(tx: Float, ty: Float) {
        val s = scale
        val joyX = 90f * s
        val joyY = height - 60f * s
        val radius = 48f * s

        var dx = tx - joyX
        var dy = ty - joyY

        val distance = sqrt(dx * dx + dy * dy)

        if (distance > radius && distance > 0f) {
            dx = dx / distance * radius
            dy = dy / distance * radius
        }

        joystickXValue = (dx / radius).coerceIn(-1f, 1f)
        joystickYValue = (dy / radius).coerceIn(-1f, 1f)
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = scale
        val w = width.toFloat()
        val h = height.toFloat()

        val joyX = 90f * s
        val joyY = h - 60f * s
        val joyRadius = 65f * s

        val buttonY1 = h - 82f * s
        val buttonY2 = h - 14f * s
        val buttonWidth = 72f * s
        val gap = 8f * s
        val rightEdge = w - 12f * s

        val skillRect = RectF(
            rightEdge - buttonWidth,
            buttonY1,
            rightEdge,
            buttonY2
        )

        val formRect = RectF(
            skillRect.left - gap - buttonWidth,
            buttonY1,
            skillRect.left - gap,
            buttonY2
        )

        val attackRect = RectF(
            formRect.left - gap - buttonWidth,
            buttonY1,
            formRect.left - gap,
            buttonY2
        )

        val jumpRect = RectF(
            attackRect.left - gap - buttonWidth,
            buttonY1,
            attackRect.left - gap,
            buttonY2
        )

        fun handleDown(index: Int) {
            val tx = e.getX(index)
            val ty = e.getY(index)
            val id = e.getPointerId(index)

            if (won || lost) {
                resetGame()
                return
            }

            val dx = tx - joyX
            val dy = ty - joyY

            if (
                joystickPointerId == -1 &&
                dx * dx + dy * dy <= joyRadius * joyRadius
            ) {
                joystickPointerId = id
                updateJoystick(tx, ty)
                return
            }

            if (jumpRect.contains(tx, ty)) {
                jumpPointerId = id
                jump()
                return
            }

            if (attackRect.contains(tx, ty)) {
                attackPointerId = id
                attack()
                return
            }

            if (formRect.contains(tx, ty)) {
                formPointerId = id
                light = !light
                return
            }

            if (skillRect.contains(tx, ty)) {
                skillPointerId = id
                useSkill()
            }
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                handleDown(e.actionIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                if (joystickPointerId != -1) {
                    val index = e.findPointerIndex(joystickPointerId)

                    if (index >= 0) {
                        updateJoystick(e.getX(index), e.getY(index))
                    }
                }
            }

            MotionEvent.ACTION_POINTER_UP,
            MotionEvent.ACTION_UP -> {
                val id = e.getPointerId(e.actionIndex)

                if (id == joystickPointerId) {
                    joystickPointerId = -1
                    joystickXValue = 0f
                    joystickYValue = 0f
                }

                if (id == jumpPointerId) jumpPointerId = -1
                if (id == attackPointerId) attackPointerId = -1
                if (id == formPointerId) formPointerId = -1
                if (id == skillPointerId) skillPointerId = -1

                if (e.actionMasked == MotionEvent.ACTION_UP) {
                    joystickPointerId = -1
                    joystickXValue = 0f
                    joystickYValue = 0f

                    jumpPointerId = -1
                    attackPointerId = -1
                    formPointerId = -1
                    skillPointerId = -1
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                joystickPointerId = -1
                joystickXValue = 0f
                joystickYValue = 0f

                jumpPointerId = -1
                if (id == attackPointerId) attackPointerId = -1
