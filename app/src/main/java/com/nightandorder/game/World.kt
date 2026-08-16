package com.nightandorder.game

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

class Player(val def: CharacterDef) {
    var x = 0f
    var y = 0f
    var vx = 0f
    var vy = 0f
    var hp = def.hp
    var maxHp = def.hp
    var invuln = 0f
    var facing = 1f
}

class Enemy(
    var kind: EnemyKind,
    var x: Float,
    var y: Float,
    var hp: Float,
    var maxHp: Float,
    var speed: Float,
    var radius: Float,
    var damage: Float,
    var xp: Int,
    var touchCd: Float = 0f,
    var facing: Float = 1f,
    var role: Role = Role.MOB,
    var invuln: Boolean = false,
)

enum class Role { MOB, BOSS, SERVANT }

enum class GemKind { SOUL, GREATER, VITAL }

class Projectile(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var radius: Float,
    var damage: Float,
    var pierce: Int,
    var holy: Boolean,
    var art: BoltArt,
    var orbit: Boolean = false,
    var orbitAngle: Float = 0f,
    var orbitRadius: Float = 0f,
    var orbitSpeed: Float = 0f,
    var seek: Boolean = false,
    var seekTurn: Float = 0f,
    var explode: Boolean = false,
    var explodeRadius: Float = 0f,
)

class Pickup(
    var x: Float,
    var y: Float,
    var value: Int,
    var kind: GemKind = GemKind.SOUL,
    var life: Float = 40f,
)

class FloatNum(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,
    var life: Float = 0.7f,
    val maxLife: Float = 0.7f,
)

class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var life: Float,
    var maxLife: Float,
    var color: Int,
    var size: Float,
)

class WeaponInst(val id: WeaponId, var level: Int) {
    var cd = 0f
}

class Passives {
    val level = HashMap<PassiveId, Int>()
    fun lv(id: PassiveId) = level[id] ?: 0
}

sealed class Offer(val title: String, val body: String) {
    class WeaponNew(val id: WeaponId, title: String, body: String) : Offer(title, body)
    class WeaponUp(val id: WeaponId, val next: Int, title: String, body: String) : Offer(title, body)
    class PassiveUp(val id: PassiveId, val next: Int, title: String, body: String) : Offer(title, body)
    class Evolve(val evo: Catalog.Evolution, title: String, body: String) : Offer(title, body)
    class Ritual(title: String, body: String) : Offer(title, body)
    class Curse(title: String, body: String) : Offer(title, body)
}

enum class BossPhase { GUARD, OPEN, RAGE }

enum class RunEnd { DEAD, DAWN }

