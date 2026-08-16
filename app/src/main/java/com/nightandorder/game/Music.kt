package com.nightandorder.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.sin

class Music(private val prefs: Prefs) {
    enum class Bed { MENU, NIGHT, DAWN, END }

    @Volatile var bed: Bed = Bed.MENU
    @Volatile var vampire: Boolean = true

    private var track: AudioTrack? = null
    private var worker: Thread? = null
    @Volatile private var running = false
    @Volatile private var paused = false

    private var p1 = 0.0
    private var p2 = 0.0
    private var p3 = 0.0
    private var pn = 0.0
    private var clock = 0.0
    private var nextNote = 2.2
    private var noteHz = 0.0
    private var noteAmp = 0.0

    fun start() {
        if (running) return
        val min = AudioTrack.getMinBufferSize(SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (min <= 0) return
        val tr = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SR)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(min.coerceAtLeast(BUF * 4))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        applyGain(tr)
        track = tr
        running = true
        paused = false
        tr.play()
        worker = Thread({ loop() }, "night-music").also { it.isDaemon = true; it.start() }
    }

    fun pause() {
        paused = true
        runCatching { track?.pause() }
    }

    fun resume() {
        if (!running) {
            start()
            return
        }
        paused = false
        runCatching { track?.play() }
    }

    fun setVolume() {
        applyGain(track)
    }

    fun release() {
        running = false
        paused = false
        worker?.interrupt()
        worker = null
        runCatching { track?.pause() }
        runCatching { track?.release() }
        track = null
    }

    private fun applyGain(tr: AudioTrack?) {
        val g = (prefs.volume * 0.36f).coerceIn(0f, 1f)
        runCatching { tr?.setVolume(g) }
    }

    private fun loop() {
        val buf = ShortArray(BUF)
        while (running) {
            if (paused) {
                try {
                    Thread.sleep(40)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }
            fill(buf)
            val tr = track ?: break
            if (tr.write(buf, 0, buf.size) < 0) break
        }
    }

    private fun fill(buf: ShortArray) {
        val dt = 1.0 / SR
        val vamp = vampire
        val (aHz, bHz, cHz, drone) = when (bed) {
            Bed.MENU -> Quad(if (vamp) 36.7 else 49.0, if (vamp) 55.0 else 73.4, if (vamp) 73.4 else 98.0, 0.11)
            Bed.NIGHT -> Quad(if (vamp) 36.7 else 49.0, if (vamp) 55.0 else 73.4, if (vamp) 82.4 else 98.0, 0.16)
            Bed.DAWN -> Quad(if (vamp) 46.2 else 65.4, if (vamp) 69.3 else 98.0, if (vamp) 138.6 else 164.8, 0.20)
            Bed.END -> Quad(if (vamp) 32.7 else 43.7, if (vamp) 49.0 else 65.4, if (vamp) 65.4 else 87.3, 0.09)
        }
        val scale = if (vamp) VAMP_SCALE else HOLY_SCALE
        val twoPi = Math.PI * 2.0
        for (i in buf.indices) {
            clock += dt
            if (clock >= nextNote) {
                nextNote = clock + 3.4 + (clock * 0.17) % 2.1
                noteHz = scale[((clock * 13.0).toInt() + bed.ordinal * 3) % scale.size]
                noteAmp = if (bed == Bed.MENU) 0.045 else 0.075
            }
            noteAmp *= 0.9994
            p1 += twoPi * aHz * dt
            p2 += twoPi * bHz * dt
            p3 += twoPi * cHz * dt
            if (noteHz > 0.0) pn += twoPi * noteHz * dt
            if (p1 > twoPi) p1 -= twoPi
            if (p2 > twoPi) p2 -= twoPi
            if (p3 > twoPi) p3 -= twoPi
            if (pn > twoPi) pn -= twoPi
            val swell = 0.78 + 0.22 * sin(clock * 0.23)
            var s = sin(p1) * drone * 0.55 * swell
            s += sin(p2) * drone * 0.32
            s += sin(p3) * drone * 0.18
            if (noteAmp > 0.002) s += sin(pn) * noteAmp
            buf[i] = (s.coerceIn(-0.85, 0.85) * 32767.0).toInt().toShort()
        }
    }

    private data class Quad(val a: Double, val b: Double, val c: Double, val d: Double)

    companion object {
        private const val SR = 22050
        private const val BUF = 1024
        private val VAMP_SCALE = doubleArrayOf(146.8, 164.8, 174.6, 196.0, 220.0, 233.1, 174.6, 146.8)
        private val HOLY_SCALE = doubleArrayOf(196.0, 220.0, 233.1, 261.6, 293.7, 311.1, 261.6, 196.0)
    }
}
