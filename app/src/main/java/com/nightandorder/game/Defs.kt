package com.nightandorder.game

enum class CharacterId { MORVAN, LILITH, NIX, LUCIA, HALE, SERA }

enum class WeaponId {
    BLOOD_ORBIT, NIGHT_FANGS, CRIMSON_NOVA, BAT_CLOUD, HEX_BOLT,
    RADIANT_CROSS, JUDGMENT, SANCTUARY, DAWN_RING, PSALM,
    FANG_STORM, BLOOD_ECLIPSE, HEX_SWARM,
    SOLAR_CROWN, FINAL_WORD, HALLOWED_GROUND,
}

enum class EnemyKind { THRALL, BAT, FLAGELLANT, KNIGHT, BOSS }

enum class BoltArt {
    FANG, BAT, ORB, SPEAR, HEX, CROSS;

    val file: String
        get() = when (this) {
            FANG -> "bolt_fang.png"
            BAT -> "bolt_bat.png"
            ORB -> "bolt_orb.png"
            SPEAR -> "bolt_spear.png"
            HEX -> "bolt_hex.png"
            CROSS -> "bolt_cross.png"
        }

    val faces: Boolean
        get() = this != ORB

    val fallback: Int
        get() = when (this) {
            FANG -> 0xFFE8E0D0.toInt()
            BAT -> 0xFF8B2030.toInt()
            ORB -> 0xFFB02030.toInt()
            SPEAR -> 0xFFE8D48A.toInt()
            HEX -> 0xFFC04088.toInt()
            CROSS -> 0xFFE8D48A.toInt()
        }

