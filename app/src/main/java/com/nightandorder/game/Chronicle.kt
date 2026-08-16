package com.nightandorder.game

data class ChronicleNews(
    val heroes: List<CharacterId> = emptyList(),
    val relics: List<Relic> = emptyList(),
)

object Chronicle {
    fun isOpen(prefs: Prefs, id: CharacterId): Boolean {
        if (id == CharacterId.MORVAN || id == CharacterId.LUCIA) return true
        if (prefs.heroUnlocked(id)) return true
        if (prefs.charMarks(id) > 0) {
            prefs.unlockHero(id)
            return true
        }
        return false
    }

    fun hint(id: CharacterId): String = when (id) {
        CharacterId.MORVAN, CharacterId.LUCIA -> "В хронике с первой ночи."
        CharacterId.LILITH -> "Три минуты за вампира — или восемьдесят убийств за один забег."
        CharacterId.NIX -> "Дожить до зари за ночь. Или открыть Лилит и накопить 180 убийств ночи."
        CharacterId.HALE -> "Три минуты за орден — или восемьдесят убийств за один забег."
        CharacterId.SERA -> "Дожить до зари за орден. Или открыть Хейла и накопить 180 убийств ордена."
    }

    fun record(prefs: Prefs, world: World): ChronicleNews {
        val id = world.character.id
        val faction = world.character.faction
        prefs.addRun(id, faction, world.time, world.kills, world.level, world.end == RunEnd.DAWN)
        if (world.daily) {
            prefs.recordDaily(world.character.name, world.time, world.kills, world.end == RunEnd.DAWN)
        }
        val heroes = ArrayList<CharacterId>()
        fun tryUnlock(who: CharacterId) {
            if (!isOpen(prefs, who) && qualifies(prefs, world, who)) {
                prefs.unlockHero(who)
                heroes += who
            }
        }
        tryUnlock(CharacterId.LILITH)
        tryUnlock(CharacterId.NIX)
        tryUnlock(CharacterId.HALE)
        tryUnlock(CharacterId.SERA)
        val relics = ArrayList<Relic>()
        fun tryRelic(relic: Relic, ok: Boolean) {
            if (ok && !prefs.hasRelic(relic)) {
                prefs.grantRelic(relic)
                relics += relic
            }
        }
        tryRelic(Relic.FIRST_DAWN, world.end == RunEnd.DAWN)
        tryRelic(Relic.SABBATH, world.sabbath)
        tryRelic(Relic.PALE_DEATH, world.end == RunEnd.DEAD && world.moon >= 0.82f)
        tryRelic(Relic.CHAPEL, world.biome == Biome.CHAPEL && world.time >= 180f)
        tryRelic(Relic.FOG, world.sawFog)
        tryRelic(Relic.DAILY, world.daily && world.time >= 120f)
        return ChronicleNews(heroes, relics)
    }

    private fun qualifies(prefs: Prefs, world: World, who: CharacterId): Boolean {
        val vamp = world.character.faction == Faction.VAMPIRE
        val holy = !vamp
        val dawn = world.end == RunEnd.DAWN
        return when (who) {
            CharacterId.LILITH -> vamp && (world.time >= 180f || world.kills >= 80)
            CharacterId.NIX -> vamp && (dawn || (isOpen(prefs, CharacterId.LILITH) && prefs.factionKills(Faction.VAMPIRE) >= 180))
            CharacterId.HALE -> holy && (world.time >= 180f || world.kills >= 80)
            CharacterId.SERA -> holy && (dawn || (isOpen(prefs, CharacterId.HALE) && prefs.factionKills(Faction.HOLY) >= 180))
            else -> false
        }
    }
}