class World(
    val character: CharacterDef,
    val meta: MetaMods = MetaMods(),
    startRite: RiteMods = RiteMods(),
) {
    val player = Player(character)
    val enemies = ArrayList<Enemy>(256)
    val projectiles = ArrayList<Projectile>(160)
    val pickups = ArrayList<Pickup>(256)
    val particles = ArrayList<Particle>(200)
    val floats = ArrayList<FloatNum>(64)
    val events = ArrayList<Cue>(8)
    val weapons = ArrayList<WeaponInst>()
    val passives = Passives()
    val rng = Random(System.nanoTime())

    var time = 0f
    var kills = 0
    var level = 1
    var xp = 0
    var xpToNext = xpNeed(1)
    var brightness = 0.55f
    var spawnAcc = 0f
    var eliteAcc = 0f
    var nextChest = 90f
    var pendingOffers: List<Offer>? = null
    var pendingChest: List<Offer>? = null
    var ritualLeft = 0f
    var curseMul = 1f
    var dawnStarted = false
    var boss: Enemy? = null
    var bossPhase = BossPhase.GUARD
    var rageCd = 0f
    var humCd = 0f
    private var hitSfxCd = 0f
    private var spawnedAt180 = false
    private var spawnedAt360 = false
    var end: RunEnd? = null
    var inputX = 0f
    var inputY = 0f
    var rite = startRite
    var brokenLeft = 0f
    var lookAways = 0

    val power: Power
        get() {
            val shifted = if (character.faction == Faction.VAMPIRE) {
                (brightness - meta.brightShift).coerceIn(0f, 1f)
            } else {
                (brightness + meta.brightShift).coerceIn(0f, 1f)
            }
            return BrightnessPower.of(character.faction, shifted)
        }
    val isDawn get() = time >= 420f

    init {
        player.maxHp = character.hp * meta.hpMul * rite.hpMul
        player.hp = player.maxHp
        weapons += WeaponInst(character.signature, meta.startSigLevel.coerceIn(1, 8))
    }

    fun damageMul(): Float {
        var m = power.damage * (1f + passives.lv(PassiveId.DAMAGE) * 0.18f) * curseMul * meta.dmgMul * rite.dmgMul
        if (ritualLeft > 0f) m *= 1.12f
        if (isDawn && character.faction == Faction.HOLY) m *= 1.22f
        if (brokenLeft > 0f) m *= 0.86f
        return m
    }
    fun cooldownMul() = (1f / power.ability) * (1f - passives.lv(PassiveId.COOLDOWN) * 0.08f).coerceAtLeast(0.45f) * meta.cdMul * rite.cdMul
    fun areaMul() = (0.85f + power.ability * 0.25f) * (1f + passives.lv(PassiveId.AREA) * 0.12f) * meta.areaMul * rite.areaMul
    fun extraShots() = passives.lv(PassiveId.PROJECTILES)
    fun magnet() = 52f + passives.lv(PassiveId.MAGNET) * 70f + meta.magnetAdd + rite.magnetAdd +
        (if (ritualLeft > 0f) 90f else 0f) + (if (brokenLeft > 0f) -36f else 0f)
    fun magnetPull() = 200f + passives.lv(PassiveId.MAGNET) * 140f
    fun armor() = (passives.lv(PassiveId.ARMOR) * 0.08f + meta.armorAdd + rite.armorAdd).coerceIn(0f, 0.72f)
    fun moveSpeed() = character.speed * (1f + passives.lv(PassiveId.SPEED) * 0.08f) * meta.spdMul * rite.spdMul *
        if (ritualLeft > 0f) 1.22f else 1f

    fun lookAway() {
        if (end != null) return
        lookAways += 1
        val skip = (26f + lookAways * 6f).coerceAtMost(50f)
        time = (time + skip).coerceAtMost(RUN_SECONDS - 5f)
        brokenLeft = 18f + lookAways * 5f
        emit(Cue.HUM_DARK)
        pushFloat(player.x, player.y - 22f, "взгляд сорвался", 0xFFC07080.toInt())
    }

    fun emit(c: Cue) { events += c }

    fun tick(dt: Float) {
        tickFloats(dt)
        if (end != null || pendingOffers != null || pendingChest != null) return
        time += dt
        ritualLeft = (ritualLeft - dt).coerceAtLeast(0f)
        brokenLeft = (brokenLeft - dt).coerceAtLeast(0f)
        hitSfxCd = (hitSfxCd - dt).coerceAtLeast(0f)
        if (time >= RUN_SECONDS) {
            end = RunEnd.DAWN
            return
        }

        val spd = moveSpeed()
        val im = hypot(inputX, inputY)
        if (im > 0.12f) {
            val nx = inputX / im
            val ny = inputY / im
            player.vx = nx * spd
            player.vy = ny * spd
            if (nx != 0f) player.facing = if (nx > 0f) 1f else -1f
        } else {
            player.vx *= 0.7f
            player.vy *= 0.7f
        }
        player.x += player.vx * dt
        player.y += player.vy * dt
        val pushed = Field.pushOut(player.x, player.y, player.def.radius)
        player.x = pushed.first
        player.y = pushed.second
        player.invuln = (player.invuln - dt).coerceAtLeast(0f)

        tickWeapons(dt)
        tickProjectiles(dt)
        spawn(dt)
        tickEnemies(dt)
        tickBoss(dt)
        collide()
        tickPickups(dt)
        tickParticles(dt)
        tickDawn(dt)
        tickChest()
        tickHum(dt)
    }

    private fun tickWeapons(dt: Float) {
        val p = player
        val dmg = damageMul()
        val area = areaMul()
        val shots = extraShots()
        for (w in weapons) {
            w.cd -= dt
            when (w.id) {
                WeaponId.BLOOD_ORBIT -> {
                    val want = 2 + w.level / 2 + shots
                    val have = projectiles.count { it.orbit && !it.holy }
                    if (have < want) {
                        val ang = (have * (Math.PI * 2.0 / want)).toFloat()
                        projectiles += Projectile(
                            p.x, p.y, 0f, 0f,
                            life = 999f,
                            radius = 9f * area,
                            damage = (7f + w.level * 2.2f) * dmg,
                            pierce = 99,
                            holy = false,
                            art = BoltArt.ORB,
                            orbit = true,
                            orbitAngle = ang,
                            orbitRadius = 46f + w.level * 6f,
                            orbitSpeed = 2.1f + w.level * 0.12f,
                        )
                    }
                }
                WeaponId.NIGHT_FANGS -> {
                    val interval = (0.55f - w.level * 0.03f).coerceAtLeast(0.18f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 1 + w.level / 3 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x) + (i - n / 2f) * 0.12f
                            shoot(p.x, p.y, a, 260f, (9f + w.level * 2.4f) * dmg, 0.7f, 6f * area, 1, false, BoltArt.FANG)
                        }
                    }
                }
                WeaponId.CRIMSON_NOVA -> {
                    val interval = (2.4f - w.level * 0.12f).coerceAtLeast(1.0f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        nova(p.x, p.y, (52f + w.level * 8f) * area, (16f + w.level * 4f) * dmg, false)
                    }
                }
                WeaponId.BAT_CLOUD -> {
                    val interval = (1.15f - w.level * 0.06f).coerceAtLeast(0.45f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 4 + w.level / 2 + shots
                        repeat(n) { i ->
                            val a = i * (Math.PI.toFloat() * 2f / n) + time
                            shoot(p.x, p.y, a, 180f, (6f + w.level * 1.6f) * dmg, 0.85f, 7f * area, 0, false, BoltArt.BAT)
                        }
                    }
                }
                WeaponId.RADIANT_CROSS -> {
                    val interval = (0.85f - w.level * 0.04f).coerceAtLeast(0.28f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 4 + (if (w.level >= 5) 4 else 0) + shots
                        repeat(n) { i ->
                            val a = i * (Math.PI.toFloat() * 2f / n) + time * 0.4f
                            shoot(p.x, p.y, a, 220f, (8f + w.level * 2.1f) * dmg, 0.9f, 7f * area, 1, true, BoltArt.CROSS)
                        }
                    }
                }
                WeaponId.JUDGMENT -> {
                    val interval = (0.62f - w.level * 0.035f).coerceAtLeast(0.2f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 1 + w.level / 3 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x)
                            shoot(p.x, p.y, a, 300f, (10f + w.level * 2.6f) * dmg, 0.8f, 6f * area, 2, true, BoltArt.SPEAR)
                        }
                    }
                }
                WeaponId.SANCTUARY -> {
                    val interval = 0.28f * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val r = (44f + w.level * 7f) * area
                        val hit = (4.5f + w.level * 1.3f) * dmg
                        for (e in enemies) {
                            if (dist2(e.x, e.y, p.x, p.y) <= r * r) {
                                hurtEnemy(e, hit)
                            }
                        }
                    }
                }
                WeaponId.DAWN_RING -> {
                    val interval = (1.6f - w.level * 0.08f).coerceAtLeast(0.7f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 8 + w.level + shots
                        repeat(n) { i ->
                            val a = i * (Math.PI.toFloat() * 2f / n)
                            shoot(p.x, p.y, a, 200f, (7f + w.level * 1.8f) * dmg, 0.75f, 6.5f * area, 0, true, BoltArt.CROSS)
                        }
                    }
                }
                WeaponId.HEX_BOLT -> {
                    val interval = (0.72f - w.level * 0.04f).coerceAtLeast(0.28f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 1 + w.level / 4 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x) + (i - n / 2f) * 0.18f
                            projectiles += Projectile(
                                p.x, p.y, cos(a) * 170f, sin(a) * 170f,
                                life = 1.35f,
                                radius = 8f * area,
                                damage = (11f + w.level * 3.1f) * dmg,
                                pierce = 1,
                                holy = false,
                                art = BoltArt.HEX,
                                seek = true,
                                seekTurn = 7.5f * rite.seekMul,
                                explode = true,
                                explodeRadius = (28f + w.level * 4f) * area,
                            )
                        }
                    }
                }
                WeaponId.PSALM -> {
                    val interval = (1.05f - w.level * 0.05f).coerceAtLeast(0.42f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 1 + w.level / 3 + shots
                        val r = (34f + w.level * 5f) * area
                        val hit = (14f + w.level * 3.4f) * dmg
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            nova(t.x, t.y, r, hit, holy = true)
                        }
                    }
                }
                WeaponId.FANG_STORM -> {
                    val interval = (0.38f - w.level * 0.02f).coerceAtLeast(0.16f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 3 + w.level / 2 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x) + (i - n / 2f) * 0.1f
                            shoot(p.x, p.y, a, 280f, (11f + w.level * 2.8f) * dmg, 0.65f, 6f * area, 1, false, BoltArt.FANG)
                        }
                    }
                }
                WeaponId.BLOOD_ECLIPSE -> {
                    val interval = (1.8f - w.level * 0.08f).coerceAtLeast(0.8f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        nova(p.x, p.y, (64f + w.level * 9f) * area, (22f + w.level * 5f) * dmg, false)
                    }
                    val want = 3 + w.level / 2 + shots
                    val have = projectiles.count { it.orbit && !it.holy }
                    if (have < want) {
                        projectiles += Projectile(
                            p.x, p.y, 0f, 0f, 999f, 10f * area,
                            (9f + w.level * 2.4f) * dmg, 99, false, BoltArt.ORB,
                            orbit = true, orbitRadius = 50f + w.level * 5f, orbitSpeed = 2.4f,
                        )
                    }
                }
                WeaponId.HEX_SWARM -> {
                    val interval = (0.55f - w.level * 0.03f).coerceAtLeast(0.22f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 3 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x) + (i - 1) * 0.22f
                            projectiles += Projectile(
                                p.x, p.y, cos(a) * 190f, sin(a) * 190f,
                                1.2f, 8f * area, (13f + w.level * 3.2f) * dmg, 1, false,
                                BoltArt.HEX,
                                seek = true, seekTurn = 9f * rite.seekMul, explode = true,
                                explodeRadius = (32f + w.level * 5f) * area,
                            )
                        }
                    }
                }
                WeaponId.SOLAR_CROWN -> {
                    val interval = (0.7f - w.level * 0.03f).coerceAtLeast(0.25f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 8 + w.level + shots
                        repeat(n) { i ->
                            val a = i * (Math.PI.toFloat() * 2f / n) + time
                            shoot(p.x, p.y, a, 240f, (9f + w.level * 2.2f) * dmg, 0.85f, 7f * area, 1, true, BoltArt.CROSS)
                        }
                    }
                }
                WeaponId.FINAL_WORD -> {
                    val interval = (0.7f - w.level * 0.03f).coerceAtLeast(0.26f) * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val n = 2 + w.level / 3 + shots
                        repeat(n) { i ->
                            val t = nearest(p.x, p.y, skip = i) ?: return@repeat
                            val a = atan2(t.y - p.y, t.x - p.x)
                            shoot(p.x, p.y, a, 320f, (12f + w.level * 3f) * dmg, 0.8f, 6f * area, 2, true, BoltArt.SPEAR)
                            nova(t.x, t.y, (28f + w.level * 4f) * area, (10f + w.level * 2.4f) * dmg, true)
                        }
                    }
                }
                WeaponId.HALLOWED_GROUND -> {
                    val interval = 0.22f * cooldownMul()
                    if (w.cd <= 0f) {
                        w.cd = interval
                        val r = (56f + w.level * 8f) * area
                        val hit = (6f + w.level * 1.6f) * dmg
                        for (e in enemies) {
                            if (e.hp <= 0f) continue
                            if (dist2(e.x, e.y, p.x, p.y) <= r * r) hurtEnemy(e, hit)
                        }
                    }
                }
            }
        }
        var orbitCount = 0
        for (pr in projectiles) if (pr.orbit && !pr.holy) orbitCount++
        if (orbitCount > 0) {
            var i = 0
            for (pr in projectiles) {
                if (!pr.orbit || pr.holy) continue
                pr.orbitAngle += pr.orbitSpeed * dt
                val a = i * (Math.PI.toFloat() * 2f / orbitCount) + pr.orbitAngle
                pr.x = p.x + cos(a) * pr.orbitRadius
                pr.y = p.y + sin(a) * pr.orbitRadius
                i++
            }
        }
    }

    private fun shoot(
        x: Float, y: Float, a: Float, speed: Float,
        damage: Float, life: Float, radius: Float, pierce: Int, holy: Boolean,
        art: BoltArt,
    ) {
        projectiles += Projectile(
            x, y, cos(a) * speed, sin(a) * speed,
            life, radius, damage, pierce, holy, art,
        )
    }

    private fun nova(x: Float, y: Float, r: Float, dmg: Float, holy: Boolean) {
        for (e in enemies) {
            if (dist2(e.x, e.y, x, y) <= r * r) hurtEnemy(e, dmg)
        }
        burst(x, y, if (holy) 0xFFE8D48A.toInt() else 0xFF8B1E2D.toInt(), 8, r * 0.6f)
    }

    private fun tickProjectiles(dt: Float) {
        val it = projectiles.iterator()
        while (it.hasNext()) {
            val pr = it.next()
            if (!pr.orbit) {
                if (pr.seek) {
                    val t = nearest(pr.x, pr.y, skip = 0)
                    if (t != null) {
                        val speed = hypot(pr.vx, pr.vy).coerceAtLeast(80f)
                        val cur = atan2(pr.vy, pr.vx)
                        val want = atan2(t.y - pr.y, t.x - pr.x)
                        var diff = want - cur
                        val pi = Math.PI.toFloat()
                        while (diff > pi) diff -= pi * 2f
                        while (diff < -pi) diff += pi * 2f
                        val turn = pr.seekTurn * dt
                        val na = cur + diff.coerceIn(-turn, turn)
                        pr.vx = cos(na) * speed
                        pr.vy = sin(na) * speed
                    }
                }
                pr.x += pr.vx * dt
                pr.y += pr.vy * dt
                pr.life -= dt
                if (pr.life <= 0f) {
                    if (pr.explode) nova(pr.x, pr.y, pr.explodeRadius, pr.damage * 0.55f, pr.holy)
                    it.remove()
                    continue
                }
            }
            var hit = false
            val reach = 70f
            for (e in enemies) {
                if (e.hp <= 0f) continue
                val dx = e.x - pr.x
                if (dx > reach || dx < -reach) continue
                val dy = e.y - pr.y
                if (dy > reach || dy < -reach) continue
                val rr = pr.radius + e.radius
                if (dx * dx + dy * dy <= rr * rr) {
                    hurtEnemy(e, pr.damage)
                    hit = true
                    if (!pr.orbit) {
                        pr.pierce -= 1
                        if (pr.pierce < 0) break
                    }
                }
            }
            if (hit && !pr.orbit && pr.pierce < 0) {
                if (pr.explode) nova(pr.x, pr.y, pr.explodeRadius, pr.damage * 0.55f, pr.holy)
                it.remove()
            }
        }
        if (projectiles.size > 180) {
            projectiles.subList(0, projectiles.size - 180).clear()
        }
    }

    private fun spawn(dt: Float) {
        val cap = when {
            time > 360f -> 70
            time > 240f -> 55
            time > 120f -> 45
            else -> 32
        }
        val rate = (1.1f + time / 90f) * rite.spawnMul
        spawnAcc += dt * rate
        while (spawnAcc >= 1f && enemies.size < cap) {
            spawnAcc -= 1f
            val kind = rollKind()
            val (x, y) = spawnPoint()
            enemies += makeEnemy(kind, x, y)
        }
        eliteAcc += dt
        if (eliteAcc > 55f && enemies.size < cap) {
            eliteAcc = 0f
            val (x, y) = spawnPoint()
            enemies += makeEnemy(if (time > 180f) EnemyKind.KNIGHT else EnemyKind.FLAGELLANT, x, y).also {
                it.hp *= 2.2f
                it.maxHp = it.hp
                it.xp += 8
            }
        }
        if (time >= 180f && !spawnedAt180) {
            spawnedAt180 = true
            spawnBoss()
        }
        if (time >= 360f && !spawnedAt360) {
            spawnedAt360 = true
            spawnBoss()
        }
    }

    private fun rollKind(): EnemyKind {
        if (rng.nextFloat() < rite.batBias.coerceAtLeast(0f)) return EnemyKind.BAT
        val t = time
        val r = rng.nextFloat()
        return when {
            t < 40f -> if (r < 0.85f) EnemyKind.THRALL else EnemyKind.BAT
            t < 120f -> when {
                r < 0.55f -> EnemyKind.THRALL
                r < 0.85f -> EnemyKind.BAT
                else -> EnemyKind.FLAGELLANT
            }
            t < 240f -> when {
                r < 0.35f -> EnemyKind.THRALL
                r < 0.6f -> EnemyKind.BAT
                r < 0.88f -> EnemyKind.FLAGELLANT
                else -> EnemyKind.KNIGHT
            }
            else -> when {
                r < 0.2f -> EnemyKind.THRALL
                r < 0.4f -> EnemyKind.BAT
                r < 0.7f -> EnemyKind.FLAGELLANT
                else -> EnemyKind.KNIGHT
            }
        }
    }

    private fun spawnPoint(): Pair<Float, Float> {
        val a = rng.nextFloat() * Math.PI.toFloat() * 2f
        val d = 380f + rng.nextFloat() * 80f
        var x = player.x + cos(a) * d
        var y = player.y + sin(a) * d
        repeat(4) {
            if (!Field.blocked(x, y, 20f)) return x to y
            val b = rng.nextFloat() * Math.PI.toFloat() * 2f
            x = player.x + cos(b) * d
            y = player.y + sin(b) * d
        }
        return x to y
    }

    private fun makeEnemy(kind: EnemyKind, x: Float, y: Float): Enemy {
        val scale = 1f + time / 420f
        return when (kind) {
            EnemyKind.THRALL -> Enemy(kind, x, y, 18f * scale, 18f * scale, 46f, 14f, 7f, 1)
            EnemyKind.BAT -> Enemy(kind, x, y, 12f * scale, 12f * scale, 78f, 12f, 6f, 1)
            EnemyKind.FLAGELLANT -> Enemy(kind, x, y, 36f * scale, 36f * scale, 52f, 16f, 10f, 2)
            EnemyKind.KNIGHT -> Enemy(kind, x, y, 90f * scale, 90f * scale, 40f, 20f, 16f, 5)
            EnemyKind.BOSS -> Enemy(kind, x, y, 520f * scale, 520f * scale, 34f, 34f, 22f, 40)
        }
    }

    private fun tickEnemies(dt: Float) {
        val px = player.x
        val py = player.y
        for (e in enemies) {
            if (e.hp <= 0f) continue
            val dx = px - e.x
            val dy = py - e.y
            val m = hypot(dx, dy).coerceAtLeast(0.001f)
            val step = e.speed * rite.enemySpdMul
            e.x += dx / m * step * dt
            e.y += dy / m * step * dt
            if (e.kind != EnemyKind.BAT) {
                val ep = Field.pushOut(e.x, e.y, e.radius)
                e.x = ep.first
                e.y = ep.second
            }
            if (dx != 0f) e.facing = if (dx > 0f) 1f else -1f
            e.touchCd = (e.touchCd - dt).coerceAtLeast(0f)
        }
        val n = min(enemies.size, 36)
        for (i in 0 until n) {
            val a = enemies[i]
            if (a.hp <= 0f) continue
            for (j in i + 1 until n) {
                val b = enemies[j]
                if (b.hp <= 0f) continue
                val dx = b.x - a.x
                val dy = b.y - a.y
                val d2 = dx * dx + dy * dy
                val minD = a.radius + b.radius
                if (d2 < minD * minD && d2 > 0.01f) {
                    val d = sqrt(d2)
                    val push = (minD - d) * 0.35f
                    val nx = dx / d
                    val ny = dy / d
                    a.x -= nx * push
                    a.y -= ny * push
                    b.x += nx * push
                    b.y += ny * push
                }
            }
        }
    }

    private fun collide() {
        if (player.invuln > 0f) return
        for (e in enemies) {
            if (e.hp <= 0f || e.touchCd > 0f) continue
            val rr = player.def.radius + e.radius
            if (dist2(player.x, player.y, e.x, e.y) <= rr * rr) {
                val taken = e.damage * (1f - armor()).coerceAtLeast(0.35f)
                player.hp -= taken
                player.invuln = 0.45f
                e.touchCd = 0.35f
                emit(Cue.HURT)
                burst(player.x, player.y, 0xFFCC3344.toInt(), 10, 40f)
                pushFloat(player.x, player.y - 12f, "-${taken.toInt()}", 0xFFFF6677.toInt())
                if (player.hp <= 0f) {
                    player.hp = 0f
                    end = RunEnd.DEAD
                }
                return
            }
        }
    }

    private fun hurtEnemy(e: Enemy, amount: Float) {
        if (e.hp <= 0f) return
        if (e.invuln) return
        e.hp -= amount
        pushFloat(e.x, e.y - 10f, amount.toInt().toString(), 0xFFFFE8A0.toInt())
        if (hitSfxCd <= 0f) {
            emit(Cue.HIT)
            hitSfxCd = 0.06f
        }
        if (e.hp <= 0f) {
            e.hp = 0f
            kills += 1
            if (e.role == Role.BOSS) {
                pickups += Pickup(e.x, e.y, 40, GemKind.GREATER)
                pickups += Pickup(e.x + 12f, e.y, 1, GemKind.VITAL)
                emit(Cue.BOSS)
            } else {
                dropGem(e.x, e.y, e.xp)
            }
            burst(e.x, e.y, 0xFFD0C8B0.toInt(), 6, 28f)
        }
    }

    private fun dropGem(x: Float, y: Float, base: Int) {
        val r = rng.nextFloat()
        val kind = when {
            r < 0.05f -> GemKind.VITAL
            r < 0.16f -> GemKind.GREATER
            else -> GemKind.SOUL
        }
        val value = if (kind == GemKind.GREATER) base * 5 else base
        pickups += Pickup(x, y, value, kind)
    }

    private fun tickPickups(dt: Float) {
        val mag = magnet()
        val pullSpd = magnetPull()
        val it = pickups.iterator()
        while (it.hasNext()) {
            val g = it.next()
            g.life -= dt
            val d = hypot(player.x - g.x, player.y - g.y)
            if (d < mag) {
                val pull = pullSpd * dt
                g.x += (player.x - g.x) / d.coerceAtLeast(1f) * pull
                g.y += (player.y - g.y) / d.coerceAtLeast(1f) * pull
            }
            if (d < 18f) {
                when (g.kind) {
                    GemKind.SOUL -> {
                        addXp(g.value)
                        emit(Cue.GEM)
                    }
                    GemKind.GREATER -> {
                        addXp(g.value)
                        emit(Cue.GEM_RARE)
                    }
                    GemKind.VITAL -> {
                        val heal = player.maxHp * 0.10f
                        player.hp = min(player.maxHp, player.hp + heal)
                        pushFloat(player.x, player.y - 16f, "+${heal.toInt()}", 0xFF66FF99.toInt())
                        emit(Cue.HEAL)
                    }
                }
                it.remove()
            } else if (g.life <= 0f) {
                it.remove()
            }
        }
        enemies.removeAll { it.hp <= 0f }
    }

    private fun addXp(v: Int) {
        if (v <= 0) return
        xp += max(1, (v * meta.xpMul).toInt())
        while (xp >= xpToNext && pendingOffers == null) {
            xp -= xpToNext
            level += 1
            xpToNext = xpNeed(level)
            pendingOffers = rollOffers()
            emit(Cue.LEVEL)
        }
    }

    fun rollOffers(): List<Offer> {
        val pool = ArrayList<Offer>()
        val owned = weapons.map { it.id }.toSet()
        for (w in weapons) {
            if (w.level < Catalog.weapon(w.id).maxLevel) {
                val def = Catalog.weapon(w.id)
                pool += Offer.WeaponUp(w.id, w.level + 1, def.name, "Уровень ${w.level + 1}. ${def.blurb}")
            }
        }
        if (weapons.size < 4) {
            for (def in Catalog.weaponsFor(character.faction)) {
                if (def.id !in owned) {
                    pool += Offer.WeaponNew(def.id, def.name, def.blurb)
                }
            }
        }
        for (def in Catalog.passives) {
            val lv = passives.lv(def.id)
            if (lv < def.maxLevel) {
                pool += Offer.PassiveUp(def.id, lv + 1, def.name, "Уровень ${lv + 1}. ${def.blurb}")
            }
        }
        pool.shuffle(rng)
        return pool.take(3)
    }

    fun pick(offer: Offer) {
        when (offer) {
            is Offer.WeaponNew -> weapons += WeaponInst(offer.id, 1)
            is Offer.WeaponUp -> weapons.first { it.id == offer.id }.level = offer.next
            is Offer.PassiveUp -> {
                passives.level[offer.id] = offer.next
                if (offer.id == PassiveId.HP) {
                    val add = 18f
                    player.maxHp += add
                    player.hp = min(player.maxHp, player.hp + add)
                }
            }
            is Offer.Evolve -> {
                weapons.removeAll { it.id == offer.evo.a || it.id == offer.evo.b }
                weapons += WeaponInst(offer.evo.result, 1)
            }
            is Offer.Ritual -> ritualLeft = 28f
            is Offer.Curse -> {
                curseMul = 1.4f
                val cut = player.maxHp * 0.22f
                player.maxHp = (player.maxHp - cut).coerceAtLeast(30f)
                player.hp = min(player.hp, player.maxHp)
            }
        }
        pendingOffers = null
        pendingChest = null
    }

    private fun tickParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.life -= dt
            if (p.life <= 0f) it.remove()
        }
        if (particles.size > 70) particles.subList(0, particles.size - 70).clear()
    }

    private fun spawnBoss() {
        val (x, y) = spawnPoint()
        val b = makeEnemy(EnemyKind.BOSS, x, y)
        b.role = Role.BOSS
        b.invuln = true
        b.hp *= 1.15f
        b.maxHp = b.hp
        enemies += b
        boss = b
        bossPhase = BossPhase.GUARD
        spawnServants(6)
        emit(Cue.BOSS)
    }

    private fun spawnServants(n: Int) {
        val b = boss ?: return
        repeat(n) { i ->
            val a = i * (Math.PI.toFloat() * 2f / n)
            val s = makeEnemy(EnemyKind.FLAGELLANT, b.x + cos(a) * 70f, b.y + sin(a) * 70f)
            s.role = Role.SERVANT
            s.hp *= 0.8f
            s.maxHp = s.hp
            enemies += s
        }
    }

    private fun servantsAlive() = enemies.count { it.role == Role.SERVANT && it.hp > 0f }

    private fun tickBoss(dt: Float) {
        val b = boss ?: return
        if (b.hp <= 0f) {
            boss = null
            return
        }
        when (bossPhase) {
            BossPhase.GUARD -> {
                b.invuln = servantsAlive() > 0
                if (!b.invuln) bossPhase = BossPhase.OPEN
            }
            BossPhase.OPEN -> {
                b.invuln = false
                if (b.hp < b.maxHp * 0.5f) {
                    bossPhase = BossPhase.RAGE
                    spawnServants(4)
                    emit(Cue.BOSS)
                }
            }
            BossPhase.RAGE -> {
                b.invuln = false
                rageCd -= dt
                val dark = brightness < 0.4f
                if (rageCd <= 0f) {
                    rageCd = if (dark) 1.1f else 1.8f
                    val r = if (dark) 88f else 70f
                    if (dist2(player.x, player.y, b.x, b.y) < r * r) {
                        val taken = if (dark) 14f else 10f
                        player.hp -= taken * (1f - armor())
                        emit(Cue.HURT)
                        pushFloat(player.x, player.y - 14f, "-${taken.toInt()}", 0xFFFF6677.toInt())
                        if (player.hp <= 0f) {
                            player.hp = 0f
                            end = RunEnd.DEAD
                        }
                    }
                    burst(b.x, b.y, if (dark) 0xFF8B1E2D.toInt() else 0xFFE8D48A.toInt(), 10, r)
                }
            }
        }
        var i = 0
        val n = servantsAlive().coerceAtLeast(1)
        for (e in enemies) {
            if (e.role != Role.SERVANT || e.hp <= 0f) continue
            val a = time * 1.2f + i * (Math.PI.toFloat() * 2f / n)
            e.x = b.x + cos(a) * 74f
            e.y = b.y + sin(a) * 74f
            i++
        }
    }

    private fun tickDawn(dt: Float) {
        if (!isDawn) return
        if (!dawnStarted) {
            dawnStarted = true
            emit(Cue.DAWN)
        }
        if (character.faction == Faction.VAMPIRE && brightness > 0.22f) {
            val burn = (3.5f + 14f * brightness) * dt * rite.burnMul * if (brokenLeft > 0f) 1.2f else 1f
            player.hp -= burn
            if (player.hp <= 0f) {
                player.hp = 0f
                end = RunEnd.DEAD
            }
        }
    }

    private fun tickChest() {
        if (time < nextChest || pendingOffers != null || pendingChest != null) return
        nextChest += 90f
        pendingChest = rollChest()
        emit(Cue.CHEST)
    }

    private fun rollChest(): List<Offer> {
        val list = ArrayList<Offer>(3)
        val owned = weapons.map { it.id }.toSet()
        val lv = { id: WeaponId -> weapons.find { it.id == id }?.level ?: 0 }
        val evo = Catalog.readyEvo(owned, lv)
        if (evo != null) {
            val res = Catalog.weapon(evo.result)
            list += Offer.Evolve(
                evo,
                res.name,
                "Сложить два оружия в одно сильнее. ${res.blurb}",
            )
        }
        list += Offer.Ritual("Короткий обряд", "28 секунд: вы бежите быстрее, бьёте чуть сильнее, сферы летят сами.")
        list += Offer.Curse("Кровавая клятва", "Весь забег: +40% урона, но максимальное здоровье падает на 22%.")
        if (list.size < 3) {
            list += Offer.Ritual("Жажда сфер", "На 28 секунд сферы опыта сами летят к вам.")
        }
        return list.take(3)
    }

    private fun tickHum(dt: Float) {
        humCd -= dt
        if (humCd > 0f) return
        humCd = 3.6f
        val rite = power.rite
        if (character.faction == Faction.VAMPIRE && rite > 0.45f) emit(Cue.HUM_DARK)
        else if (character.faction == Faction.HOLY && rite > 0.55f) emit(Cue.HUM_LIGHT)
    }

    private fun tickFloats(dt: Float) {
        val it = floats.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.life -= dt
            f.y -= 22f * dt
            if (f.life <= 0f) it.remove()
        }
        if (floats.size > 40) floats.subList(0, floats.size - 40).clear()
    }

    private fun pushFloat(x: Float, y: Float, text: String, color: Int) {
        floats += FloatNum(x, y, text, color)
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int, speed: Float) {
        repeat(n) {
            val a = rng.nextFloat() * Math.PI.toFloat() * 2f
            val s = speed * (0.3f + rng.nextFloat())
            particles += Particle(x, y, cos(a) * s, sin(a) * s, 0.35f, 0.35f, color, 3f + rng.nextFloat() * 3f)
        }
    }

    private fun nearest(x: Float, y: Float, skip: Int): Enemy? {
        if (skip <= 0) {
            var best: Enemy? = null
            var bestD = Float.MAX_VALUE
            for (e in enemies) {
                if (e.hp <= 0f) continue
                val d = dist2(e.x, e.y, x, y)
                if (d < bestD) {
                    bestD = d
                    best = e
                }
            }
            return best
        }
        val k = (skip + 1).coerceAtMost(8)
        val found = arrayOfNulls<Enemy>(k)
        val dist = FloatArray(k) { Float.MAX_VALUE }
        for (e in enemies) {
            if (e.hp <= 0f) continue
            val d = dist2(e.x, e.y, x, y)
            if (d >= dist[k - 1]) continue
            var pos = k - 1
            while (pos > 0 && d < dist[pos - 1]) {
                found[pos] = found[pos - 1]
                dist[pos] = dist[pos - 1]
                pos--
            }
            found[pos] = e
            dist[pos] = d
        }
        return found.getOrNull(skip) ?: found.firstOrNull { it != null }
    }

    private fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return dx * dx + dy * dy
    }

    private fun hypot(x: Float, y: Float) = sqrt(x * x + y * y)

    companion object {
        const val RUN_SECONDS = 480f

        fun xpNeed(lv: Int): Int = 8 + lv * 5 + (lv * lv) / 2
    }
}
