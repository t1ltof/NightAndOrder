package com.nightandorder.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import kotlin.math.max

class Assets(context: Context) {
    val characters = HashMap<CharacterId, Bitmap>()
    val enemies = HashMap<EnemyKind, Bitmap>()
    var tile: Bitmap? = null
        private set

    init {
        val am = context.assets
        fun load(name: String, size: Int): Bitmap? {
            return runCatching {
                am.open(name).use { BitmapFactory.decodeStream(it) }
            }.getOrNull()?.let { src ->
                val keyed = keyBackground(src)
                Bitmap.createScaledBitmap(keyed, size, size, false).also {
                    if (it != keyed && keyed != src) keyed.recycle()
                    if (src != keyed) src.recycle()
                }
            }
        }

        characters[CharacterId.MORVAN] = load("char_morvan.png", 96) ?: fallback(0xFF5A1020.toInt(), 96)
        characters[CharacterId.LILITH] = load("char_lilith.png", 88) ?: fallback(0xFF3A0820.toInt(), 88)
        characters[CharacterId.LUCIA] = load("char_lucia.png", 92) ?: fallback(0xFFE8D5A3.toInt(), 92)
        characters[CharacterId.HALE] = load("char_hale.png", 96) ?: fallback(0xFFC9B070.toInt(), 96)
        characters[CharacterId.NIX] = load("char_nix.png", 96) ?: fallback(0xFF4A1038.toInt(), 96)
        characters[CharacterId.SERA] = load("char_sera.png", 96) ?: fallback(0xFFE8D8A8.toInt(), 96)

        enemies[EnemyKind.THRALL] = load("enemy_thrall.png", 72) ?: fallback(0xFF6A6A58.toInt(), 72)
        enemies[EnemyKind.BAT] = load("enemy_bat.png", 64) ?: fallback(0xFF4A2030.toInt(), 64)
        enemies[EnemyKind.FLAGELLANT] = load("enemy_flagellant.png", 80) ?: fallback(0xFF5A4030.toInt(), 80)
        enemies[EnemyKind.KNIGHT] = load("enemy_knight.png", 96) ?: fallback(0xFF2A2A32.toInt(), 96)
        enemies[EnemyKind.BOSS] = load("enemy_boss.png", 160) ?: fallback(0xFF7A2030.toInt(), 160)

        tile = load("tile_ground.png", 128)
    }

    private fun fallback(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        c.drawCircle(size / 2f, size / 2f, size * 0.42f, p)
        p.color = Color.WHITE
        p.alpha = 40
        c.drawCircle(size * 0.38f, size * 0.35f, size * 0.12f, p)
        return bmp
    }

    private fun keyBackground(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val out = src.copy(Bitmap.Config.ARGB_8888, true)
        val px = IntArray(w * h)
        out.getPixels(px, 0, w, 0, 0, w, h)
        val corners = intArrayOf(px[0], px[w - 1], px[(h - 1) * w], px[h * w - 1])
        var cr = 0
        var cg = 0
        var cb = 0
        for (c in corners) {
            cr += Color.red(c)
            cg += Color.green(c)
            cb += Color.blue(c)
        }
        cr /= 4
        cg /= 4
        cb /= 4
        val isMagenta = cr > 160 && cb > 160 && cg < 120
        val isGreen = cg > 160 && cr < 120 && cb < 120
        if (!isMagenta && !isGreen) return out
        for (i in px.indices) {
            val r = Color.red(px[i])
            val g = Color.green(px[i])
            val b = Color.blue(px[i])
            val dr = r - cr
            val dg = g - cg
            val db = b - cb
            val dist = dr * dr + dg * dg + db * db
            if (dist < 90 * 90) {
                val t = (dist / (90f * 90f)).coerceIn(0f, 1f)
                val a = if (t < 0.35f) 0 else ((t - 0.35f) / 0.65f * 255f).toInt()
                px[i] = (a shl 24) or (px[i] and 0x00FFFFFF)
            }
        }
        out.setPixels(px, 0, w, 0, 0, w, h)
        return cropAlpha(out)
    }

    private fun cropAlpha(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        val px = IntArray(w * h)
        src.getPixels(px, 0, w, 0, 0, w, h)
        var minX = w
        var minY = h
        var maxX = 0
        var maxY = 0
        for (y in 0 until h) {
            for (x in 0 until w) {
                if (Color.alpha(px[y * w + x]) > 16) {
                    if (x < minX) minX = x
                    if (y < minY) minY = y
                    if (x > maxX) maxX = x
                    if (y > maxY) maxY = y
                }
            }
        }
        if (maxX <= minX || maxY <= minY) return src
        val pad = 4
        minX = (minX - pad).coerceAtLeast(0)
        minY = (minY - pad).coerceAtLeast(0)
        maxX = (maxX + pad).coerceAtMost(w - 1)
        maxY = (maxY + pad).coerceAtMost(h - 1)
        val cw = maxX - minX + 1
        val ch = maxY - minY + 1
        val side = max(cw, ch)
        val dst = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888)
        val ox = (side - cw) / 2
        val oy = (side - ch) / 2
        val c = Canvas(dst)
        c.drawBitmap(src, android.graphics.Rect(minX, minY, maxX + 1, maxY + 1), android.graphics.Rect(ox, oy, ox + cw, oy + ch), null)
        return dst
    }
}
