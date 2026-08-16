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
)

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
    var life: Float = 40f,
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
}

enum class RunEnd { DEAD, DAWN }

class World(val character: CharacterDef) {
    val player = Player(character)
    val enemies = ArrayList<Enemy>(256)
    val projectiles = ArrayList<Projectile>(160)
    val pickups = ArrayList<Pickup>(256)
    val particles = ArrayList<Particle>(200)
    val weapons = ArrayList<WeaponInst>()
    val passives = Passives()
    val rng = Random(System.nanoTime())

    var time = 0f
    var kills = 0
    var level = 1
    var xp = 0
    var xpToNext = 6
    var brightness = 0.55f
    var spawnAcc = 0f
    var eliteAcc = 0f
    var end: RunEnd? = null
    var pendingOffers: List<Offer>? = null
    var inputX = 0f
    var inputY = 0f

    val power get() = BrightnessPower.of(character.faction, brightness)

    init {
        weapons += WeaponInst(character.signature, 1)
    }

    fun damageMul() = power.damage * (1f + passives.lv(PassiveId.DAMAGE) * 0.18f)
    fun cooldownMul() = (1f / power.ability) * (1f - passives.lv(PassiveId.COOLDOWN) * 0.08f).coerceAtLeast(0.45f)
    fun areaMul() = (0.85f + power.ability * 0.25f) * (1f + passives.lv(PassiveId.AREA) * 0.12f)
    fun extraShots() = passives.lv(PassiveId.PROJECTILES)
    fun magnet() = 48f + passives.lv(PassiveId.MAGNET) * 28f
    fun armor() = passives.lv(PassiveId.ARMOR) * 0.08f
    fun moveSpeed() = character.speed * (1f + passives.lv(PassiveId.SPEED) * 0.08f)

