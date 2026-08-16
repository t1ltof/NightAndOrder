package com.nightandorder.game

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint

class Assets(context: Context) {
    val characters = HashMap<CharacterId, Bitmap>()
    val enemies = HashMap<EnemyKind, Bitmap>()
    var tile: Bitmap? = null
        private set

    init {
        val am = context.assets
        fun load(name: String, size: Int): Bitmap? {
            return runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                am.open(name).use { BitmapFactory.decodeStream(it, null, bounds) }
                var sample = 1
                val srcW = bounds.outWidth.coerceAtLeast(1)
                while (srcW / sample > size * 2) sample *= 2
                val opts = BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                    inScaled = false
                }
                val decoded = am.open(name).use { BitmapFactory.decodeStream(it, null, opts) }
                    ?: return@runCatching null
                if (decoded.width == size && decoded.height == size) {
                    decoded
                } else {
                    Bitmap.createScaledBitmap(decoded, size, size, true).also {
                        if (it != decoded) decoded.recycle()
                    }
                }
            }.getOrNull()
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

        tile = load("tile_ground.png", 96)
    }

    private fun fallback(color: Int, size: Int): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = color
        c.drawCircle(size / 2f, size / 2f, size * 0.42f, p)
        return bmp
    }
}
