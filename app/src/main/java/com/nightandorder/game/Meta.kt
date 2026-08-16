package com.nightandorder.game

enum class HeroPerk { VITALITY, MIGHT, SWIFT, CRAFT, GREED }

enum class FactionPerk { BLOODLINE, HUNT, RITE, HOLD, NIGHTLAW }

data class PerkInfo(
    val name: String,
    val blurb: String,
)

data class MetaMods(
    val hpMul: Float = 1f,
    val dmgMul: Float = 1f,
    val spdMul: Float = 1f,
    val cdMul: Float = 1f,
    val areaMul: Float = 1f,
    val magnetAdd: Float = 0f,
    val armorAdd: Float = 0f,
    val xpMul: Float = 1f,
    val startSigLevel: Int = 1,
    val brightShift: Float = 0f,
)

object Meta {
    const val MAX_RANK = 5

    fun cost(rank: Int): Int = 12 + rank * 12 + rank * rank * 4

    fun coin(faction: Faction): String = if (faction == Faction.VAMPIRE) "кровь" else "печати"

    fun factionTitle(faction: Faction): String = if (faction == Faction.VAMPIRE) "Ночь" else "Орден"

    fun heroPerk(id: CharacterId, perk: HeroPerk): PerkInfo {
        val stat = when (perk) {
            HeroPerk.VITALITY -> "Каждый ранг даёт больше здоровья."
            HeroPerk.MIGHT -> "Каждый ранг усиливает весь урон."
            HeroPerk.SWIFT -> "Каждый ранг делает шаг быстрее."
            HeroPerk.CRAFT -> "Оружие срабатывает чаще, вспышки шире. На 3-м и 5-м ранге стартовое оружие сильнее."
            HeroPerk.GREED -> "Сферы опыта тянутся дальше. За ранг чуть больше опыта."
        }
        val name = when (id) {
            CharacterId.MORVAN -> when (perk) {
                HeroPerk.VITALITY -> "Жила"
                HeroPerk.MIGHT -> "Тяжёлая кровь"
                HeroPerk.SWIFT -> "Шаг графа"
                HeroPerk.CRAFT -> "Наследие ночи"
                HeroPerk.GREED -> "Жадность склепа"
            }
            CharacterId.LILITH -> when (perk) {
                HeroPerk.VITALITY -> "Дикая живучесть"
                HeroPerk.MIGHT -> "Острые клыки"
                HeroPerk.SWIFT -> "Хищница"
                HeroPerk.CRAFT -> "Стайный инстинкт"
                HeroPerk.GREED -> "Жажда"
            }
            CharacterId.NIX -> when (perk) {
                HeroPerk.VITALITY -> "Костяной остов"
                HeroPerk.MIGHT -> "Чёрное пламя"
                HeroPerk.SWIFT -> "Далёкая тень"
                HeroPerk.CRAFT -> "Петля проклятия"
                HeroPerk.GREED -> "Сбор шёпота"
            }
            CharacterId.LUCIA -> when (perk) {
                HeroPerk.VITALITY -> "Обет"
                HeroPerk.MIGHT -> "Кара сестры"
                HeroPerk.SWIFT -> "Шаг процессии"
                HeroPerk.CRAFT -> "Литания"
                HeroPerk.GREED -> "Сбор света"
            }
            CharacterId.HALE -> when (perk) {
                HeroPerk.VITALITY -> "Железная вера"
                HeroPerk.MIGHT -> "Тяжёлый приговор"
                HeroPerk.SWIFT -> "Шаг инквизитора"
                HeroPerk.CRAFT -> "Допрос"
                HeroPerk.GREED -> "Конфискат"
            }
            CharacterId.SERA -> when (perk) {
                HeroPerk.VITALITY -> "Тонкая плоть"
                HeroPerk.MIGHT -> "Чистый свет"
                HeroPerk.SWIFT -> "Лёгкий шаг"
                HeroPerk.CRAFT -> "Псалом шире"
                HeroPerk.GREED -> "Дары алтаря"
            }
        }
        return PerkInfo(name, stat)
    }

    fun factionPerk(faction: Faction, perk: FactionPerk): PerkInfo {
        return when (faction) {
            Faction.VAMPIRE -> when (perk) {
                FactionPerk.BLOODLINE -> PerkInfo("Кровь рода", "Здоровье всем вампирам.")
                FactionPerk.HUNT -> PerkInfo("Охота стаи", "Урон всем вампирам.")
                FactionPerk.RITE -> PerkInfo("Ночной обряд", "Оружие чаще, вспышки шире — всей ночи.")
                FactionPerk.HOLD -> PerkInfo("Хватка тьмы", "Броня и магнит душ всем вампирам.")
                FactionPerk.NIGHTLAW -> PerkInfo("Закон ночи", "Тьма считает экран чуть темнее. Рит работает сильнее.")
            }
            Faction.HOLY -> when (perk) {
                FactionPerk.BLOODLINE -> PerkInfo("Устав ордена", "Здоровье всему Святому ордену.")
                FactionPerk.HUNT -> PerkInfo("Кара ордена", "Урон всему Святому ордену.")
                FactionPerk.RITE -> PerkInfo("Часы молитвы", "Оружие чаще, вспышки шире — всему ордену.")
                FactionPerk.HOLD -> PerkInfo("Щит веры", "Броня и магнит душ всему ордену.")
                FactionPerk.NIGHTLAW -> PerkInfo("Закон света", "Свет считает экран чуть ярче. Рит работает сильнее.")
            }
        }
    }

    fun resolve(prefs: Prefs, def: CharacterDef): MetaMods {
        val h = { p: HeroPerk -> prefs.heroRank(def.id, p) }
        val f = { p: FactionPerk -> prefs.factionRank(def.faction, p) }
        val craft = h(HeroPerk.CRAFT)
        val start = when {
            craft >= 5 -> 3
            craft >= 3 -> 2
            else -> 1
        }
        return MetaMods(
            hpMul = 1f + h(HeroPerk.VITALITY) * 0.10f + f(FactionPerk.BLOODLINE) * 0.06f,
            dmgMul = 1f + h(HeroPerk.MIGHT) * 0.07f + f(FactionPerk.HUNT) * 0.05f,
            spdMul = 1f + h(HeroPerk.SWIFT) * 0.05f,
            cdMul = (1f - h(HeroPerk.CRAFT) * 0.04f - f(FactionPerk.RITE) * 0.03f).coerceAtLeast(0.70f),
            areaMul = 1f + h(HeroPerk.CRAFT) * 0.04f + f(FactionPerk.RITE) * 0.03f,
            magnetAdd = h(HeroPerk.GREED) * 14f + f(FactionPerk.HOLD) * 10f,
            armorAdd = f(FactionPerk.HOLD) * 0.03f,
            xpMul = 1f + h(HeroPerk.GREED) * 0.08f,
            startSigLevel = start,
            brightShift = f(FactionPerk.NIGHTLAW) * 0.035f,
        )
    }

    fun score(world: World): Int {
        val timePts = (world.time / 8f).toInt()
        val killPts = world.kills / 5
        val lvPts = world.level * 2
        val dawn = if (world.end == RunEnd.DAWN) 70 else 0
        val linger = if (world.end == RunEnd.DAWN) 0 else (world.time / 25f).toInt()
        return (timePts + killPts + lvPts + dawn + linger).coerceAtLeast(4)
    }

    fun award(prefs: Prefs, world: World): Int {
        val n = score(world)
        prefs.addCharMarks(world.character.id, n)
        prefs.addFactionMarks(world.character.faction, n)
        return n
    }
}
