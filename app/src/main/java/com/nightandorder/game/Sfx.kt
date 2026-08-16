package com.nightandorder.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.random.Random

class Sfx(private val context: Context, private val prefs: Prefs) {
    private var pool: SoundPool? = null
    private var tones: ToneGenerator? = null
    private val samples = HashMap<String, Int>()
    private var hitFlip = false
    private val rng = Random(7)
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
        pool?.release()
        samples.clear()
        val vol = (prefs.volume * 100).toInt().coerceIn(0, 100)
        tones = if (vol <= 0) null else runCatching {
            ToneGenerator(android.media.AudioManager.STREAM_MUSIC, vol)
        }.getOrNull()
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(10).setAudioAttributes(attrs).build()
        pool = sp
        fun load(name: String) {
            runCatching {
                context.assets.openFd(name).use { fd ->
                    samples[name] = sp.load(fd, 1)
                }
            }
        }
        load("sfx_hit_a.wav")
        load("sfx_hit_b.wav")
        load("sfx_hurt.wav")
        load("sfx_kill.wav")
    }

    fun play(event: Cue) {
        when (event) {
            Cue.HIT -> bang(if (hitFlip) "sfx_hit_b.wav" else "sfx_hit_a.wav", 0.72f).also { hitFlip = !hitFlip }
            Cue.KILL -> bang("sfx_kill.wav", 0.88f)
            Cue.HURT -> bang("sfx_hurt.wav", 1f)
            else -> {
                val tg = tones ?: return
                runCatching { playTone(tg, event) }
            }
        }
    }

    private fun bang(name: String, gain: Float) {
        val sp = pool ?: return
        val id = samples[name] ?: return
        if (prefs.volume <= 0.01f) return
        val v = (prefs.volume * gain).coerceIn(0f, 1f)
        val rate = 0.92f + rng.nextFloat() * 0.16f
        runCatching { sp.play(id, v, v, 1, 0, rate) }
    }

    private fun playTone(tg: ToneGenerator, event: Cue) {
        when (event) {
            Cue.GEM -> tg.startTone(ToneGenerator.TONE_PROP_ACK, 35)
            Cue.GEM_RARE -> tg.startTone(ToneGenerator.TONE_PROP_NACK, 70)
            Cue.HEAL -> tg.startTone(ToneGenerator.TONE_CDMA_PIP, 80)
            Cue.LEVEL -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 120)
            Cue.CHEST -> tg.startTone(ToneGenerator.TONE_SUP_CONFIRM, 140)
            Cue.BOSS -> tg.startTone(ToneGenerator.TONE_CDMA_EMERGENCY_RINGBACK, 200)
            Cue.DAWN -> tg.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 220)
            Cue.HUM_DARK -> tg.startTone(ToneGenerator.TONE_CDMA_MED_L, 60)
            Cue.HUM_LIGHT -> tg.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 50)
            else -> Unit
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
        pool?.release()
        pool = null
        samples.clear()
    }
}

enum class Cue {
    HIT, KILL, HURT, GEM, GEM_RARE, HEAL, LEVEL, CHEST, BOSS, DAWN, HUM_DARK, HUM_LIGHT,
}
