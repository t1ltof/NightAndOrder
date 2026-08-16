package com.nightandorder.game

import java.time.LocalDate

enum class Biome { GRAVE, CHAPEL }

enum class NightEvent { FOG, BLOOD_MOON, PROCESSION, SWARM }

enum class Relic {
    FIRST_DAWN, SABBATH, PALE_DEATH, CHAPEL, FOG, DAILY;

    val title: String
        get() = when (this) {
            FIRST_DAWN -> "Первая заря"
            SABBATH -> "След шабаша"
            PALE_DEATH -> "Смерть при полной луне"
            CHAPEL -> "Камень часовни"
            FOG -> "Дыхание тумана"
            DAILY -> "Свидетель дня"
        }

    val blurb: String
        get() = when (this) {
            FIRST_DAWN -> "Дожили до рассвета. Всем героям чуть больше здоровья."
            SABBATH -> "Забег в пятницу после заката. Ночь бьёт злее, орден слышит дальше."
            PALE_DEATH -> "Упали, когда луна была круглая. Магнит и опыт."
            CHAPEL -> "Три минуты на плитах часовни. Вспышки шире."
            FOG -> "Прошли сквозь туман. Чуть больше брони."
            DAILY -> "Сходили в ночь дня. Чуть больше урона."
        }

    val hint: String
        get() = when (this) {
            FIRST_DAWN -> "Дожить до рассвета хоть раз."
            SABBATH -> "Закончить забег в пятницу после заката."
            PALE_DEATH -> "Упасть, когда луна полная."
            CHAPEL -> "Продержаться три минуты в часовне."
            FOG -> "Пережить туман на поле."
            DAILY -> "Провести в ночи дня хотя бы две минуты."
        }
}

object Night {
    fun biomeFor(daily: Boolean, faction: Faction, date: LocalDate = LocalDate.now()): Biome {
        return if (daily) {
            if (date.toEpochDay() % 2L == 0L) Biome.CHAPEL else Biome.GRAVE
        } else {
            if (faction == Faction.HOLY) Biome.CHAPEL else Biome.GRAVE
        }
    }

    fun today(): String = LocalDate.now().toString()

    fun dateLabel(date: LocalDate = LocalDate.now()): String {
        val months = arrayOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря",
        )
        return "${date.dayOfMonth} ${months[date.monthValue - 1]}"
    }
}
