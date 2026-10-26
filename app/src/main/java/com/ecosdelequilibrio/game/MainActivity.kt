
package com.ecosdelequilibrio.game

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.MotionEvent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.RectF
import android.content.Context
import kotlin.math.max
import kotlin.math.min

class MainActivity : Activity() {
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

    private val scale get() = width / 960f
    private val ground get() = height * 0.78f

    override fun onDraw(c: Canvas) {
        super.onDraw(c)
        updateGame()
        drawGame(c)
        postInvalidateDelayed(16)
    }

    private fun updateGame() {
        if (won || width == 0) return

        if (left) {
            x -= 4f
            facing = -1f
        }
        if (right) {
            x += 4f
            facing = 1f
        }

        x = max(25f, min(1800f, x))

        velocity += 0.65f
        jumpY += velocity

        if (jumpY >= 0f) {
            jumpY = 0f
            velocity = 0f
            grounded = true
        }

        world = (x - 300f).coerceIn(0f, 900f)

        if (attack) {
            attack = false
            if (kotlin.math.abs(x - bossX) < 125f) {
                bossHealth--
                if (bossHealth <= 0) won = true
            }
        }

        if (kotlin.math.abs(x - bossX) < 65f &&
            jumpY == 0f && bossHealth > 0) {
            // El jefe amenaza al jugador al acercarse.
            // El ataque del jugador se controla desde el botón.
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

        // Fondo y montañas
        p.color = Color.rgb(42, 51, 83)
        for (i in 0..7) {
            val bx = i * 170f * s - (world * s * 0.2f % (170f * s))
            val path = android.graphics.Path()
            path.moveTo(bx, ground - 20f * s)
            path.lineTo(bx + 85f * s, h * 0.25f)
            path.lineTo(bx + 170f * s, ground - 20f * s)
            path.close()
            c.drawPath(path, p)
        }

        // Suelo
        p.color = Color.rgb(43, 61, 67)
        c.drawRect(0f, ground, w, h, p)
        p.color = Color.rgb(93, 200, 165)
        c.drawRect(0f, ground, w, ground + 6f * s, p)

        // Plataformas
        p.color = Color.rgb(80, 105, 117)
        for (i in 0..7) {
            val px = (220f + i * 210f - world) * s
            if (px > -130f * s && px < w) {
                c.drawRect(
                    px, ground - 100f * s,
                    px + 100f * s, ground - 85f * s, p
                )
            }
        }

        // Cristales
        p.color = if (light) Color.CYAN else Color.MAGENTA
        for (i in 0..9) {
            val cx = (100f + i * 190f - world) * s
            if (cx in 0f..w) {
                c.drawCircle(cx, ground - 35f * s, 5f * s, p)
            }
        }

        // Personaje
        val px = (x - world) * s
        val py = ground + jumpY * s

        p.color = if (light) Color.rgb(91, 211, 255)
                  else Color.rgb(212, 111, 255)
        c.drawRoundRect(
            px - 15f * s, py - 48f * s,
            px + 15f * s, py, 7f * s, 7f * s, p
        )

        p.color = Color.WHITE
        c.drawCircle(
            px + facing * 6f * s,
            py - 36f * s, 3f * s, p
        )

        // Guardián
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

            p.color = Color.DKGRAY
            c.drawRect(w - 190f * s, 20f * s, w - 20f * s, 33f * s, p)
            p.color = Color.RED
            c.drawRect(
                w - 190f * s, 20f * s,
                w - (20f + 170f * (10 - bossHealth) / 10f) * s,
                33f * s, p
            )
        }

        // Interfaz
        p.color = Color.WHITE
        p.textSize = 16f * s
        c.drawText("ECOS DEL EQUILIBRIO", 16f * s, 25f * s, p)
        c.drawText("VIDA: $health", 16f * s, 48f * s, p)
        c.drawText(
            if (light) "PODER: LUZ" else "PODER: OSCURIDAD",
            16f * s, 70f * s, p
        )

        button(c, RectF(15f*s, h-85f*s, 85f*s, h-15f*s), "IZQ")
        button(c, RectF(95f*s, h-85f*s, 165f*s, h-15f*s), "DER")
        button(c, RectF(w-190f*s, h-85f*s, w-115f*s, h-15f*s), "SALTO")
        button(c, RectF(w-105f*s, h-85f*s, w-15f*s, h-15f*s), "ATACA")
        button(c, RectF(w-175f*s, 45f*s, w-15f*s, 85f*s), "LUZ / OSC.")

        if (won) {
            p.color = Color.WHITE
            p.textSize = 30f * s
            c.drawText("¡GUARDIÁN DERROTADO!", w * 0.25f, h * 0.45f, p)
        }
    }

    private fun button(c: Canvas, r: RectF, label: String) {
        p.color = Color.argb(190, 22, 36, 55)
        c.drawRoundRect(r, 10f * scale, 10f * scale, p)
        p.color = Color.WHITE
        p.textSize = 11f * scale
        p.textAlign = Paint.Align.CENTER
        c.drawText(label, r.centerX(), r.centerY() + 4f * scale, p)
        p.textAlign = Paint.Align.LEFT
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        val s = scale
        val w = width.toFloat()
        val h = height.toFloat()
        val tx = e.x
        val ty = e.y

        if (e.action == MotionEvent.ACTION_DOWN ||
            e.action == MotionEvent.ACTION_MOVE) {
            left = tx < 90f * s && ty > h - 100f * s
            right = tx in (90f*s)..(180f*s) && ty > h - 100f*s

            if (e.action == MotionEvent.ACTION_DOWN) {
                if (tx > w - 185f*s && ty < 95f*s) {
                    light = !light
                } else if (tx > w - 195f*s &&
                    tx < w - 110f*s && ty > h - 100f*s) {
                    if (grounded) {
                        velocity = -13f
                        grounded = false
                    }
                } else if (tx >= w - 110f*s && ty > h - 100f*s) {
                    attack = true
                }
            }
        }

        if (e.action == MotionEvent.ACTION_UP ||
            e.action == MotionEvent.ACTION_CANCEL) {
            left = false
            right = false
        }

        return true
    }
}
