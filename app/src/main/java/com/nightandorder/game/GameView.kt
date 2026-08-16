package com.nightandorder.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

enum class Screen { TITLE, SELECT, PLAY, LEVELUP, PAUSE, END }

class GameView(
    context: Context,
    private val brightness: BrightnessMonitor,
    private val updates: UpdateClient,
) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val assets = Assets(context)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val spritePaint = Paint().apply { isFilterBitmap = true }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        color = Color.WHITE
    }
    private val tmp = RectF()

    @Volatile private var running = false
    @Volatile private var thread: Thread? = null
    @Volatile var screen = Screen.TITLE
    private var world: World? = null
    private var selected: CharacterDef = Catalog.characters[0]
    private var hover: CharacterDef? = Catalog.characters[0]

    private var stickId = -1
    private var stickCx = 0f
    private var stickCy = 0f
    private var stickX = 0f
    private var stickY = 0f
    private var keyL = false
    private var keyR = false
    private var keyU = false
    private var keyD = false

    private val pendingHits = ArrayList<Hit>(16)
    @Volatile private var liveHits: List<Hit> = emptyList()
    private var lastNs = 0L
    private var titlePulse = 0f
    private var lastBrightNs = 0L
    private var cachedBg: Bitmap? = null
    private var cachedBgVampire = false
    private var cachedBgW = 0
    private var cachedBgH = 0
    private val vampires = Catalog.characters.filter { it.faction == Faction.VAMPIRE }
    private val holies = Catalog.characters.filter { it.faction == Faction.HOLY }

    init {
        holder.addCallback(this)
        holder.setFormat(PixelFormat.OPAQUE)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    private fun resetPaint() {
        paint.shader = null
        paint.alpha = 255
        paint.style = Paint.Style.FILL
        paint.strokeWidth = 0f
        paint.color = Color.WHITE
    }

    fun onResumeGame() {
        if (holder.surface.isValid) startLoop()
    }

    fun onPauseGame() {
        if (screen == Screen.PLAY) screen = Screen.PAUSE
        stopLoop()
    }

    fun onBack(): Boolean {
        return when (screen) {
            Screen.PLAY -> {
                screen = Screen.PAUSE
                true
            }
            Screen.PAUSE, Screen.LEVELUP -> true
            Screen.SELECT, Screen.END -> {
                screen = Screen.TITLE
                true
            }
            Screen.TITLE -> false
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        startLoop()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        stopLoop()
    }

    private fun startLoop() {
        if (running) return
        running = true
        lastNs = SystemClock.elapsedRealtimeNanos()
        thread = Thread(this, "night-loop").also { it.start() }
    }

    private fun stopLoop() {
        running = false
        thread?.join(400)
        thread = null
    }

    override fun run() {
        while (running) {
            val frameStart = SystemClock.elapsedRealtimeNanos()
            var dt = (frameStart - lastNs) / 1_000_000_000f
            lastNs = frameStart
            if (dt > 0.05f) dt = 0.05f
            if (screen == Screen.PLAY && frameStart - lastBrightNs > 500_000_000L) {
                brightness.refresh()
                lastBrightNs = frameStart
            }
            tick(dt)
            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    drawAll(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
            val tookMs = (SystemClock.elapsedRealtimeNanos() - frameStart) / 1_000_000L
            val sleep = 16L - tookMs
            if (sleep > 1L) {
                try {
                    Thread.sleep(sleep)
                } catch (_: InterruptedException) {
                }
            }
        }
    }

    private fun tick(dt: Float) {
        titlePulse += dt
        val w = world
        if (screen == Screen.PLAY && w != null) {
            w.brightness = brightness.brightness
            var ix = stickX
            var iy = stickY
            if (keyL) ix -= 1f
            if (keyR) ix += 1f
            if (keyU) iy -= 1f
            if (keyD) iy += 1f
            w.inputX = ix
            w.inputY = iy
            w.tick(dt)
            when {
                w.pendingOffers != null -> screen = Screen.LEVELUP
                w.end != null -> screen = Screen.END
            }
        }
    }

    private fun startRun(def: CharacterDef) {
        world = World(def)
        world?.brightness = brightness.brightness
        stickId = -1
        stickX = 0f
        stickY = 0f
        screen = Screen.PLAY
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val w = width.toFloat()
        val h = height.toFloat()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val i = event.actionIndex
                val x = event.getX(i)
                val y = event.getY(i)
                val id = event.getPointerId(i)
                if (screen == Screen.PLAY && x < w * 0.62f && y > h * 0.45f) {
                    stickId = id
                    stickCx = x
                    stickCy = y
                    stickX = 0f
                    stickY = 0f
                } else {
                    onTap(x, y)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (stickId != -1) {
                    val idx = event.findPointerIndex(stickId)
                    if (idx >= 0) {
                        val dx = event.getX(idx) - stickCx
                        val dy = event.getY(idx) - stickCy
                        val m = hypot(dx, dy).coerceAtLeast(1f)
                        val maxR = 90f
                        val clamped = min(m, maxR)
                        stickX = dx / m * (clamped / maxR)
                        stickY = dy / m * (clamped / maxR)
                    }
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                val id = event.getPointerId(event.actionIndex)
                if (id == stickId) {
                    stickId = -1
                    stickX = 0f
                    stickY = 0f
                }
            }
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> keyL = true
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> keyR = true
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> keyU = true
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> keyD = true
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_SPACE -> {
                if (screen == Screen.TITLE) screen = Screen.SELECT
                else if (screen == Screen.SELECT) startRun(selected)
            }
            KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_ESCAPE -> onBack()
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> keyL = false
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> keyR = false
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> keyU = false
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> keyD = false
            else -> return super.onKeyUp(keyCode, event)
        }
        return true
    }

    private fun onTap(x: Float, y: Float) {
        val hits = liveHits
        for (i in hits.indices.reversed()) {
            val b = hits[i]
            if (b.contains(x, y)) {
                b.action()
                return
            }
        }
    }

    private fun drawAll(c: Canvas) {
        resetPaint()
        pendingHits.clear()
        when (screen) {
            Screen.TITLE -> drawTitle(c)
            Screen.SELECT -> drawSelect(c)
            Screen.PLAY -> drawPlay(c)
            Screen.LEVELUP -> {
                drawPlay(c)
                drawLevelUp(c)
            }
            Screen.PAUSE -> {
                drawPlay(c)
                drawPause(c)
            }
            Screen.END -> {
                drawPlay(c)
                drawEnd(c)
            }
        }
        liveHits = pendingHits.toList()
    }

    private fun drawBackdrop(c: Canvas, vampireTint: Boolean) {
        val w = c.width
        val h = c.height
        val cache = cachedBg
        if (cache == null || cachedBgW != w || cachedBgH != h || cachedBgVampire != vampireTint) {
            cache?.recycle()
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565)
            val cc = Canvas(bmp)
            val top = if (vampireTint) 0xFF140810.toInt() else 0xFF101018.toInt()
            val bot = if (vampireTint) 0xFF2A1018.toInt() else 0xFF1A1830.toInt()
            paint.shader = LinearGradient(0f, 0f, 0f, h.toFloat(), top, bot, Shader.TileMode.CLAMP)
            cc.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
            paint.shader = null
            val tile = assets.tile
            if (tile != null) {
                paint.alpha = 70
                var y = 0f
                while (y < h) {
                    var x = 0f
                    while (x < w) {
                        cc.drawBitmap(tile, x, y, paint)
                        x += tile.width
                    }
                    y += tile.height
                }
                paint.alpha = 255
            }
            cachedBg = bmp
            cachedBgW = w
            cachedBgH = h
            cachedBgVampire = vampireTint
            c.drawBitmap(bmp, 0f, 0f, null)
        } else {
            c.drawBitmap(cache, 0f, 0f, null)
        }
    }

    private fun drawTitle(c: Canvas) {
        drawBackdrop(c, true)
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        text.textAlign = Paint.Align.CENTER
        text.color = 0xFFE8C98A.toInt()
        text.textSize = w * 0.11f
        c.drawText("НОЧЬ", w / 2f, h * 0.28f, text)
        text.textSize = w * 0.055f
        text.color = 0xFF8B1E2D.toInt()
        c.drawText("И", w / 2f, h * 0.35f, text)
        text.textSize = w * 0.11f
        text.color = 0xFFE8C98A.toInt()
        c.drawText("ОРДЕН", w / 2f, h * 0.46f, text)
        text.textSize = w * 0.038f
        text.color = 0x88E8D5A3.toInt()
        c.drawText("две крови · одно поле", w / 2f, h * 0.52f, text)
        text.textSize = w * 0.028f
        text.color = 0x66E8D5A3.toInt()
        c.drawText(BuildConfig.VERSION_NAME, w / 2f, h * 0.57f, text)

        val showUpdate = updates.phase == UpdatePhase.AVAILABLE ||
            updates.phase == UpdatePhase.DOWNLOADING ||
            updates.phase == UpdatePhase.READY ||
            updates.phase == UpdatePhase.FAILED
        if (showUpdate) {
            drawUpdateCard(c, w, h)
            drawButton(c, w * 0.22f, h * 0.60f, w * 0.56f, h * 0.07f, "Войти", 0xFF3A2018.toInt()) {
                screen = Screen.SELECT
            }
        } else {
            val pulse = 0.55f + 0.45f * (0.5f + 0.5f * sin(titlePulse * 2.2f))
            text.alpha = (pulse * 255).toInt()
            text.textSize = w * 0.042f
            text.color = Color.WHITE
            c.drawText("коснись, чтобы войти", w / 2f, h * 0.78f, text)
            text.alpha = 255
            pendingHits += Hit(0f, 0f, w, h) { screen = Screen.SELECT }
        }
    }

    private fun drawUpdateCard(c: Canvas, w: Float, h: Float) {
        val top = h * 0.70f
        paint.color = 0xEE140810.toInt()
        tmp.set(w * 0.07f, top, w * 0.93f, h * 0.96f)
        c.drawRoundRect(tmp, 18f, 18f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        paint.color = 0xFFD4B06A.toInt()
        c.drawRoundRect(tmp, 18f, 18f, paint)
        paint.style = Paint.Style.FILL
        text.textAlign = Paint.Align.CENTER
        text.color = 0xFFE8C98A.toInt()
        text.textSize = w * 0.038f
        val rel = updates.remote
        when (updates.phase) {
            UpdatePhase.AVAILABLE -> {
                c.drawText("Новая кровь  ${rel?.versionName ?: ""}", w / 2f, top + h * 0.045f, text)
                text.textSize = w * 0.028f
                text.color = 0xCCE8D5A3.toInt()
                c.drawText("на устройстве  ${BuildConfig.VERSION_NAME}", w / 2f, top + h * 0.08f, text)
                drawButton(c, w * 0.12f, top + h * 0.11f, w * 0.44f, h * 0.055f, "Обновить", 0xFF6A1824.toInt()) {
                    updates.download()
                }
                drawButton(c, w * 0.58f, top + h * 0.11f, w * 0.30f, h * 0.055f, "Позже", 0xFF2A1014.toInt()) {
                    updates.dismiss()
                }
            }
            UpdatePhase.DOWNLOADING -> {
                c.drawText("Качаю ${rel?.versionName ?: ""}", w / 2f, top + h * 0.05f, text)
                paint.color = 0xFF2A1014.toInt()
                tmp.set(w * 0.14f, top + h * 0.09f, w * 0.86f, top + h * 0.115f)
                c.drawRoundRect(tmp, 8f, 8f, paint)
                paint.color = 0xFFD4B06A.toInt()
                tmp.right = tmp.left + (w * 0.72f) * updates.progress
                c.drawRoundRect(tmp, 8f, 8f, paint)
                text.textSize = w * 0.028f
                text.color = Color.WHITE
                c.drawText("${(updates.progress * 100).toInt()}%", w / 2f, top + h * 0.16f, text)
            }
            UpdatePhase.READY -> {
                c.drawText("Готово к установке", w / 2f, top + h * 0.05f, text)
                drawButton(c, w * 0.22f, top + h * 0.10f, w * 0.56f, h * 0.06f, "Поставить", 0xFF6A1824.toInt()) {
                    (context as? android.app.Activity)?.let { updates.installOrRequestPermission(it) }
                }
            }
            UpdatePhase.FAILED -> {
                c.drawText("Не вышло скачать", w / 2f, top + h * 0.045f, text)
                text.textSize = w * 0.026f
                text.color = 0xAAD4B06A.toInt()
                c.drawText(updates.error ?: "", w / 2f, top + h * 0.08f, text)
                drawButton(c, w * 0.18f, top + h * 0.11f, w * 0.36f, h * 0.055f, "Ещё раз", 0xFF6A1824.toInt()) {
                    updates.download()
                }
                drawButton(c, w * 0.56f, top + h * 0.11f, w * 0.28f, h * 0.055f, "Позже", 0xFF2A1014.toInt()) {
                    updates.dismiss()
                }
            }
            else -> Unit
        }
    }

    private fun drawSelect(c: Canvas) {
        drawBackdrop(c, selected.faction == Faction.VAMPIRE)
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        text.textAlign = Paint.Align.CENTER
        text.textSize = w * 0.046f
        text.color = 0xFFE8C98A.toInt()
        c.drawText("Выбери кровь", w / 2f, h * 0.055f, text)

        text.textSize = w * 0.026f
        text.color = 0xFFB05060.toInt()
        c.drawText("ВАМПИРЫ", w * 0.27f, h * 0.095f, text)
        text.color = 0xFFD4B06A.toInt()
        c.drawText("СВЯТОЙ ОРДЕН", w * 0.73f, h * 0.095f, text)

        val gridTop = h * 0.11f
        val gridBot = h * 0.62f
        val cardW = w * 0.42f
        val gapY = h * 0.012f
        val cardH = (gridBot - gridTop - gapY * 2f) / 3f
        val leftX = w * 0.055f
        val rightX = w * 0.525f
        vampires.forEachIndexed { i, def ->
            drawPortraitCard(c, def, leftX, gridTop + i * (cardH + gapY), cardW, cardH)
        }
        holies.forEachIndexed { i, def ->
            drawPortraitCard(c, def, rightX, gridTop + i * (cardH + gapY), cardW, cardH)
        }

        val loreY = h * 0.64f
        paint.color = 0xCC120814.toInt()
        tmp.set(w * 0.055f, loreY, w * 0.945f, h * 0.81f)
        c.drawRoundRect(tmp, 18f, 18f, paint)
        text.color = 0xFFE8D5A3.toInt()
        text.textSize = w * 0.042f
        c.drawText(selected.name, w / 2f, loreY + h * 0.04f, text)
        text.textSize = w * 0.028f
        text.color = 0x88E8D5A3.toInt()
        c.drawText(selected.title, w / 2f, loreY + h * 0.07f, text)
        text.textSize = w * 0.032f
        text.color = Color.WHITE
        drawFittedText(c, selected.lore, w / 2f, loreY + h * 0.12f, w * 0.84f, w * 0.032f)

        drawButton(c, w * 0.18f, h * 0.84f, w * 0.64f, h * 0.075f, "В ПОЛЕ", 0xFF6A1824.toInt()) {
            startRun(selected)
        }
    }

    private fun drawPortraitCard(c: Canvas, def: CharacterDef, x: Float, y: Float, bw: Float, bh: Float) {
        val chosen = def.id == selected.id
        paint.color = if (chosen) {
            if (def.faction == Faction.VAMPIRE) 0xAA5A1020.toInt() else 0xAA3A3420.toInt()
        } else {
            0x66100814.toInt()
        }
        tmp.set(x, y, x + bw, y + bh)
        c.drawRoundRect(tmp, 16f, 16f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = if (chosen) 4f else 1.5f
        paint.color = if (def.faction == Faction.VAMPIRE) 0xFF8B1E2D.toInt() else 0xFFD4B06A.toInt()
        c.drawRoundRect(tmp, 16f, 16f, paint)
        paint.style = Paint.Style.FILL

        val nameSize = (bh * 0.13f).coerceAtMost(bw * 0.13f)
        val titleSize = nameSize * 0.72f
        val textBlock = nameSize + titleSize + bh * 0.08f
        val spriteRoom = (bh - textBlock - 10f).coerceAtLeast(bh * 0.45f)
        val sprite = spriteRoom.coerceAtMost(bw * 0.72f)
        val clip = assets.characterWalk[def.id]
        val bmp = clip?.at(titlePulse * 0.7f) ?: assets.characters[def.id]
        if (bmp != null) {
            val bob = Motion.walkBob(titlePulse + def.id.ordinal, false)
            val dx = x + (bw - sprite) / 2f
            val dy = y + 6f + bob
            tmp.set(dx, dy, dx + sprite, dy + sprite)
            c.drawBitmap(bmp, null, tmp, spritePaint)
        }
        val nameY = y + 6f + sprite + nameSize
        text.textAlign = Paint.Align.CENTER
        text.textSize = nameSize
        text.color = Color.WHITE
        drawFittedText(c, def.name, x + bw / 2f, nameY, bw * 0.9f, nameSize)
        text.textSize = titleSize
        text.color = 0x88E8D5A3.toInt()
        drawFittedText(c, def.title, x + bw / 2f, nameY + titleSize + 2f, bw * 0.92f, titleSize)
        pendingHits += Hit(x, y, x + bw, y + bh) { selected = def }
    }

    private fun drawFittedText(c: Canvas, value: String, cx: Float, y: Float, maxW: Float, want: Float) {
        var size = want
        text.textSize = size
        while (size > 10f && text.measureText(value) > maxW) {
            size -= 1f
            text.textSize = size
        }
        c.drawText(value, cx, y, text)
    }

    private fun drawPlay(c: Canvas) {
        val wld = world ?: return
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        val camX = wld.player.x
        val camY = wld.player.y
        val scale = w / 420f

        fun sx(x: Float) = w / 2f + (x - camX) * scale
        fun sy(y: Float) = h / 2f + (y - camY) * scale

        c.drawColor(0xFF141018.toInt())
        resetPaint()
        val ground = assets.ground
        if (ground != null) {
            val tw = ground.width.toFloat()
            val ox = posMod(camX * scale, tw)
            val oy = posMod(camY * scale, tw)
            var y = -oy
            while (y < h) {
                var x = -ox
                while (x < w) {
                    c.drawBitmap(ground, x, y, spritePaint)
                    x += tw
                }
                y += tw
            }
        }

        val margin = 80f
        for (g in wld.pickups) {
            val gx = sx(g.x)
            val gy = sy(g.y)
            if (gx < -margin || gy < -margin || gx > w + margin || gy > h + margin) continue
            val pulse = Motion.gemPulse(wld.time * 2f + g.x * 0.01f)
            paint.color = 0xFF7EC8FF.toInt()
            c.drawCircle(gx, gy, 5f * scale * pulse, paint)
            paint.color = 0xAAE8F8FF.toInt()
            c.drawCircle(gx, gy, 2.2f * scale * pulse, paint)
        }
        for (e in wld.enemies) {
            val ex = sx(e.x)
            val ey = sy(e.y)
            if (ex < -margin || ey < -margin || ex > w + margin || ey > h + margin) continue
            val clip = assets.enemyWalk[e.kind]
            val bmp = clip?.at(wld.time) ?: assets.enemies[e.kind]
            val bob = Motion.walkBob(wld.time + e.x * 0.05f, true)
            val squash = Motion.walkSquash(wld.time + e.x * 0.05f, true)
            val size = e.radius * 2.4f * scale
            if (bmp != null) {
                c.save()
                if (e.facing < 0f) c.scale(-1f, 1f, ex, ey)
                val hw = size / 2f
                val hh = size / 2f * squash
                tmp.set(ex - hw, ey - hh + bob, ex + hw, ey + hh + bob)
                c.drawBitmap(bmp, null, tmp, spritePaint)
                c.restore()
            } else {
                paint.color = 0xFF6A5040.toInt()
                c.drawCircle(ex, ey + bob, e.radius * scale, paint)
            }
            if (e.hp < e.maxHp) {
                val bw = e.radius * 2.2f * scale
                paint.color = 0xFF000000.toInt()
                c.drawRect(ex - bw / 2f, ey - e.radius * scale - 8f, ex + bw / 2f, ey - e.radius * scale - 4f, paint)
                paint.color = 0xFFCC3344.toInt()
                c.drawRect(ex - bw / 2f, ey - e.radius * scale - 8f, ex - bw / 2f + bw * (e.hp / e.maxHp), ey - e.radius * scale - 4f, paint)
            }
        }
        for (pr in wld.projectiles) {
            val px = sx(pr.x)
            val py = sy(pr.y)
            if (px < -margin || py < -margin || px > w + margin || py > h + margin) continue
            paint.color = when {
                pr.holy && pr.explode -> 0xFFFFF0B0.toInt()
                pr.holy -> 0xFFE8D48A.toInt()
                pr.seek -> 0xFFC04088.toInt()
                else -> 0xFFB02030.toInt()
            }
            c.save()
            c.rotate(Math.toDegrees(Motion.spin(wld.time + pr.x * 0.02f, 14f).toDouble()).toFloat(), px, py)
            val rr = pr.radius * scale
            if (pr.orbit || pr.seek) {
                c.drawCircle(px, py, rr, paint)
                paint.color = 0x66FFFFFF
                c.drawCircle(px - rr * 0.3f, py - rr * 0.3f, rr * 0.35f, paint)
            } else {
                tmp.set(px - rr, py - rr * 0.55f, px + rr, py + rr * 0.55f)
                c.drawRoundRect(tmp, rr * 0.4f, rr * 0.4f, paint)
            }
            c.restore()
        }
        for (p in wld.particles) {
            val px = sx(p.x)
            val py = sy(p.y)
            if (px < -margin || py < -margin || px > w + margin || py > h + margin) continue
            val a = (255 * (p.life / p.maxLife)).toInt().coerceIn(0, 255)
            paint.color = (a shl 24) or (p.color and 0x00FFFFFF)
            c.drawCircle(px, py, p.size * scale, paint)
        }
        resetPaint()

        val p = wld.player
        val flash = p.invuln > 0f && ((p.invuln * 20).toInt() % 2 == 0)
        if (!flash) {
            val moving = hypot(p.vx, p.vy) > 12f
            val clip = assets.characterWalk[p.def.id]
            val animT = if (moving) wld.time else wld.time * 0.4f
            val bmp = clip?.at(animT) ?: assets.characters[p.def.id]
            val size = p.def.radius * 3.1f * scale
            if (bmp != null) {
                val px = sx(p.x)
                val py = sy(p.y)
                val bob = Motion.walkBob(wld.time, moving)
                val squash = Motion.walkSquash(wld.time, moving)
                c.save()
                if (p.facing < 0f) {
                    c.scale(-1f, 1f, px, py)
                }
                val hw = size / 2f
                val hh = size / 2f * squash
                tmp.set(px - hw, py - hh + bob, px + hw, py + hh + bob)
                c.drawBitmap(bmp, null, tmp, spritePaint)
                c.restore()
            }
        }

        drawHud(c, wld)
        if (stickId != -1) {
            paint.color = 0x55FFFFFF.toInt()
            c.drawCircle(stickCx, stickCy, 90f, paint)
            paint.color = 0x99FFFFFF.toInt()
            c.drawCircle(stickCx + stickX * 90f, stickCy + stickY * 90f, 34f, paint)
            resetPaint()
        }
    }

    private fun posMod(value: Float, mod: Float): Float {
        if (mod <= 0f) return 0f
        val r = value % mod
        return if (r < 0f) r + mod else r
    }

    private fun drawHud(c: Canvas, wld: World) {
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        paint.color = 0x88000000.toInt()
        c.drawRect(0f, 0f, w, h * 0.105f, paint)

        val barRight = w * 0.62f
        val hp = (wld.player.hp / wld.player.maxHp).coerceIn(0f, 1f)
        paint.color = 0xFF2A1014.toInt()
        tmp.set(w * 0.03f, h * 0.016f, barRight, h * 0.040f)
        c.drawRoundRect(tmp, 8f, 8f, paint)
        paint.color = 0xFFA02030.toInt()
        tmp.right = tmp.left + (barRight - tmp.left) * hp
        c.drawRoundRect(tmp, 8f, 8f, paint)

        val xp = (wld.xp.toFloat() / wld.xpToNext).coerceIn(0f, 1f)
        paint.color = 0xFF102028.toInt()
        tmp.set(w * 0.03f, h * 0.048f, barRight, h * 0.068f)
        c.drawRoundRect(tmp, 8f, 8f, paint)
        paint.color = 0xFF6EC0E8.toInt()
        tmp.right = tmp.left + (barRight - tmp.left) * xp
        c.drawRoundRect(tmp, 8f, 8f, paint)

        val rite = wld.power.rite
        val cx = w * 0.68f
        val cy = h * 0.042f
        paint.color = 0x33000000.toInt()
        c.drawCircle(cx, cy, 14f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        paint.color = if (wld.character.faction == Faction.VAMPIRE) 0xFF8B1E2D.toInt() else 0xFFD4B06A.toInt()
        c.drawCircle(cx, cy, 14f, paint)
        paint.style = Paint.Style.FILL
        paint.alpha = (40 + rite * 200).toInt()
        c.drawCircle(cx, cy, 8f * (0.35f + rite * 0.65f), paint)
        paint.alpha = 255

        text.textAlign = Paint.Align.CENTER
        text.textSize = w * 0.040f
        text.color = 0xFFE8D5A3.toInt()
        val sec = wld.time.toInt()
        c.drawText("%d:%02d".format(sec / 60, sec % 60), w * 0.82f, h * 0.038f, text)
        text.textSize = w * 0.024f
        text.color = 0x88FFFFFF.toInt()
        c.drawText("ур.${wld.level}  ${wld.kills}", w * 0.82f, h * 0.068f, text)

        drawButton(c, w * 0.90f, h * 0.018f, w * 0.08f, h * 0.062f, "II", 0x66201018.toInt()) {
            if (screen == Screen.PLAY) screen = Screen.PAUSE
        }
    }

    private fun drawLevelUp(c: Canvas) {
        val wld = world ?: return
        val offers = wld.pendingOffers ?: return
        dim(c)
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        text.textAlign = Paint.Align.CENTER
        text.color = 0xFFE8C98A.toInt()
        text.textSize = w * 0.055f
        c.drawText("Кровь густеет", w / 2f, h * 0.16f, text)
        offers.forEachIndexed { i, offer ->
            val y = h * 0.24f + i * h * 0.18f
            paint.color = 0xEE1A1016.toInt()
            tmp.set(w * 0.1f, y, w * 0.9f, y + h * 0.15f)
            c.drawRoundRect(tmp, 18f, 18f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = 0xFFD4B06A.toInt()
            paint.strokeWidth = 2f
            c.drawRoundRect(tmp, 18f, 18f, paint)
            paint.style = Paint.Style.FILL
            text.color = Color.WHITE
            text.textSize = w * 0.045f
            drawFittedText(c, offer.title, w / 2f, y + h * 0.06f, w * 0.72f, w * 0.045f)
            text.color = 0xCCE8D5A3.toInt()
            drawFittedText(c, offer.body, w / 2f, y + h * 0.105f, w * 0.74f, w * 0.032f)
            pendingHits += Hit(tmp.left, tmp.top, tmp.right, tmp.bottom) {
                wld.pick(offer)
                screen = Screen.PLAY
            }
        }
    }

    private fun drawPause(c: Canvas) {
        dim(c)
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        text.textAlign = Paint.Align.CENTER
        text.color = 0xFFE8C98A.toInt()
        text.textSize = w * 0.07f
        c.drawText("Пауза", w / 2f, h * 0.32f, text)
        drawButton(c, w * 0.2f, h * 0.42f, w * 0.6f, h * 0.08f, "Дальше", 0xFF3A2018.toInt()) {
            screen = Screen.PLAY
        }
        drawButton(c, w * 0.2f, h * 0.54f, w * 0.6f, h * 0.08f, "Оставить поле", 0xFF2A1014.toInt()) {
            world = null
            screen = Screen.TITLE
        }
    }

    private fun drawEnd(c: Canvas) {
        val wld = world ?: return
        dim(c)
        val w = c.width.toFloat()
        val h = c.height.toFloat()
        val win = wld.end == RunEnd.DAWN
        text.textAlign = Paint.Align.CENTER
        text.textSize = w * 0.06f
        text.color = if (win) 0xFFE8C98A.toInt() else 0xFFC05060.toInt()
        drawFittedText(
            c,
            if (win) "Рассвет застал вас" else "Поле взяло своё",
            w / 2f,
            h * 0.28f,
            w * 0.86f,
            w * 0.06f,
        )
        text.textSize = w * 0.038f
        text.color = Color.WHITE
        val sec = wld.time.toInt()
        c.drawText("${wld.character.name}  ·  ${"%d:%02d".format(sec / 60, sec % 60)}", w / 2f, h * 0.38f, text)
        c.drawText("убито  ${wld.kills}     круг  ${wld.level}", w / 2f, h * 0.45f, text)
        text.textSize = w * 0.032f
        text.color = 0x88E8D5A3.toInt()
        val line = if (wld.character.faction == Faction.VAMPIRE) {
            if (wld.power.rite > 0.45f) "Ночь запомнила этот час." else "Рассвет был слишком близко."
        } else {
            if (wld.power.rite > 0.55f) "Свет видел каждый ваш удар." else "Сияние сегодня было тусклым."
        }
        c.drawText(line, w / 2f, h * 0.54f, text)
        drawButton(c, w * 0.18f, h * 0.66f, w * 0.64f, h * 0.08f, "Ещё раз", 0xFF6A1824.toInt()) {
            startRun(wld.character)
        }
        drawButton(c, w * 0.18f, h * 0.78f, w * 0.64f, h * 0.08f, "К выбору", 0xFF2A1018.toInt()) {
            world = null
            screen = Screen.SELECT
        }
    }

    private fun dim(c: Canvas) {
        paint.color = 0xAA07050A.toInt()
        c.drawRect(0f, 0f, c.width.toFloat(), c.height.toFloat(), paint)
    }

    private fun drawButton(
        c: Canvas,
        x: Float,
        y: Float,
        bw: Float,
        bh: Float,
        label: String,
        color: Int,
        action: () -> Unit,
    ) {
        paint.color = color
        tmp.set(x, y, x + bw, y + bh)
        c.drawRoundRect(tmp, 16f, 16f, paint)
        paint.style = Paint.Style.STROKE
        paint.color = 0x88E8C98A.toInt()
        paint.strokeWidth = 2f
        c.drawRoundRect(tmp, 16f, 16f, paint)
        paint.style = Paint.Style.FILL
        text.textAlign = Paint.Align.CENTER
        text.color = Color.WHITE
        text.textSize = bh * 0.42f
        c.drawText(label, x + bw / 2f, y + bh * 0.66f, text)
        pendingHits += Hit(x, y, x + bw, y + bh, action)
    }

    private class Hit(
        val l: Float,
        val t: Float,
        val r: Float,
        val b: Float,
        val action: () -> Unit,
    ) {
        fun contains(x: Float, y: Float) = x in l..r && y in t..b
    }
}
