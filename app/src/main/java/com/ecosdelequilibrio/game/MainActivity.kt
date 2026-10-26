
package com.ecosdelequilibrio.game

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Path
import android.content.Context
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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

    private var x = 120f
    private var jumpY = 0f
    private var velocity = 0f
    private var health = 5
    private var bossHealth = 10
    private var bossX = 700f
    private var world = 0f
    private var facing = 1f

    private var light = true
    private var left = false
    private var right = false
    private var grounded = true
    private var attack = false
    private var won = false
    private var lost = false

    private var invulnerability = 0
    private var bossAttackTimer = 0
    private var attackFlash = 0

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

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

        updateGame()
        drawGame(c)

        postInvalidateDelayed(16)
    }

    private fun resetGame() {
        x = 120f
        jumpY = 0f
        velocity = 0f
        health = 5
        bossHealth = 10
        bossX = 700f
        world = 0f
        facing = 1f
        light = true
        grounded = true
        attack = false
        won = false
        lost = false
        invulnerability = 0
        bossAttackTimer = 0
        attackFlash = 0
    }

    private fun updateGame() {
        if (width == 0 || won || lost) return

        if (invulnerability > 0) invulnerability--
        if (attackFlash > 0) attackFlash--

        if (left) {
            x -= 4f
            facing = -1f
        }

        if (right) {
            x += 4f
            facing = 1f
        }

        x = x.coerceIn(20f, 1800f)

        // Si abandonas una plataforma, empiezas a caer.
        if (grounded && jumpY < 0f && !standingOnPlatform()) {
            grounded = false
        }

        val previousY = jumpY

        if (!grounded) {
            velocity += 0.58f
            jumpY += velocity

            // Aterrizaje sobre las plataformas.
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
                        break
                    }
                }
            }

            // Suelo principal.
            if (jumpY >= 0f) {
                jumpY = 0f
                velocity = 0f
                grounded = true
            }
        }

        // Cámara lateral.
        world = (x - 300f).coerceIn(0f, 900f)

        // Ataque del protagonista.
        if (attack) {
            attack = false
            attackFlash = 8

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

        // El Guardián golpea si te acercas demasiado.
        if (bossHealth > 0 && abs(x - bossX) < 95f) {
            bossAttackTimer++

            if (bossAttackTimer >= 75) {
                bossAttackTimer = 0

                if (invulnerability == 0) {
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
            val sx = ((i * 137f - world * 0.12f) %
                (w + 30f)).let { if (it < 0f) it + w + 30f else it }

            val sy = (i * 47f % (h * 0.60f))
            c.drawCircle(sx, sy, 1.5f * s, p)
        }

        // Montañas del fondo.
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

        // Plataformas sólidas.
        for (i in 0..7) {
            val px = (platformX(i) - world) * s
            val py = ground - 100f * s

            if (px > -130f * s && px < w) {
                p.color = Color.rgb(80, 105, 117)
                c.drawRoundRect(
                    px, py,
                    px + 100f * s, py + 15f * s,
                    3f * s, 3f * s, p
                )

                p.color = Color.rgb(93, 200, 165)
                c.drawRect(
                    px, py,
                    px + 100f * s, py + 3f * s, p
                )
            }
        }

        // Cristales del mundo.
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

        // Personaje.
        val px = (x - world) * s
        val py = ground + jumpY * s

        if (invulnerability % 8 < 5 || invulnerability == 0) {
            p.color = if (light) {
                Color.rgb(91, 211, 255)
            } else {
                Color.rgb(212, 111, 255)
            }

            c.drawRoundRect(
                px - 15f * s, py - 48f * s,
                px + 15f * s, py,
                7f * s, 7f * s, p
            )

            // Ojos.
            p.color = Color.WHITE
            c.drawCircle(
                px + facing * 6f * s,
                py - 36f * s,
                3f * s, p
            )

            // Espada al atacar.
            if (attackFlash > 0) {
                p.color = Color.rgb(255, 245, 170)

                val swordX = px + facing * 30f * s
                c.drawRoundRect(
                    min(px, swordX),
                    py - 35f * s,
                    max(px, swordX) + 5f * s,
                    py - 29f * s,
                    2f * s, 2f * s, p
                )
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
            c.drawCircle(
                bx - 10f * s, ground - 65f * s,
                4f * s, p
            )
            c.drawCircle(
                bx + 10f * s, ground - 65f * s,
                4f * s, p
            )

            // Barra de vida del jefe.
            val barLeft = w - 190f * s
            val barTop = 20f * s
            val barWidth = 170f * s

            p.color = Color.DKGRAY
            c.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth, barTop + 13f * s,
                3f * s, 3f * s, p
            )

            p.color = Color.RED
            c.drawRoundRect(
                barLeft, barTop,
                barLeft + barWidth * bossHealth / 10f,
                barTop + 13f * s,
                3f * s, 3f * s, p
            )
        }

        // Interfaz.
        p.color = Color.WHITE
        p.textSize = 16f * s

        c.drawText("ECOS DEL EQUILIBRIO", 16f * s, 25f * s, p)
        c.drawText("VIDA: $health / 5", 16f * s, 48f * s, p)

        c.drawText(
            if (light) "PODER: LUZ" else "PODER: OSCURIDAD",
            16f * s, 70f * s, p
        )

        button(
            c, RectF(15f*s, h-85f*s, 85f*s, h-15f*s), "IZQ"
        )
        button(
            c, RectF(95f*s, h-85f*s, 165f*s, h-15f*s), "DER"
        )
        button(
            c, RectF(w-190f*s, h-85f*s, w-115f*s, h-15f*s), "SALTO"
        )
        button(
            c, RectF(w-105f*s, h-85f*s, w-15f*s, h-15f*s), "ATACA"
        )
        button(
            c, RectF(w-175f*s, 45f*s, w-15f*s, 85f*s), "LUZ / OSC."
        )

        // Mensajes finales.
        if (won || lost) {
            p.color = Color.argb(220, 8, 12, 25)
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

    private fun button(c: Canvas, r: RectF, label: String) {
        p.color = Color.argb(190, 22, 36, 55)
        c.drawRoundRect(r, 10f * scale, 10f * scale, p)

        p.color = Color.WHITE
        p.textSize = 11f * scale
        p.textAlign = Paint.Align.CENTER

        c.drawText(
            label,
            r.centerX(),
            r.centerY() + 4f * scale,
            p
        )

        p.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = scale
        val w = width.toFloat()
        val h = height.toFloat()

        val tx = e.x
        val ty = e.y

        if (won || lost) {
            if (
                e.action == MotionEvent.ACTION_DOWN ||
                e.action == MotionEvent.ACTION_UP
            ) {
                resetGame()
                invalidate()
            }
            return true
        }

        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> {
                left = tx in (15f*s)..(85f*s) &&
                    ty > h - 100f*s

                right = tx in (95f*s)..(165f*s) &&
                    ty > h - 100f*s

                if (e.actionMasked == MotionEvent.ACTION_DOWN) {
                    if (tx > w - 185f*s && ty < 95f*s) {
                        light = !light
                    } else if (
                        tx > w - 195f*s &&
                        tx < w - 110f*s &&
                        ty > h - 100f*s
                    ) {
                        if (grounded) {
                            velocity = -13f
                            grounded = false
                        }
                    } else if (
                        tx >= w - 110f*s &&
                        ty > h - 100f*s
                    ) {
                        attack = true
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_CANCEL -> {
                left = false
                right = false
            }
        }

        invalidate()
        return true
    }
}