    val sizeMul: Float
        get() = when (this) {
            BAT -> 3.8f
            SPEAR, FANG -> 3.5f
            CROSS -> 3.3f
            HEX -> 3.1f
            ORB -> 2.8f
        }
}

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
            "Живёт дольше всех, ходит медленно. Вокруг него кружат сферы крови.",
            hp = 140f, speed = 88f, radius = 18f,
            signature = WeaponId.BLOOD_ORBIT,
            asset = "char_morvan.png",
        ),
        CharacterDef(
            CharacterId.LILITH, Faction.VAMPIRE,
            "Лилит", "Дикая кровь",
            "Быстрая и хрупкая. Клыки сами находят ближайшего врага.",
            hp = 82f, speed = 124f, radius = 15f,
            signature = WeaponId.NIGHT_FANGS,
            asset = "char_lilith.png",
        ),
        CharacterDef(
            CharacterId.NIX, Faction.VAMPIRE,
            "Никс", "Чернокнижница",
            "Держит дистанцию. Проклятие догоняет цель и взрывается.",
            hp = 74f, speed = 104f, radius = 15f,
            signature = WeaponId.HEX_BOLT,
            asset = "char_nix.png",
        ),
        CharacterDef(
            CharacterId.LUCIA, Faction.HOLY,
            "Луция", "Сестра Ордена",
            "Надёжная середина. Кресты света разлетаются вокруг неё.",
            hp = 112f, speed = 100f, radius = 16f,
            signature = WeaponId.RADIANT_CROSS,
            asset = "char_lucia.png",
        ),
        CharacterDef(
            CharacterId.HALE, Faction.HOLY,
            "Хейл", "Инквизитор",
            "Бьёт копьями света в ближайших. Ровный и понятный боец.",
            hp = 104f, speed = 108f, radius = 17f,
            signature = WeaponId.JUDGMENT,
            asset = "char_hale.png",
        ),
        CharacterDef(
            CharacterId.SERA, Faction.HOLY,
            "Сера", "Чародейка",
            "Хрупче сестры, бьёт больнее. На врагов падают столпы света.",
            hp = 88f, speed = 102f, radius = 16f,
            signature = WeaponId.PSALM,
            asset = "char_sera.png",
        ),
    )

    val weapons = listOf(
        WeaponDef(WeaponId.BLOOD_ORBIT, Faction.VAMPIRE, "Сферы крови", "Кружат вокруг вас и ранят всех, кого заденут. С уровнем сфер становится больше."),
        WeaponDef(WeaponId.NIGHT_FANGS, Faction.VAMPIRE, "Ночные клыки", "Сами летят в ближайшего врага. С уровнем клыков больше, бьют чаще."),
        WeaponDef(WeaponId.CRIMSON_NOVA, Faction.VAMPIRE, "Багровая вспышка", "Раз в несколько секунд взрыв вокруг вас. С уровнем вспышка шире и больнее."),
        WeaponDef(WeaponId.BAT_CLOUD, Faction.VAMPIRE, "Стая крыльев", "Летучие мыши разлетаются во все стороны. С уровнем их больше."),
        WeaponDef(WeaponId.HEX_BOLT, Faction.VAMPIRE, "Проклятие", "Самонаводящийся снаряд. Попадает — взрывается. С уровнем взрыв больше."),
        WeaponDef(WeaponId.RADIANT_CROSS, Faction.HOLY, "Кресты света", "Осколки разлетаются кругом. С уровнем их больше, на пятом уровне — ещё кольцо."),
        WeaponDef(WeaponId.JUDGMENT, Faction.HOLY, "Приговор", "Копья света бьют в ближайших врагов. С уровнем копий больше."),
        WeaponDef(WeaponId.SANCTUARY, Faction.HOLY, "Святое поле", "Постоянный круг вокруг вас, жжёт всех внутри. С уровнем круг шире."),
        WeaponDef(WeaponId.DAWN_RING, Faction.HOLY, "Кольцо зари", "Волна лучей во все стороны. С уровнем лучей больше."),
        WeaponDef(WeaponId.PSALM, Faction.HOLY, "Псалом", "На ближайших врагов падают столпы света. С уровнем целей больше."),
        WeaponDef(WeaponId.FANG_STORM, Faction.VAMPIRE, "Стая клыков", "Слияние клыков и крыльев: сразу несколько клыков в разных врагов."),
        WeaponDef(WeaponId.BLOOD_ECLIPSE, Faction.VAMPIRE, "Кровавое затмение", "Слияние сфер и вспышки: орбита плюс сильный взрыв вокруг вас."),
        WeaponDef(WeaponId.HEX_SWARM, Faction.VAMPIRE, "Рой проклятий", "Слияние проклятия и клыков: три снаряда сами находят цели и взрываются."),
        WeaponDef(WeaponId.SOLAR_CROWN, Faction.HOLY, "Солнечный венец", "Слияние крестов и кольца зари: плотная волна святых осколков."),
        WeaponDef(WeaponId.FINAL_WORD, Faction.HOLY, "Последнее слово", "Слияние приговора и псалма: копьё и столп света в одну цель."),
        WeaponDef(WeaponId.HALLOWED_GROUND, Faction.HOLY, "Святая земля", "Слияние поля и крестов: широкий круг, который жжёт постоянно."),
    )

    data class Evolution(
        val a: WeaponId,
        val b: WeaponId,
        val result: WeaponId,
    )

    val evolutions = listOf(
        Evolution(WeaponId.NIGHT_FANGS, WeaponId.BAT_CLOUD, WeaponId.FANG_STORM),
        Evolution(WeaponId.BLOOD_ORBIT, WeaponId.CRIMSON_NOVA, WeaponId.BLOOD_ECLIPSE),
        Evolution(WeaponId.HEX_BOLT, WeaponId.NIGHT_FANGS, WeaponId.HEX_SWARM),
        Evolution(WeaponId.RADIANT_CROSS, WeaponId.DAWN_RING, WeaponId.SOLAR_CROWN),
        Evolution(WeaponId.JUDGMENT, WeaponId.PSALM, WeaponId.FINAL_WORD),
        Evolution(WeaponId.SANCTUARY, WeaponId.RADIANT_CROSS, WeaponId.HALLOWED_GROUND),
    )

    val passives = listOf(
        PassiveDef(PassiveId.HP, "Живучесть", "Больше здоровья. Каждый уровень добавляет запас жизни сразу."),
        PassiveDef(PassiveId.SPEED, "Скорость", "Вы бегаете быстрее и проще обходите толпу."),
        PassiveDef(PassiveId.DAMAGE, "Ярость", "Все атаки наносят больше урона."),
        PassiveDef(PassiveId.COOLDOWN, "Скорость чар", "Оружие срабатывает чаще."),
        PassiveDef(PassiveId.AREA, "Размах", "Вспышки, поля и взрывы покрывают большую площадь."),
        PassiveDef(PassiveId.MAGNET, "Магнит душ", "Сферы опыта сами летят к вам. С уровнем — дальше и быстрее."),
        PassiveDef(PassiveId.ARMOR, "Броня", "Вы получаете меньше урона от касаний."),
        PassiveDef(PassiveId.PROJECTILES, "Залп", "Оружие выпускает дополнительный снаряд."),
    )

    fun character(id: CharacterId) = characters.first { it.id == id }
    fun weapon(id: WeaponId) = weapons.first { it.id == id }
    fun passive(id: PassiveId) = passives.first { it.id == id }

    private val evoResults = evolutions.map { it.result }.toSet()
    fun weaponsFor(faction: Faction) = weapons.filter { it.faction == faction && it.id !in evoResults }
    fun readyEvo(owned: Set<WeaponId>, levels: (WeaponId) -> Int): Evolution? {
        return evolutions.firstOrNull { evo ->
            evo.result !in owned &&
                evo.a in owned && evo.b in owned &&
                levels(evo.a) >= 4 && levels(evo.b) >= 4
        }
    }
}
