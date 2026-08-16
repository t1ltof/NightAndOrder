package com.nightandorder.game

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.BatteryManager
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlin.math.cos

data class RiteSample(
    val hour: Float,
    val nightness: Float,
    val moon: Float,
    val battery: Float,
    val charging: Boolean,
    val silent: Boolean,
    val headphones: Boolean,
    val sunday: Boolean,
    val sabbath: Boolean,
)

data class RiteMods(
    val dmgMul: Float = 1f,
    val hpMul: Float = 1f,
    val spdMul: Float = 1f,
    val cdMul: Float = 1f,
    val areaMul: Float = 1f,
    val magnetAdd: Float = 0f,
    val armorAdd: Float = 0f,
    val seekMul: Float = 1f,
    val spawnMul: Float = 1f,
    val batBias: Float = 0f,
    val enemySpdMul: Float = 1f,
    val burnMul: Float = 1f,
)

object Rites {
    fun sample(context: Context): RiteSample {
        val now = ZonedDateTime.now()
        val hour = now.hour + now.minute / 60f
        val nightness = (0.5 + 0.5 * cos(hour * Math.PI / 12.0)).toFloat()
        val moon = moonPhase(LocalDate.now())
        val bat = battery(context)
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val silent = audio.ringerMode != AudioManager.RINGER_MODE_NORMAL
        val dow = now.dayOfWeek
        return RiteSample(
            hour = hour,
            nightness = nightness,
            moon = moon,
            battery = bat.first,
            charging = bat.second,
            silent = silent,
            headphones = wearingHeadphones(audio),
            sunday = dow == DayOfWeek.SUNDAY,
            sabbath = (dow == DayOfWeek.FRIDAY && hour >= 18f) ||
                (dow == DayOfWeek.SATURDAY && hour < 4f),
        )
    }

    fun mods(faction: Faction, hero: CharacterId, s: RiteSample, daily: Boolean = false): RiteMods {
        val src = if (daily) s.copy(battery = 0.55f, charging = false) else s
        return modsOf(faction, hero, src)
    }

    private fun modsOf(faction: Faction, hero: CharacterId, s: RiteSample): RiteMods {
        var dmg = 1f
        var hp = 1f
        var spd = 1f
        var cd = 1f
        var area = 1f
        var mag = 0f
        var armor = 0f
        var seek = 1f
        var spawn = 1f
        var bats = 0f
        var espd = 1f
        var burn = 1f
        val vampire = faction == Faction.VAMPIRE
        val mage = hero == CharacterId.NIX || hero == CharacterId.SERA

        if (vampire) {
            dmg *= 0.90f + 0.22f * s.nightness
            cd *= 1.06f - 0.10f * s.nightness
            if (s.nightness < 0.30f) hp *= 0.94f
            burn *= 0.85f + 0.30f * (1f - s.nightness)
        } else {
            val day = 1f - s.nightness
            dmg *= 0.92f + 0.18f * day
            area *= 0.96f + 0.10f * day
        }

        val full = ((s.moon - 0.55f) / 0.45f).coerceIn(0f, 1f)
        val darkMoon = ((0.45f - s.moon) / 0.45f).coerceIn(0f, 1f)
        bats += full * 0.22f - darkMoon * 0.10f
        spawn *= 1f + full * 0.10f - darkMoon * 0.08f
        espd *= 1f + full * 0.06f
        if (vampire) {
            dmg *= 1f + full * 0.12f
            mag += full * 16f - darkMoon * 10f
        } else {
            area *= 1f + darkMoon * 0.10f
            mag += darkMoon * 18f
            dmg *= 1f - full * 0.05f
        }

        val starve = (1f - s.battery).coerceIn(0f, 1f).let { it * it }
        if (vampire) {
            dmg *= 1f + starve * 0.20f
            hp *= 1f - starve * 0.16f
            spd *= 1f + starve * 0.06f
        } else {
            dmg *= 1f - starve * 0.14f
            armor -= starve * 0.04f
        }
        if (s.charging) {
            if (vampire) {
                spd *= 0.88f
                armor += 0.07f
            } else {
                armor += 0.06f
                area *= 1.08f
                mag += 22f
            }
        }

        if (s.silent) {
            if (vampire) {
                dmg *= 1.08f
                mag += 28f
            } else {
                dmg *= 0.92f
                cd *= 1.08f
                area *= 0.94f
            }
        } else if (!vampire) {
            cd *= 0.96f
            area *= 1.05f
        }

        if (s.headphones) {
            if (mage) {
                seek *= 1.28f
                area *= 1.10f
                cd *= 0.94f
            } else {
                mag += 8f
            }
        } else if (mage) {
            spawn *= 1.08f
            espd *= 1.05f
        }

        if (s.sunday) {
            if (vampire) dmg *= 0.94f
            else {
                dmg *= 1.10f
                hp *= 1.06f
                armor += 0.04f
            }
        }
        if (s.sabbath) {
            if (vampire) {
                dmg *= 1.12f
                mag += 14f
                bats += 0.12f
            } else {
                dmg *= 0.95f
            }
        }

        return RiteMods(
            dmgMul = dmg.coerceIn(0.78f, 1.55f),
            hpMul = hp.coerceIn(0.80f, 1.20f),
            spdMul = spd.coerceIn(0.82f, 1.18f),
            cdMul = cd.coerceIn(0.82f, 1.18f),
            areaMul = area.coerceIn(0.85f, 1.28f),
            magnetAdd = mag.coerceIn(-16f, 56f),
            armorAdd = armor.coerceIn(-0.06f, 0.16f),
            seekMul = seek.coerceIn(1f, 1.40f),
            spawnMul = spawn.coerceIn(0.85f, 1.22f),
            batBias = bats.coerceIn(-0.12f, 0.35f),
            enemySpdMul = espd.coerceIn(0.95f, 1.14f),
            burnMul = burn.coerceIn(0.80f, 1.35f),
        )
    }

