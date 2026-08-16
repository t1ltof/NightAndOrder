package com.nightandorder.game

enum class HeroPerk { VITALITY, MIGHT, SWIFT, CRAFT, GREED, RECOVERY, FURY, LUCK, WARD }

enum class FactionPerk { BLOODLINE, HUNT, RITE, HOLD, NIGHTLAW, HARVEST, MERCY, OMEN }

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
    val regen: Float = 0f,
    val furyMul: Float = 0f,
    val luck: Float = 0f,
    val invulnAdd: Float = 0f,
    val markMul: Float = 1f,
    val healOnGem: Float = 0f,
    val eventTimeMul: Float = 1f,
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
            HeroPerk.RECOVERY -> "Понемногу восстанавливает здоровье во время забега."
            HeroPerk.FURY -> "После убийства коротко усиливает удар."
            HeroPerk.LUCK -> "Чаще выпадают редкие сферы."
            HeroPerk.WARD -> "После удара дольше нельзя задеть. Чуть больше брони."
        }
        val name = when (id) {
            CharacterId.MORVAN -> when (perk) {
                HeroPerk.VITALITY -> "Жила"
                HeroPerk.MIGHT -> "Тяжёлая кровь"
                HeroPerk.SWIFT -> "Шаг графа"
                HeroPerk.CRAFT -> "Наследие ночи"
                HeroPerk.GREED -> "Жадность склепа"
                HeroPerk.RECOVERY -> "Тихая кровь"
                HeroPerk.FURY -> "Гнев склепа"
                HeroPerk.LUCK -> "Старый клад"
                HeroPerk.WARD -> "Каменный плащ"
            }
            CharacterId.LILITH -> when (perk) {
                HeroPerk.VITALITY -> "Дикая живучесть"
                HeroPerk.MIGHT -> "Острые клыки"
                HeroPerk.SWIFT -> "Хищница"
                HeroPerk.CRAFT -> "Стайный инстинкт"
                HeroPerk.GREED -> "Жажда"
                HeroPerk.RECOVERY -> "Заживление"
                HeroPerk.FURY -> "Ярость охоты"
                HeroPerk.LUCK -> "Удачный клык"
                HeroPerk.WARD -> "Ускользание"
            }
            CharacterId.NIX -> when (perk) {
                HeroPerk.VITALITY -> "Костяной остов"
                HeroPerk.MIGHT -> "Чёрное пламя"
                HeroPerk.SWIFT -> "Далёкая тень"
                HeroPerk.CRAFT -> "Петля проклятия"
                HeroPerk.GREED -> "Сбор шёпота"
                HeroPerk.RECOVERY -> "Холодный покой"
                HeroPerk.FURY -> "Чёрный всплеск"
                HeroPerk.LUCK -> "Кривая монета"
                HeroPerk.WARD -> "Пепел"
            }
            CharacterId.LUCIA -> when (perk) {
                HeroPerk.VITALITY -> "Обет"
                HeroPerk.MIGHT -> "Кара сестры"
                HeroPerk.SWIFT -> "Шаг процессии"
                HeroPerk.CRAFT -> "Литания"
                HeroPerk.GREED -> "Сбор света"
                HeroPerk.RECOVERY -> "Покров"
                HeroPerk.FURY -> "Святой гнев"
                HeroPerk.LUCK -> "Милость"
                HeroPerk.WARD -> "Покров сестры"
            }
            CharacterId.HALE -> when (perk) {
                HeroPerk.VITALITY -> "Железная вера"
                HeroPerk.MIGHT -> "Тяжёлый приговор"
                HeroPerk.SWIFT -> "Шаг инквизитора"
                HeroPerk.CRAFT -> "Допрос"
                HeroPerk.GREED -> "Конфискат"
                HeroPerk.RECOVERY -> "Перевязь"
                HeroPerk.FURY -> "Казнь"
                HeroPerk.LUCK -> "Трофей"
                HeroPerk.WARD -> "Забрало"
            }
            CharacterId.SERA -> when (perk) {
                HeroPerk.VITALITY -> "Тонкая плоть"
                HeroPerk.MIGHT -> "Чистый свет"
                HeroPerk.SWIFT -> "Лёгкий шаг"
                HeroPerk.CRAFT -> "Псалом шире"
                HeroPerk.GREED -> "Дары алтаря"
                HeroPerk.RECOVERY -> "Тихий псалом"
                HeroPerk.FURY -> "Вспышка хора"
                HeroPerk.LUCK -> "Жребий света"
                HeroPerk.WARD -> "Тонкий щит"
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
                FactionPerk.HARVEST -> PerkInfo("Дань ночи", "За забег чуть больше крови и опыта — всей ночи.")
                FactionPerk.MERCY -> PerkInfo("Глоток", "Сферы опыта слегка лечат всех вампиров.")
                FactionPerk.OMEN -> PerkInfo("Знамение луны", "События ночи длятся дольше.")
            }
            Faction.HOLY -> when (perk) {
                FactionPerk.BLOODLINE -> PerkInfo("Устав ордена", "Здоровье всему Святому ордену.")
                FactionPerk.HUNT -> PerkInfo("Кара ордена", "Урон всему Святому ордену.")
                FactionPerk.RITE -> PerkInfo("Часы молитвы", "Оружие чаще, вспышки шире — всему ордену.")
                FactionPerk.HOLD -> PerkInfo("Щит веры", "Броня и магнит душ всему ордену.")
                FactionPerk.NIGHTLAW -> PerkInfo("Закон света", "Свет считает экран чуть ярче. Рит работает сильнее.")
                FactionPerk.HARVEST -> PerkInfo("Десятина", "За забег чуть больше печатей и опыта — всему ордену.")
                FactionPerk.MERCY -> PerkInfo("Причастие", "Сферы опыта слегка лечат весь орден.")
                FactionPerk.OMEN -> PerkInfo("Знамение зари", "События ночи длятся дольше.")
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
        val r = { relic: Relic -> if (prefs.hasRelic(relic)) 1f else 0f }
        val sabbathVamp = if (def.faction == Faction.VAMPIRE) r(Relic.SABBATH) else 0f
        val sabbathHoly = if (def.faction == Faction.HOLY) r(Relic.SABBATH) else 0f
        return MetaMods(
            hpMul = 1f + h(HeroPerk.VITALITY) * 0.10f + f(FactionPerk.BLOODLINE) * 0.06f + r(Relic.FIRST_DAWN) * 0.06f,
            dmgMul = 1f + h(HeroPerk.MIGHT) * 0.07f + f(FactionPerk.HUNT) * 0.05f +
                sabbathVamp * 0.06f + r(Relic.DAILY) * 0.04f,
            spdMul = 1f + h(HeroPerk.SWIFT) * 0.05f,
            cdMul = (1f - h(HeroPerk.CRAFT) * 0.04f - f(FactionPerk.RITE) * 0.03f).coerceAtLeast(0.70f),
            areaMul = 1f + h(HeroPerk.CRAFT) * 0.04f + f(FactionPerk.RITE) * 0.03f + r(Relic.CHAPEL) * 0.07f,
            magnetAdd = h(HeroPerk.GREED) * 14f + f(FactionPerk.HOLD) * 10f +
                r(Relic.PALE_DEATH) * 16f + sabbathHoly * 18f,
            armorAdd = f(FactionPerk.HOLD) * 0.03f + r(Relic.FOG) * 0.03f + h(HeroPerk.WARD) * 0.02f,
            xpMul = 1f + h(HeroPerk.GREED) * 0.08f + r(Relic.PALE_DEATH) * 0.06f,
            startSigLevel = start,
            brightShift = f(FactionPerk.NIGHTLAW) * 0.035f,
            regen = h(HeroPerk.RECOVERY) * 0.0028f,
            furyMul = h(HeroPerk.FURY) * 0.07f,
            luck = h(HeroPerk.LUCK) * 0.035f,
            invulnAdd = h(HeroPerk.WARD) * 0.06f,
            markMul = 1f + f(FactionPerk.HARVEST) * 0.08f,
            healOnGem = h(HeroPerk.RECOVERY) * 0.004f + f(FactionPerk.MERCY) * 0.012f,
            eventTimeMul = 1f + f(FactionPerk.OMEN) * 0.10f,
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
        val n = (score(world) * world.meta.markMul).toInt().coerceAtLeast(4)
        prefs.addCharMarks(world.character.id, n)
        prefs.addFactionMarks(world.character.faction, n)
        return n
    }
}
