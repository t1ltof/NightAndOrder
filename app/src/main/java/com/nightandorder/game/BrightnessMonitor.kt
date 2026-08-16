package com.nightandorder.game

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings

class BrightnessMonitor(private val context: Context) {
    @Volatile
    var brightness: Float = 0.55f
        private set

    private val resolver = context.contentResolver
    private val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    fun start() {
        resolver.registerContentObserver(
            Settings.System.getUriFor(Settings.System.SCREEN_BRIGHTNESS),
            false,
            observer,
        )
        refresh()
    }

    fun stop() {
        runCatching { resolver.unregisterContentObserver(observer) }
    }

    fun refresh() {
        brightness = read()
    }

    private fun read(): Float {
        val raw = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS, 140)
        val max = when {
            raw > 255 -> 2047f
            else -> 255f
        }
        return (raw / max).coerceIn(0f, 1f)
    }
}
