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

    fun charMarks(id: CharacterId): Int = p.getInt("m_c_${id.name}", 0)

    fun factionMarks(faction: Faction): Int = p.getInt("m_f_${faction.name}", 0)

    fun addCharMarks(id: CharacterId, n: Int) {
        if (n == 0) return
        p.edit().putInt("m_c_${id.name}", (charMarks(id) + n).coerceAtLeast(0)).apply()
    }

    fun addFactionMarks(faction: Faction, n: Int) {
        if (n == 0) return
        p.edit().putInt("m_f_${faction.name}", (factionMarks(faction) + n).coerceAtLeast(0)).apply()
    }

    fun heroRank(id: CharacterId, perk: HeroPerk): Int =
        p.getInt("h_${id.name}_${perk.name}", 0).coerceIn(0, Meta.MAX_RANK)

    fun factionRank(faction: Faction, perk: FactionPerk): Int =
        p.getInt("f_${faction.name}_${perk.name}", 0).coerceIn(0, Meta.MAX_RANK)

    fun tryBuyHero(id: CharacterId, perk: HeroPerk): Boolean {
        val rank = heroRank(id, perk)
        if (rank >= Meta.MAX_RANK) return false
        val price = Meta.cost(rank)
        val have = charMarks(id)
        if (have < price) return false
        p.edit()
            .putInt("m_c_${id.name}", have - price)
            .putInt("h_${id.name}_${perk.name}", rank + 1)
            .apply()
        return true
    }

    fun tryBuyFaction(faction: Faction, perk: FactionPerk): Boolean {
        val rank = factionRank(faction, perk)
        if (rank >= Meta.MAX_RANK) return false
        val price = Meta.cost(rank)
        val have = factionMarks(faction)
        if (have < price) return false
        p.edit()
            .putInt("m_f_${faction.name}", have - price)
            .putInt("f_${faction.name}_${perk.name}", rank + 1)
            .apply()
        return true
    }
}
