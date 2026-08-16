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
    val characterWalk = HashMap<CharacterId, SpriteClip>()
    val enemyWalk = HashMap<EnemyKind, SpriteClip>()
    val props = HashMap<PropKind, Bitmap>()
    val bolts = HashMap<BoltArt, Bitmap>()
    var tile: Bitmap? = null
        private set
    var ground: Bitmap? = null
        private set
    var chapelGround: Bitmap? = null
        private set
    var menuBg: Bitmap? = null
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
        enemies[EnemyKind.ARCHER] = load("enemy_archer.png", 80) ?: fallback(0xFF4A3A48.toInt(), 80)
        enemies[EnemyKind.VESSEL] = load("enemy_vessel.png", 88) ?: fallback(0xFF8A4030.toInt(), 88)
        enemies[EnemyKind.WARDEN] = load("enemy_warden.png", 104) ?: fallback(0xFF3A3A42.toInt(), 104)
        enemies[EnemyKind.HERALD] = load("enemy_herald.png", 168) ?: fallback(0xFFE0C070.toInt(), 168)

        fun loadSheet(name: String, cell: Int): SpriteClip? {
            val src = runCatching {
                am.open(name).use { BitmapFactory.decodeStream(it) }
            }.getOrNull() ?: return null
            val h = src.height
            if (h <= 0) return null
            val count = (src.width / h).coerceAtLeast(1)
            val frames = Array(count) { i ->
                val raw = Bitmap.createBitmap(src, i * h, 0, h, h)
                if (raw.width == cell && raw.height == cell) raw
                else Bitmap.createScaledBitmap(raw, cell, cell, true).also {
                    if (it != raw) raw.recycle()
                }
            }
            return SpriteClip(frames, fps = 8f)
        }

        fun clipOrStill(sheet: String, still: Bitmap, cell: Int): SpriteClip {
            return loadSheet(sheet, cell) ?: SpriteClip(arrayOf(still), 1f)
        }

        characterWalk[CharacterId.MORVAN] = clipOrStill("walk_morvan.png", characters.getValue(CharacterId.MORVAN), 96)
        characterWalk[CharacterId.LILITH] = clipOrStill("walk_lilith.png", characters.getValue(CharacterId.LILITH), 88)
        characterWalk[CharacterId.NIX] = clipOrStill("walk_nix.png", characters.getValue(CharacterId.NIX), 96)
        characterWalk[CharacterId.LUCIA] = clipOrStill("walk_lucia.png", characters.getValue(CharacterId.LUCIA), 92)
        characterWalk[CharacterId.HALE] = clipOrStill("walk_hale.png", characters.getValue(CharacterId.HALE), 96)
        characterWalk[CharacterId.SERA] = clipOrStill("walk_sera.png", characters.getValue(CharacterId.SERA), 96)

        enemyWalk[EnemyKind.THRALL] = clipOrStill("walk_thrall.png", enemies.getValue(EnemyKind.THRALL), 72)
        enemyWalk[EnemyKind.BAT] = clipOrStill("walk_bat.png", enemies.getValue(EnemyKind.BAT), 64)
        enemyWalk[EnemyKind.FLAGELLANT] = clipOrStill("walk_flagellant.png", enemies.getValue(EnemyKind.FLAGELLANT), 80)
        enemyWalk[EnemyKind.KNIGHT] = clipOrStill("walk_knight.png", enemies.getValue(EnemyKind.KNIGHT), 96)
        enemyWalk[EnemyKind.BOSS] = clipOrStill("walk_boss.png", enemies.getValue(EnemyKind.BOSS), 160)
        enemyWalk[EnemyKind.ARCHER] = clipOrStill("walk_archer.png", enemies.getValue(EnemyKind.ARCHER), 80)
        enemyWalk[EnemyKind.VESSEL] = clipOrStill("walk_vessel.png", enemies.getValue(EnemyKind.VESSEL), 88)
        enemyWalk[EnemyKind.WARDEN] = clipOrStill("walk_warden.png", enemies.getValue(EnemyKind.WARDEN), 104)
        enemyWalk[EnemyKind.HERALD] = clipOrStill("walk_herald.png", enemies.getValue(EnemyKind.HERALD), 168)

        for (art in BoltArt.entries) {
            bolts[art] = load(art.file, 64) ?: fallback(art.fallback, 48)
        }

        props[PropKind.ROCK] = load("prop_rock.png", 96) ?: fallback(0xFF4A4A52.toInt(), 72)
        props[PropKind.STONE] = load("prop_stone.png", 72) ?: fallback(0xFF6A6A70.toInt(), 56)
        props[PropKind.TREE] = load("prop_tree.png", 128) ?: fallback(0xFF1A2018.toInt(), 96)
        props[PropKind.TREE_WIDE] = load("prop_tree2.png", 140) ?: fallback(0xFF142018.toInt(), 104)
        props[PropKind.PILLAR] = load("prop_pillar.png", 120) ?: fallback(0xFF6A6A68.toInt(), 88)
        props[PropKind.CROSS] = load("prop_cross.png", 100) ?: fallback(0xFF5A6058.toInt(), 72)
        props[PropKind.SLAB] = load("prop_slab.png", 96) ?: fallback(0xFF5A5A60.toInt(), 64)

        tile = load("tile_ground.png", 128)
        menuBg = runCatching {
            am.open("menu_bg.jpg").use { BitmapFactory.decodeStream(it) }
        }.getOrNull()
        fun sheetOf(src: Bitmap): Bitmap {
            val cell = src.width
            val sheet = Bitmap.createBitmap(cell * 4, cell * 4, Bitmap.Config.RGB_565)
            val cc = Canvas(sheet)
            val p = Paint().apply { isFilterBitmap = false }
            for (y in 0 until 4) {
                for (x in 0 until 4) {
                    cc.drawBitmap(src, (x * cell).toFloat(), (y * cell).toFloat(), p)
                }
            }
            return sheet
        }
        ground = tile?.let { sheetOf(it) }
        chapelGround = load("tile_chapel.png", 128)?.let { sheetOf(it) }
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