    fun tick(dt: Float) {
        if (end != null || pendingOffers != null) return
        time += dt
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
        player.invuln = (player.invuln - dt).coerceAtLeast(0f)

        tickWeapons(dt)
        tickProjectiles(dt)
        spawn(dt)
        tickEnemies(dt)
        collide()
        tickPickups(dt)
        tickParticles(dt)
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
                            shoot(p.x, p.y, a, 260f, (9f + w.level * 2.4f) * dmg, 0.7f, 6f * area, 1, false)
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
                            shoot(p.x, p.y, a, 180f, (6f + w.level * 1.6f) * dmg, 0.85f, 7f * area, 0, false)
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
                            shoot(p.x, p.y, a, 220f, (8f + w.level * 2.1f) * dmg, 0.9f, 7f * area, 1, true)
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
                            shoot(p.x, p.y, a, 300f, (10f + w.level * 2.6f) * dmg, 0.8f, 6f * area, 2, true)
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
                            shoot(p.x, p.y, a, 200f, (7f + w.level * 1.8f) * dmg, 0.75f, 6.5f * area, 0, true)
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
                                seek = true,
                                seekTurn = 7.5f,
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
            }
        }
        val orbits = projectiles.filter { it.orbit }
        val vampireOrbits = orbits.filter { !it.holy }
        vampireOrbits.forEachIndexed { i, pr ->
            val n = vampireOrbits.size.coerceAtLeast(1)
            pr.orbitAngle += pr.orbitSpeed * dt
            val base = i * (Math.PI.toFloat() * 2f / n)
            val a = base + pr.orbitAngle
            pr.x = p.x + cos(a) * pr.orbitRadius
            pr.y = p.y + sin(a) * pr.orbitRadius
        }
    }

    private fun shoot(
        x: Float, y: Float, a: Float, speed: Float,
        damage: Float, life: Float, radius: Float, pierce: Int, holy: Boolean,
    ) {
        projectiles += Projectile(
            x, y, cos(a) * speed, sin(a) * speed,
            life, radius, damage, pierce, holy,
        )
    }

    private fun nova(x: Float, y: Float, r: Float, dmg: Float, holy: Boolean) {
        for (e in enemies) {
            if (dist2(e.x, e.y, x, y) <= r * r) hurtEnemy(e, dmg)
        }
        burst(x, y, if (holy) 0xFFE8D48A.toInt() else 0xFF8B1E2D.toInt(), 18, r * 0.6f)
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
            for (e in enemies) {
                if (e.hp <= 0f) continue
                val rr = pr.radius + e.radius
                if (dist2(pr.x, pr.y, e.x, e.y) <= rr * rr) {
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
            time > 360f -> 170
            time > 240f -> 140
            time > 120f -> 110
            else -> 80
        }
        val rate = 1.6f + time / 70f
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
        if (time.toInt() in listOf(180, 360) && enemies.none { it.kind == EnemyKind.BOSS }) {
            val (x, y) = spawnPoint()
            enemies += makeEnemy(EnemyKind.BOSS, x, y)
        }
    }

    private fun rollKind(): EnemyKind {
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
        return player.x + cos(a) * d to player.y + sin(a) * d
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
            e.x += dx / m * e.speed * dt
            e.y += dy / m * e.speed * dt
            e.touchCd = (e.touchCd - dt).coerceAtLeast(0f)
        }
        // light separation
        val n = min(enemies.size, 90)
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
                burst(player.x, player.y, 0xFFCC3344.toInt(), 10, 40f)
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
        e.hp -= amount
        if (e.hp <= 0f) {
            e.hp = 0f
            kills += 1
            pickups += Pickup(e.x, e.y, e.xp)
            burst(e.x, e.y, 0xFFD0C8B0.toInt(), 8, 30f)
        }
    }

    private fun tickPickups(dt: Float) {
        val mag = magnet()
        val it = pickups.iterator()
        while (it.hasNext()) {
            val g = it.next()
            g.life -= dt
            val d = hypot(player.x - g.x, player.y - g.y)
            if (d < mag) {
                val pull = 220f * dt
                g.x += (player.x - g.x) / d.coerceAtLeast(1f) * pull
                g.y += (player.y - g.y) / d.coerceAtLeast(1f) * pull
            }
            if (d < 18f) {
                addXp(g.value)
                it.remove()
            } else if (g.life <= 0f) {
                it.remove()
            }
        }
        enemies.removeAll { it.hp <= 0f }
    }

    private fun addXp(v: Int) {
        xp += v
        while (xp >= xpToNext && pendingOffers == null) {
            xp -= xpToNext
            level += 1
            xpToNext = (6 + level * 4 + (level * level) / 6).coerceAtMost(80)
            pendingOffers = rollOffers()
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
                pool += Offer.PassiveUp(def.id, lv + 1, def.name, "${def.blurb} (${lv + 1})")
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
        }
        pendingOffers = null
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
        if (particles.size > 160) particles.subList(0, particles.size - 160).clear()
    }

    private fun burst(x: Float, y: Float, color: Int, n: Int, speed: Float) {
        repeat(n) {
            val a = rng.nextFloat() * Math.PI.toFloat() * 2f
            val s = speed * (0.3f + rng.nextFloat())
            particles += Particle(x, y, cos(a) * s, sin(a) * s, 0.35f, 0.35f, color, 3f + rng.nextFloat() * 3f)
        }
    }

    private fun nearest(x: Float, y: Float, skip: Int): Enemy? {
        if (enemies.isEmpty()) return null
        val sorted = enemies.filter { it.hp > 0f }.sortedBy { dist2(it.x, it.y, x, y) }
        return sorted.getOrNull(skip.coerceAtMost(sorted.lastIndex))
    }

    private fun dist2(ax: Float, ay: Float, bx: Float, by: Float): Float {
        val dx = ax - bx
        val dy = ay - by
        return dx * dx + dy * dy
    }

    private fun hypot(x: Float, y: Float) = sqrt(x * x + y * y)

    companion object {
        const val RUN_SECONDS = 480f
    }
}
