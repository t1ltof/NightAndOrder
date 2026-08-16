package com.nightandorder.game

import android.content.Context

class Prefs(context: Context) {
    private val p = context.getSharedPreferences("night_and_order", Context.MODE_PRIVATE)

    var volume: Float
        get() = p.getFloat("volume", 0.7f)
        set(v) { p.edit().putFloat("volume", v.coerceIn(0f, 1f)).apply() }

    var vibrate: Boolean
        get() = p.getBoolean("vibrate", true)
        set(v) { p.edit().putBoolean("vibrate", v).apply() }

    var hideNumbers: Boolean
        get() = p.getBoolean("hide_numbers", false)
        set(v) { p.edit().putBoolean("hide_numbers", v).apply() }
}
