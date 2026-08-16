package com.nightandorder.game

import kotlin.math.pow

enum class Faction { VAMPIRE, HOLY }

/**
 * Hidden rite. Never shown as a number in the HUD.
 *
 * Holy Order bonus grows almost linearly with screen brightness.
 * Vampire bonus is a steep night curve: almost nothing until the phone
 * is genuinely dark, then it explodes.
 *
 * Holy max bonus at full brightness = H.
 * Vampire max bonus at zero brightness = 10H.
 */
data class Power(
    val damage: Float,
    val ability: Float,
    val rite: Float,
)

object BrightnessPower {
    const val HOLY_MAX_BONUS = 0.40f
    const val VAMPIRE_MAX_BONUS = HOLY_MAX_BONUS * 10f

    fun of(faction: Faction, brightness: Float): Power {
        val b = brightness.coerceIn(0f, 1f)
        return when (faction) {
            Faction.VAMPIRE -> {
                val t = (1f - b).pow(4.5f)
                val bonus = VAMPIRE_MAX_BONUS * t
                Power(
                    damage = 0.70f + bonus,
                    ability = 0.75f + bonus * 0.55f,
                    rite = t,
                )
            }
            Faction.HOLY -> {
                val t = b.pow(1.15f)
                val bonus = HOLY_MAX_BONUS * t
                Power(
                    damage = 0.88f + bonus,
                    ability = 0.90f + bonus * 0.80f,
                    rite = t,
                )
            }
        }
    }
}
