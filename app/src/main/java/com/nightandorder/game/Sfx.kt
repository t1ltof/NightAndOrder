package com.nightandorder.game

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class Sfx(context: Context, private val prefs: Prefs) {
    private var tones: ToneGenerator? = null
    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= 31) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    init {
        rebuild()
    }

    fun rebuild() {
        tones?.release()
        val vol = (prefs.volume * 100).toInt().coerceIn(0, 100)
        tones = if (vol <= 0) null else runCatching {
            ToneGenerator(AudioManager.STREAM_MUSIC, vol)
        }.getOrNull()
    }

    fun play(event: Cue) {
        val tg = tones ?: return
        runCatching { playTone(tg, event) }
    }

    private fun playTone(tg: ToneGenerator, event: Cue) {
        when (event) {
            Cue.HIT -> tg.startTone(ToneGenerator.TONE_PROP_BEEP, 28)
            Cue.HURT -> tg.startTone(ToneGenerator.TONE_CDMA_SOFT_ERROR_LITE, 90)
            Cue.GEM -> tg.startTone(ToneGenerator.TONE_PROP_ACK, 35)
            Cue.GEM_RARE -> tg.startTone(ToneGenerator.TONE_PROP_NACK, 70)
            Cue.HEAL -> tg.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
            Cue.LEVEL -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 120)
            Cue.CHEST -> tg.startTone(ToneGenerator.TONE_SUP_CONFIRM, 140)
            Cue.BOSS -> tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200)
            Cue.DAWN -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 220)
            Cue.HUM_DARK -> tg.startTone(ToneGenerator.TONE_CDMA_MED_L, 60)
            Cue.HUM_LIGHT -> tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 50)
        }
    }

    fun vibe(ms: Long, amp: Int = 120) {
        if (!prefs.vibrate) return
        val v = vibrator ?: return
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, amp.coerceIn(1, 255)))
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(ms)
        }
    }

    fun release() {
        tones?.release()
        tones = null
    }
}

enum class Cue {
    HIT, HURT, GEM, GEM_RARE, HEAL, LEVEL, CHEST, BOSS, DAWN, HUM_DARK, HUM_LIGHT,
}
