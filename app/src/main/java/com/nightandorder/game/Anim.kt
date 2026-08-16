package com.nightandorder.game

import android.graphics.Bitmap
import kotlin.math.sin

class SpriteClip(val frames: Array<Bitmap>, val fps: Float = 8f) {
    fun at(time: Float): Bitmap {
        if (frames.isEmpty()) error("empty clip")
        if (frames.size == 1) return frames[0]
        val i = ((time * fps).toInt() % frames.size).let { if (it < 0) it + frames.size else it }
        return frames[i]
    }
}

object Motion {
    fun walkBob(time: Float, moving: Boolean): Float {
        val amp = if (moving) 3.2f else 1.4f
        val speed = if (moving) 11f else 5f
        return sin(time * speed) * amp
    }

    fun walkSquash(time: Float, moving: Boolean): Float {
        if (!moving) return 1f + sin(time * 5f) * 0.015f
        return 1f + sin(time * 11f) * 0.06f
    }

    fun gemPulse(time: Float): Float = 1f + sin(time * 7f) * 0.18f

    fun spin(time: Float, speed: Float = 8f): Float = time * speed
}
