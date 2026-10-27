
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

    private val worldWidth = 3200f

    private var playerX = 120f
    private var jumpY = 0f
    private var velocityY = 0f
    private var cameraX = 0f
    private var facing = 1f

    private var health = 5
    private var bossHealth = 12
    private val bossX = 2850f

    private var crystals = 0
    private val collected = mutableSetOf<Int>()
private val mapPrefs =
    context.getSharedPreferences("ecos_mapas", Context.MODE_PRIVATE)

private val mapsFound = mutableSetOf<Int>().apply {
    for (i in 0..5) {
        if (mapPrefs.getBoolean("map_$i", false)) add(i)
    }
}

private var showMap = false

private val mapPositions = floatArrayOf(
    350f, 850f, 1350f, 1850f, 2350f, 2750f
)

private val regionNames = arrayOf(
    "Ruinas Antiguas",
    "Caverna Susurrante",
    "Bosque Celestial",
    "Montaña Helada",
    "Profundidades Sombrías",
    "Templo del Equilibrio"
)
    private var lightMode = true
    private var grounded = true
    private var crouching = false
    private var jumpCount = 0
    private var supportPlatform = -1

    private var won = false
    private var lost = false

    private var invulnerability = 0
    private var bossAttackTimer = 0
    private var attackFlash = 0
    private var shieldTimer = 0
    private var projectileFlash = 0

    private var joystickPointerId = -1
    private var joystickDX = 0f
    private var joystickDY = 0f

    private var jumpPointerId = -1
    private var attackPointerId = -1
    private var formPointerId = -1
    private var skillPointerId = -1

    private val scale: Float
        get() = width / 960f

    private val ground: Float
        get() = height * 0.78f

    // Plataformas: posición horizontal y altura sobre el suelo.
    private val platforms = arrayOf(
        floatArrayOf(260f, 70f),
        floatArrayOf(430f, 110f),
        floatArrayOf(620f, 65f),
        floatArrayOf(820f, 130f),
        floatArrayOf(1050f, 90f),
        floatArrayOf(1270f, 65f),
        floatArrayOf(1450f, 115f),
        floatArrayOf(1660f, 80f),
        floatArrayOf(1870f, 135f),
        floatArrayOf(2100f, 95f),
        floatArrayOf(2310f, 65f),
        floatArrayOf(2500f, 110f)
    )

    // Alturas expresadas en unidades del mundo.
    private fun platformHeight(index: Int): Float {
        return platforms[index][1]
    }

    private fun onPlatform(index: Int): Boolean {
        val start = platforms[index][0]

        return playerX + 15f > start &&
            playerX - 15f < start + 105f
    }

    private fun currentPlatformStillUnderPlayer(): Boolean {
        if (supportPlatform < 0) return true

        val height = platformHeight(supportPlatform)
        val onTop = abs(jumpY + height) < 3f

        return onTop && onPlatform(supportPlatform)
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
        cameraX = 0f
        facing = 1f

        health = 5
        bossHealth = 12
        crystals = 0
        collected.clear()

        lightMode = true
        grounded = true
        crouching = false
        jumpCount = 0
        supportPlatform = -1

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

    // Salto mejorado y doble salto.
    private fun jump() {
        if (won || lost) return

        if (jumpCount < 2) {
            velocityY = if (jumpCount == 0) -09f else -07f

            grounded = false
            jumpCount++
            supportPlatform = -1
            crouching = false
        }
    }

    private fun attack() {
        if (won || lost) return

        attackFlash = 9

        val distance = bossX - playerX

        if (bossHealth > 0 &&
            abs(distance) < 125f &&
            distance * facing > 0f
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
            projectileFlash = 12

            val distance = bossX - playerX

            if (bossHealth > 0 &&
                abs(distance) < 420f &&
                distance * facing > 0f
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

        // Movimiento horizontal.
        val speed = if (crouching) 1.8f else 4f

        playerX += joystickDX * speed
        playerX = playerX.coerceIn(20f, worldWidth - 20f)

        if (joystickDX < -0.15f) facing = -1f
        if (joystickDX > 0.15f) facing = 1f

        crouching = grounded && joystickDY > 0.65f

        // Caer si abandonas una plataforma.
        if (grounded && supportPlatform >= 0 &&
            !currentPlatformStillUnderPlayer()
        ) {
            grounded = false
            supportPlatform = -1
            jumpCount = 1
        }

        val previousY = jumpY

        if (!grounded) {
            // Gravedad más suave.
            velocityY += 0.38f
            jumpY += velocityY

            // Aterrizaje en plataformas al descender.
            if (velocityY > 0f) {
                for (i in platforms.indices) {
                    val targetY = -platformHeight(i)

                    if (previousY <= targetY &&
                        jumpY >= targetY &&
                        onPlatform(i)
                    ) {
                        jumpY = targetY
                        velocityY = 0f
                        grounded = true
                        jumpCount = 0
                        supportPlatform = i
                        break
                    }
                }
            }

            // Aterrizaje en el suelo.
            if (jumpY >= 0f) {
                jumpY = 0f
                velocityY = 0f
                grounded = true
                jumpCount = 0
                supportPlatform = -1
            }
        }

        // Cámara lateral que sigue al jugador.
        val visibleWorldWidth = width / scale

        cameraX = (playerX - visibleWorldWidth * 0.38f)
            .coerceIn(0f, worldWidth - visibleWorldWidth)

        // Descubrir mapas regionales.
        for (i in mapPositions.indices) {
            if (
                i !in mapsFound &&
                kotlin.math.abs(playerX - mapPositions[i]) < 35f &&
                kotlin.math.abs(jumpY) < 45f
            ) {
                mapsFound.add(i)

                mapPrefs.edit()
                    .putBoolean("map_$i", true)
                    .apply()
            }
        }
        
        // Recoger cristales cerca del suelo.
        for (i in 0..15) {
            val crystalX = 100f + i * 185f

            if (i !in collected &&
                abs(playerX - crystalX) < 28f &&
                abs(jumpY) < 35f
            ) {
                collected.add(i)
                crystals++
            }
        }

        // Ataques del Guardián.
        if (bossHealth > 0 && abs(playerX - bossX) < 100f) {
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

    private fun drawGame(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = scale

        canvas.drawColor(
            if (lightMode) Color.rgb(17, 27, 53)
            else Color.rgb(43, 17, 53)
        )

        // Estrellas con desplazamiento parallax.
        paint.color = Color.rgb(190, 210, 255)

        for (i in 0..45) {
            val raw = (i * 137f - cameraX * s * 0.15f) %
                (w + 30f)

            val sx = if (raw < 0f) raw + w + 30f else raw
            val sy = i * 43f % (h * 0.55f)

            canvas.drawCircle(sx, sy, 1.5f * s, paint)
        }

        // El escenario se mueve con la cámara.
        canvas.save()
        canvas.translate(-cameraX * s, 0f)

        // Montañas del fondo.
        paint.color = Color.rgb(42, 51, 83)

        for (i in 0..22) {
            val bx = i * 170f * s

            val path = Path()
            path.moveTo(bx, ground - 20f * s)
            path.lineTo(bx + 85f * s, h * 0.25f)
            path.lineTo(bx + 170f * s, ground - 20f * s)
            path.close()

            canvas.drawPath(path, paint)
        }

        // Suelo.
        paint.color = Color.rgb(43, 61, 67)
        canvas.drawRect(
            0f, ground, worldWidth * s, h, paint
        )

        paint.color = Color.rgb(93, 200, 165)
        canvas.drawRect(
            0f, ground, worldWidth * s, ground + 6f * s, paint
        )

        // Plataformas.
        for (platform in platforms) {
            val px = platform[0] * s
            val py = ground - platform[1] * s

            paint.color = Color.rgb(80, 105, 117)
            canvas.drawRoundRect(
                px, py, px + 105f * s, py + 15f * s,
                4f * s, 4f * s, paint
            )

            paint.color = Color.rgb(93, 200, 165)
            canvas.drawRect(
                px, py, px + 105f * s, py + 3f * s, paint
            )
        }

        // Pergaminos de mapas regionales.
        for (i in mapPositions.indices) {
            if (i in mapsFound) continue

            val mx = mapPositions[i] - cameraX
            val my = height - 65f * scale

            paint.color = Color.rgb(235, 207, 133)
            canvas.drawRoundRect(
                mx - 10f * scale,
                my - 13f * scale,
                mx + 10f * scale,
                my + 13f * scale,
                4f * scale,
                4f * scale,
                paint
            )

            paint.color = Color.rgb(92, 54, 30)
            paint.textSize = 12f * scale
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("M", mx, my + 4f * scale, paint)
            paint.textAlign = Paint.Align.LEFT
        }
        
        // Cristales coleccionables.
        for (i in 0..15) {
            if (i in collected) continue

            val cx = (100f + i * 185f) * s
            val cy = ground - 35f * s

            paint.color = if (lightMode) Color.CYAN else Color.MAGENTA

            val crystal = Path()
            crystal.moveTo(cx, cy - 9f * s)
            crystal.lineTo(cx + 6f * s, cy)
            crystal.lineTo(cx, cy + 9f * s)
            crystal.lineTo(cx - 6f * s, cy)
            crystal.close()

            canvas.drawPath(crystal, paint)
        }

        // Entrada visual a una zona secreta.
        val secretX = 1600f * s

        paint.color = Color.rgb(45, 32, 80)
        canvas.drawRoundRect(
            secretX, ground - 65f * s,
            secretX + 65f * s, ground,
            15f * s, 15f * s, paint
        )

        paint.color = Color.rgb(180, 100, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f * s

        canvas.drawRoundRect(
            secretX + 8f * s, ground - 55f * s,
            secretX + 57f * s, ground,
            12f * s, 12f * s, paint
        )

        paint.style = Paint.Style.FILL

        // Personaje.
        val px = playerX * s
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

            // Proyectil de luz.
            if (projectileFlash > 0 && lightMode) {
                paint.color = Color.CYAN

                canvas.drawCircle(
                    px + facing *
                        (45f + (12 - projectileFlash) * 12f) * s,
                    py - 30f * s,
                    8f * s, paint
                )
            }

            // Escudo de oscuridad.
            if (shieldTimer > 0 && !lightMode) {
                paint.color = Color.argb(130, 190, 70, 255)
                canvas.drawCircle(
                    px, py - 25f * s, 38f * s, paint
                )

                paint.color = Color.WHITE
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 2f * s
                canvas.drawCircle(
                    px, py - 25f * s, 40f * s, paint
                )
                paint.style = Paint.Style.FILL
            }
        }

        // Guardián al final del mapa.
        if (bossHealth > 0) {
            val bx = bossX * s

            paint.color = Color.rgb(111, 55, 132)
            canvas.drawRoundRect(
                bx - 32f * s, ground - 90f * s,
                bx + 32f * s, ground,
                14f * s, 14f * s, paint
            )

            paint.color = Color.rgb(255, 74, 130)
            canvas.drawCircle(
                bx - 10f * s, ground - 65f * s,
                4f * s, paint
            )
            canvas.drawCircle(
                bx + 10f * s, ground - 65f * s,
                4f * s, paint
            )
        }

        canvas.restore()

        // Interfaz fija.
        paint.color = Color.WHITE
        paint.textSize = 16f * s

        canvas.drawText(
            "ECOS DEL EQUILIBRIO", 16f * s, 25f * s, paint
        )
        canvas.drawText(
            "VIDA: $health / 5", 16f * s, 48f * s, paint
        )
        canvas.drawText(
            "CRISTALES: $crystals", 16f * s, 70f * s, paint
        )

        canvas.drawText(
            if (lightMode) "FORMA: LUZ" else "FORMA: OSCURIDAD",
            16f * s, 92f * s, paint
        )

        // Botón de mapa: solo aparece tras encontrar un pergamino.
        if (mapsFound.isNotEmpty()) {
            val mapButton = RectF(
                w / 2f - 55f * s,
                8f * s,
                w / 2f + 55f * s,
                40f * s
            )

            drawButton(
                canvas,
                mapButton,
                "MAPA",
                Color.rgb(60, 160, 230)
            )
        }
        
        // Barra de vida del jefe.
        if (bossHealth > 0) {
            val bx = w - 190f * s
            val by = 20f * s
            val barWidth = 170f * s

            paint.color = Color.DKGRAY
            canvas.drawRoundRect(
                bx, by, bx + barWidth, by + 13f * s,
                3f * s, 3f * s, paint
            )

            paint.color = Color.RED
            canvas.drawRoundRect(
                bx, by,
                bx + barWidth * bossHealth / 12f,
                by + 13f * s,
                3f * s, 3f * s, paint
            )
        }

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

        // Botones.
        val top = h - 82f * s
        val bottom = h - 14f * s
        val bw = 72f * s
        val gap = 8f * s
        val edge = w - 12f * s

        val skill = RectF(edge - bw, top, edge, bottom)
        val form = RectF(
            skill.left - gap - bw, top,
            skill.left - gap, bottom
        )
        val attack = RectF(
            form.left - gap - bw, top,
            form.left - gap, bottom
        )
        val jump = RectF(
            attack.left - gap - bw, top,
            attack.left - gap, bottom
        )

        drawButton(canvas, jump, "SALTO", Color.rgb(60, 180, 230))
        drawButton(canvas, attack, "ATAQUE", Color.rgb(230, 75, 120))
        drawButton(
            canvas, form,
            if (lightMode) "OSCUR." else "LUZ",
            Color.rgb(160, 90, 220)
        )
        drawButton(
            canvas, skill,
            if (lightMode) "PROY." else "ESCUDO",
            Color.rgb(70, 160, 140)
        )

        // Pantalla final.
        if (won || lost) {
            paint.color = Color.argb(225, 8, 12, 25)
            canvas.drawRect(0f, 0f, w, h, paint)

            paint.color = if (won) {
                Color.rgb(100, 255, 190)
            } else {
                Color.rgb(255, 100, 120)
            }

            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 29f * s

            canvas.drawText(
                if (won) "¡GUARDIÁN DERROTADO!" else "HAS CAÍDO",
                w / 2f, h * 0.43f, paint
            )

            paint.color = Color.WHITE
            paint.textSize = 17f * s

            canvas.drawText(
                "TOCA PARA VOLVER A JUGAR",
                w / 2f, h * 0.53f, paint
            )

            paint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawButton(
        canvas: Canvas,
        rect: RectF,
        label: String,
        color: Int
    ) {
        paint.color = Color.argb(210, 22, 36, 55)
        canvas.drawRoundRect(
            rect, 12f * scale, 12f * scale, paint
        )

        paint.color = color
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale

        canvas.drawRoundRect(
            rect, 12f * scale, 12f * scale, paint
        )

        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        paint.textSize = 10f * scale
        paint.textAlign = Paint.Align.CENTER

        canvas.drawText(
            label,
            rect.centerX(),
            rect.centerY() + 4f * scale,
            paint
        )

        paint.textAlign = Paint.Align.LEFT
    }

    private fun updateJoystick(tx: Float, ty: Float) {
        val radius = 48f * scale
        val centerX = 90f * scale
        val centerY = height - 60f * scale

        var dx = tx - centerX
        var dy = ty - centerY

        val distance = sqrt(dx * dx + dy * dy)

        if (distance > radius && distance > 0f) {
            dx = dx / distance * radius
            dy = dy / distance * radius
        }

        joystickDX = (dx / radius).coerceIn(-1f, 1f)
        joystickDY = (dy / radius).coerceIn(-1f, 1f)
    }

private fun drawWorldMap(canvas: Canvas) {
    val w = width.toFloat()
    val h = height.toFloat()
    val s = scale

    paint.color = Color.argb(245, 5, 10, 28)
    canvas.drawRect(0f, 0f, w, h, paint)

    paint.textAlign = Paint.Align.CENTER
    paint.color = Color.CYAN
    paint.textSize = 26f * s
    canvas.drawText(
        "MAPA DEL EQUILIBRIO",
        w / 2f,
        45f * s,
        paint
    )

    val startX = w * 0.12f
    val stepX = w * 0.152f
    val centerY = h * 0.45f

    for (i in 0 until 5) {
        if (i in mapsFound && i + 1 in mapsFound) {
            paint.color = Color.CYAN
            paint.strokeWidth = 4f * s
            canvas.drawLine(
                startX + i * stepX,
                centerY,
                startX + (i + 1) * stepX,
                centerY,
                paint
            )
        }
    }

    for (i in regionNames.indices) {
        val x = startX + i * stepX

        val box = RectF(
            x - 42f * s,
            centerY - 32f * s,
            x + 42f * s,
            centerY + 32f * s
        )

        paint.color = if (i in mapsFound) {
            Color.rgb(30, 105, 125)
        } else {
            Color.rgb(45, 45, 60)
        }

        canvas.drawRoundRect(
            box, 8f * s, 8f * s, paint
        )

        paint.color = if (i in mapsFound) {
            Color.CYAN
        } else {
            Color.LTGRAY
        }

        paint.textSize = 22f * s
        canvas.drawText(
            if (i in mapsFound) "✓" else "?",
            x,
            centerY + 8f * s,
            paint
        )

        paint.textSize = 10f * s
        val label = if (i in mapsFound) {
            regionNames[i]
        } else {
            "Desconocida"
        }

        canvas.drawText(
            label,
            x,
            centerY + 48f * s,
            paint
        )
    }

    paint.color = Color.WHITE
    paint.textSize = 16f * s
    canvas.drawText(
        "Mapas encontrados: ${mapsFound.size}/6",
        w / 2f,
        h * 0.72f,
        paint
    )

    paint.textSize = 14f * s
    canvas.drawText(
        "Toca la pantalla para volver",
        w / 2f,
        h * 0.85f,
        paint
    )

    paint.textAlign = Paint.Align.LEFT
if (showMap) {
    drawWorldMap(canvas)
}}

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val s = scale
        val w = width.toFloat()
        val h = height.toFloat()

        val top = h - 82f * s
        val bottom = h - 14f * s
        val bw = 72f * s
        val gap = 8f * s
        val edge = w - 12f * s

        val skill = RectF(edge - bw, top, edge, bottom)
        val form = RectF(
            skill.left - gap - bw, top,
            skill.left - gap, bottom
        )
        val attackButton = RectF(
            form.left - gap - bw, top,
            form.left - gap, bottom
        )
        val jumpButton = RectF(
            attackButton.left - gap - bw, top,
            attackButton.left - gap, bottom
        )

        fun handleDown(index: Int) {
            val tx = event.getX(index)
            val ty = event.getY(index)
            val id = event.getPointerId(index)

            val screenW = width.toFloat()
            val uiScale = scale

            if (showMap) {
                showMap = false
                invalidate()
                return
            }

            if (
    mapsFound.isNotEmpty() &&
    ty < 60f * uiScale &&
    tx > screenW / 2f - 90f * uiScale &&
    tx < screenW / 2f + 90f * uiScale
) {
    showMap = true
    invalidate()
    return
}
            
            if (won || lost) {
                resetGame()
                invalidate()
                return
            }

            if (joystickPointerId == id ||
                (joystickPointerId == -1 && tx < w * 0.38f)
            ) {
                joystickPointerId = id
                updateJoystick(tx, ty)
                return
            }

            when {
                jumpButton.contains(tx, ty) -> {
                    jumpPointerId = id
                    jump()
                }

                attackButton.contains(tx, ty) -> {
                    attackPointerId = id
                    attack()
                }

                form.contains(tx, ty) -> {
                    formPointerId = id
                    lightMode = !lightMode
                }

                skill.contains(tx, ty) -> {
                    skillPointerId = id
                    useSkill()
                }
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_POINTER_DOWN -> {
                handleDown(event.actionIndex)
            }

            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)

                    if (id == joystickPointerId) {
                        updateJoystick(
                            event.getX(i),
                            event.getY(i)
                        )
                    }
                }
            }

            MotionEvent.ACTION_UP,
            MotionEvent.ACTION_POINTER_UP -> {
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