    fun whisper(faction: Faction, hero: CharacterId, s: RiteSample): String {
        val vampire = faction == Faction.VAMPIRE
        val mage = hero == CharacterId.NIX || hero == CharacterId.SERA
        val lines = ArrayList<String>(6)
        if (s.sabbath && vampire) lines += "Пятница держит стаю."
        if (s.sunday && !vampire) lines += "Воскресный свет не гаснет."
        if (s.sunday && vampire) lines += "Сегодня земля святая. Тяжело."
        if (s.sabbath && !vampire) lines += "Шабаш шумит за полем."
        if (s.moon > 0.82f) {
            lines += if (vampire) "Луна полная. Кровь шумит." else "Полная луна беспокоит поле."
        } else if (s.moon < 0.14f) {
            lines += if (vampire) "Луны нет. Тишина." else "Небо пустое. Слышно дальше."
        }
        if (s.nightness > 0.78f) {
            lines += if (vampire) "Час глубокой ночи." else "Ночь давит на орден."
        } else if (s.nightness < 0.22f) {
            lines += if (vampire) "Дневной час. Без тьмы трудно." else "День держит шаг."
        }
        if (s.charging) {
            lines += if (vampire) "Серебряная жила держит ноги." else "Жила тёплая. Земля слушает."
        } else if (s.battery < 0.18f) {
            lines += if (vampire) "Голод подходит близко." else "Вера садится, как лампа."
        }
        if (s.silent) {
            lines += if (vampire) "Телефон молчит. Ночь ближе." else "Без голоса псалмы глуше."
        }
        if (s.headphones) {
            lines += if (mage) "В ухе кто-то шепчет имена." else "Исповедь только вам."
        }
        if (lines.isEmpty()) {
            return if (vampire) "Ночь ещё не сказала своего." else "Свет пока ровный."
        }
        val idx = ((s.hour * 3f).toInt() + hero.ordinal) % lines.size
        return lines[idx]
    }

    fun moonPhase(date: LocalDate): Float {
        val knownNew = LocalDate.of(2000, 1, 6).toEpochDay()
        val synodic = 29.530588
        var cycle = ((date.toEpochDay() - knownNew) % synodic)
        if (cycle < 0) cycle += synodic
        val t = cycle / synodic
        return (0.5 - 0.5 * cos(t * Math.PI * 2.0)).toFloat()
    }

    private fun battery(context: Context): Pair<Float, Boolean> {
        val sticky = runCatching {
            context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        if (sticky != null) {
            val level = sticky.getIntExtra(BatteryManager.EXTRA_LEVEL, 50)
            val scale = sticky.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = sticky.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            return (level.toFloat() / scale).coerceIn(0f, 1f) to charging
        }
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
        val pct = (bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 55) / 100f
        return pct.coerceIn(0f, 1f) to (bm?.isCharging == true)
    }

    private fun wearingHeadphones(audio: AudioManager): Boolean {
        val devices = audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any {
            when (it.type) {
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLE_HEADSET,
                -> true
                else -> false
            }
        }
    }
}
