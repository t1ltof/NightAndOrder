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

    fun heroUnlocked(id: CharacterId): Boolean = p.getBoolean("u_${id.name}", false)

    fun unlockHero(id: CharacterId) {
        p.edit().putBoolean("u_${id.name}", true).apply()
    }

    fun bestTime(id: CharacterId): Float = p.getFloat("r_t_${id.name}", 0f)
    fun bestKills(id: CharacterId): Int = p.getInt("r_k_${id.name}", 0)
    fun bestLevel(id: CharacterId): Int = p.getInt("r_l_${id.name}", 0)
    fun heroDawns(id: CharacterId): Int = p.getInt("r_d_${id.name}", 0)
    fun heroRuns(id: CharacterId): Int = p.getInt("r_n_${id.name}", 0)
    fun factionKills(faction: Faction): Int = p.getInt("fk_${faction.name}", 0)
    fun factionDawns(faction: Faction): Int = p.getInt("fd_${faction.name}", 0)
    fun factionRuns(faction: Faction): Int = p.getInt("fn_${faction.name}", 0)

    fun addRun(
        id: CharacterId,
        faction: Faction,
        time: Float,
        kills: Int,
        level: Int,
        dawn: Boolean,
    ) {
        val e = p.edit()
        if (time > bestTime(id)) e.putFloat("r_t_${id.name}", time)
        if (kills > bestKills(id)) e.putInt("r_k_${id.name}", kills)
        if (level > bestLevel(id)) e.putInt("r_l_${id.name}", level)
        e.putInt("r_n_${id.name}", heroRuns(id) + 1)
        e.putInt("fn_${faction.name}", factionRuns(faction) + 1)
        e.putInt("fk_${faction.name}", factionKills(faction) + kills)
        if (dawn) {
            e.putInt("r_d_${id.name}", heroDawns(id) + 1)
            e.putInt("fd_${faction.name}", factionDawns(faction) + 1)
        }
        e.apply()
    }

    fun hasRelic(relic: Relic): Boolean = p.getBoolean("rel_${relic.name}", false)

    fun grantRelic(relic: Relic) {
        p.edit().putBoolean("rel_${relic.name}", true).apply()
    }

    fun dailyStamp(): String = p.getString("daily_stamp", "") ?: ""
    fun dailyTime(): Float = p.getFloat("daily_time", 0f)
    fun dailyKills(): Int = p.getInt("daily_kills", 0)
    fun dailyHero(): String = p.getString("daily_hero", "") ?: ""
    fun dailyDawn(): Boolean = p.getBoolean("daily_dawn", false)

    fun recordDaily(hero: String, time: Float, kills: Int, dawn: Boolean) {
        val today = Night.today()
        val e = p.edit()
        if (dailyStamp() != today) {
            e.putString("daily_stamp", today)
            e.putFloat("daily_time", time)
            e.putInt("daily_kills", kills)
            e.putString("daily_hero", hero)
            e.putBoolean("daily_dawn", dawn)
        } else {
            val better = time > dailyTime() || (time == dailyTime() && kills > dailyKills())
            if (better) {
                e.putFloat("daily_time", time)
                e.putInt("daily_kills", kills)
                e.putString("daily_hero", hero)
                e.putBoolean("daily_dawn", dawn)
            } else if (dawn) {
                e.putBoolean("daily_dawn", true)
            }
        }
        e.apply()
    }
}
