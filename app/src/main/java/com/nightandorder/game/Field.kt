package com.nightandorder.game

enum class PropKind { ROCK, STONE, TREE, TREE_WIDE }

data class Prop(
    val kind: PropKind,
    val x: Float,
    val y: Float,
    val radius: Float,
    val drawW: Float,
    val drawH: Float,
)

object Field {
    const val CELL = 200f

    fun forEachNear(px: Float, py: Float, range: Float, block: (Prop) -> Unit) {
        val minCx = kotlin.math.floor((px - range) / CELL).toInt()
        val maxCx = kotlin.math.floor((px + range) / CELL).toInt()
        val minCy = kotlin.math.floor((py - range) / CELL).toInt()
        val maxCy = kotlin.math.floor((py + range) / CELL).toInt()
        for (cy in minCy..maxCy) {
            for (cx in minCx..maxCx) {
                val p = at(cx, cy) ?: continue
                block(p)
            }
        }
    }

    fun pushOut(x: Float, y: Float, radius: Float): Pair<Float, Float> {
        var nx = x
        var ny = y
        forEachNear(x, y, 80f) { p ->
            val dx = nx - p.x
            val dy = ny - p.y
            val min = radius + p.radius
            val d2 = dx * dx + dy * dy
            if (d2 < min * min && d2 > 0.0001f) {
                val d = kotlin.math.sqrt(d2)
                val push = (min - d) + 0.5f
                nx += dx / d * push
                ny += dy / d * push
            } else if (d2 <= 0.0001f) {
                nx += min
            }
        }
        return nx to ny
    }

    fun blocked(x: Float, y: Float, radius: Float): Boolean {
        var hit = false
        forEachNear(x, y, 80f) { p ->
            val dx = x - p.x
            val dy = y - p.y
            val min = radius + p.radius
            if (dx * dx + dy * dy < min * min) hit = true
        }
        return hit
    }

    private fun at(cx: Int, cy: Int): Prop? {
        if (cx == 0 && cy == 0) return null
        val h = hash(cx, cy)
        if (h % 3 != 0) return null
        val kind = when ((h ushr 8) % 4) {
            0 -> PropKind.ROCK
            1 -> PropKind.STONE
            2 -> PropKind.TREE
            else -> PropKind.TREE_WIDE
        }
        val ox = ((h ushr 3) % 70) - 35
        val oy = ((h ushr 11) % 70) - 35
        val x = cx * CELL + ox
        val y = cy * CELL + oy
        return when (kind) {
            PropKind.ROCK -> Prop(kind, x, y, 22f, 52f, 42f)
            PropKind.STONE -> Prop(kind, x, y, 16f, 34f, 38f)
            PropKind.TREE -> Prop(kind, x, y, 18f, 56f, 78f)
            PropKind.TREE_WIDE -> Prop(kind, x, y, 20f, 64f, 82f)
        }
    }

    private fun hash(cx: Int, cy: Int): Int {
        var h = cx * 374761393 + cy * 668265263
        h = h xor (h ushr 13)
        h *= -1640531535
        return h and Int.MAX_VALUE
    }
}
