package com.nightandorder.game

enum class CharacterId { MORVAN, LILITH, NIX, LUCIA, HALE, SERA }

enum class WeaponId {
    BLOOD_ORBIT, NIGHT_FANGS, CRIMSON_NOVA, BAT_CLOUD, HEX_BOLT,
    RADIANT_CROSS, JUDGMENT, SANCTUARY, DAWN_RING, PSALM,
}

enum class EnemyKind { THRALL, BAT, FLAGELLANT, KNIGHT, BOSS }

enum class OfferKind { WEAPON, PASSIVE }

enum class PassiveId { HP, SPEED, DAMAGE, COOLDOWN, AREA, MAGNET, ARMOR, PROJECTILES }

data class CharacterDef(
    val id: CharacterId,
    val faction: Faction,
    val name: String,
    val title: String,
    val lore: String,
    val hp: Float,
    val speed: Float,
    val radius: Float,
    val signature: WeaponId,
    val asset: String,
)

data class WeaponDef(
    val id: WeaponId,
    val faction: Faction,
    val name: String,
    val blurb: String,
    val maxLevel: Int = 8,
)

data class PassiveDef(
    val id: PassiveId,
    val name: String,
    val blurb: String,
    val maxLevel: Int = 5,
)

object Catalog {
    val characters = listOf(
        CharacterDef(
            CharacterId.MORVAN, Faction.VAMPIRE,
            "Морван", "Ночной граф",
            "Тьма помнит своих.",
            hp = 140f, speed = 88f, radius = 18f,
            signature = WeaponId.BLOOD_ORBIT,
            asset = "char_morvan.png",
        ),
        CharacterDef(
            CharacterId.LILITH, Faction.VAMPIRE,
            "Лилит", "Стая",
            "Чем глубже ночь, тем ближе зубы.",
            hp = 82f, speed = 124f, radius = 15f,
            signature = WeaponId.NIGHT_FANGS,
            asset = "char_lilith.png",
        ),
        CharacterDef(
            CharacterId.NIX, Faction.VAMPIRE,
            "Никс", "Чернокнижница",
            "Заклинание любит тёмную страницу.",
            hp = 74f, speed = 104f, radius = 15f,
            signature = WeaponId.HEX_BOLT,
            asset = "char_nix.png",
        ),
        CharacterDef(
            CharacterId.LUCIA, Faction.HOLY,
            "Луция", "Сестра сияния",
            "Свет не прощает тени.",
            hp = 112f, speed = 100f, radius = 16f,
            signature = WeaponId.RADIANT_CROSS,
            asset = "char_lucia.png",
        ),
        CharacterDef(
            CharacterId.HALE, Faction.HOLY,
            "Хейл", "Инквизитор",
            "Рассвет — наш приговор.",
            hp = 104f, speed = 108f, radius = 17f,
            signature = WeaponId.JUDGMENT,
            asset = "char_hale.png",
        ),
        CharacterDef(
            CharacterId.SERA, Faction.HOLY,
            "Сера", "Чародейка Ордена",
            "Молитва громче на ярком пергаменте.",
            hp = 88f, speed = 102f, radius = 16f,
            signature = WeaponId.PSALM,
            asset = "char_sera.png",
        ),
    )

    val weapons = listOf(
        WeaponDef(WeaponId.BLOOD_ORBIT, Faction.VAMPIRE, "Кровавые сферы", "Кружат и рвут плоть."),
        WeaponDef(WeaponId.NIGHT_FANGS, Faction.VAMPIRE, "Ночные клыки", "Ищут ближайшее горло."),
        WeaponDef(WeaponId.CRIMSON_NOVA, Faction.VAMPIRE, "Багровая вспышка", "Пульс тьмы вокруг вас."),
        WeaponDef(WeaponId.BAT_CLOUD, Faction.VAMPIRE, "Облако крыльев", "Стая рвётся во все стороны."),
        WeaponDef(WeaponId.HEX_BOLT, Faction.VAMPIRE, "Проклятый болт", "Ищет жертву и взрывается."),
        WeaponDef(WeaponId.RADIANT_CROSS, Faction.HOLY, "Сияющий крест", "Святые осколки по кругу."),
        WeaponDef(WeaponId.JUDGMENT, Faction.HOLY, "Приговор", "Копья света в ближайших."),
        WeaponDef(WeaponId.SANCTUARY, Faction.HOLY, "Убежище", "Поле, что жжёт нечисть."),
        WeaponDef(WeaponId.DAWN_RING, Faction.HOLY, "Кольцо зари", "Волна лучей наружу."),
        WeaponDef(WeaponId.PSALM, Faction.HOLY, "Псалом", "Столпы света падают на врагов."),
    )

    val passives = listOf(
        PassiveDef(PassiveId.HP, "Жила", "Больше крови в жилах."),
        PassiveDef(PassiveId.SPEED, "Шаг", "Быстрее по полю."),
        PassiveDef(PassiveId.DAMAGE, "Ярость", "Удары тяжелее."),
        PassiveDef(PassiveId.COOLDOWN, "Ритуал", "Способности чаще."),
        PassiveDef(PassiveId.AREA, "Хватка", "Шире зона удара."),
        PassiveDef(PassiveId.MAGNET, "Жажда", "Самоцветы сами идут."),
        PassiveDef(PassiveId.ARMOR, "Кожа", "Меньше раны."),
        PassiveDef(PassiveId.PROJECTILES, "Эхо", "Ещё один снаряд."),
    )

    fun character(id: CharacterId) = characters.first { it.id == id }
    fun weapon(id: WeaponId) = weapons.first { it.id == id }
    fun passive(id: PassiveId) = passives.first { it.id == id }

    fun weaponsFor(faction: Faction) = weapons.filter { it.faction == faction }
}
