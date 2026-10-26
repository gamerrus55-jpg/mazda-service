package ru.my.mazda

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

enum class Level(val label: String, val color: Color, val order: Int) {
    OVERDUE("Пора менять", Red, 0),
    SOON("Скоро", Amber, 1),
    UNKNOWN("Нет отметки", Gray, 2),
    OK("В норме", Green, 3),
}

data class Due(
    val service: Service,
    val level: Level,
    val last: Record?,
    /** От чего считаем: последняя отметка или новый авто. */
    val baseKm: Int?,
    val baseDate: LocalDate?,
    val intervalKm: Int?,
    val intervalMonths: Int?,
    val dueKm: Int?,
    val dueDate: LocalDate?,
    val leftKm: Int?,
    val leftDays: Long?,
    /** Когда наберётся пробег — по среднему пробегу в день. */
    val etaDays: Long?,
    val progress: Float,
    /** Истории нет — считаем от покупки машины. */
    val fromPurchase: Boolean = false,
) {
    /** Короткая строка для карточки. */
    val summary: String
        get() {
            if (level == Level.UNKNOWN) return "Укажи, когда делали последний раз"
            val parts = mutableListOf<String>()
            if (leftKm != null) {
                parts += if (leftKm <= 0) "просрочено на ${km(-leftKm)}" else "через ${km(leftKm)}"
            }
            if (leftDays != null) {
                parts += if (leftDays <= 0) "срок прошёл ${date(dueDate!!)}" else "до ${date(dueDate!!)}"
            } else if (etaDays != null && leftKm != null && leftKm > 0) {
                parts += "≈ ${period(etaDays)}"
            }
            return parts.joinToString(" · ")
        }
}

object Status {

    fun calc(s: Service, store: Store, today: LocalDate = LocalDate.now()): Due {
        val last = store.lastRecord(s.id)
        val odo = store.odometer
        val hard = store.severe

        val baseKm: Int
        val baseDate: LocalDate
        val km: Int?
        val months: Int?
        var fromPurchase = false
        if (last != null) {
            baseKm = last.km
            baseDate = last.date
            km = if (hard) s.kmHard else s.km
            months = if (hard) s.monthsHard else s.months
        } else if (s.firstKm != null || s.firstMonths != null) {
            baseKm = 0
            baseDate = store.buildDate
            km = s.firstKm
            months = s.firstMonths
        } else if (store.purchaseSet) {
            baseKm = store.purchaseKm
            baseDate = store.purchaseDate
            km = if (hard) s.kmHard else s.km
            months = if (hard) s.monthsHard else s.months
            fromPurchase = true
        } else {
            return Due(s, Level.UNKNOWN, null, null, null, if (hard) s.kmHard else s.km,
                if (hard) s.monthsHard else s.months, null, null, null, null, null, 0f)
        }

        val dueKm = km?.let { baseKm + it }
        val dueDate = months?.let { baseDate.plusMonths(it.toLong()) }
        val leftKm = dueKm?.let { it - odo }
        val leftDays = dueDate?.let { ChronoUnit.DAYS.between(today, it) }
        val perDay = store.kmPerDay()
        val eta = if (leftKm != null && leftKm > 0 && perDay != null) (leftKm / perDay).toLong() else null

        val pKm = if (km != null && km > 0) (odo - baseKm).toFloat() / km else 0f
        val pT = if (dueDate != null) {
            val total = ChronoUnit.DAYS.between(baseDate, dueDate).coerceAtLeast(1)
            ChronoUnit.DAYS.between(baseDate, today).toFloat() / total
        } else 0f
        val progress = max(pKm, pT).coerceIn(0f, 1f)

        val overdue = (leftKm != null && leftKm <= 0) || (leftDays != null && leftDays <= 0)
        val soon = (leftKm != null && leftKm <= store.soonKm) ||
            (leftDays != null && leftDays <= store.soonDays) ||
            (eta != null && eta <= store.soonDays)
        val level = when {
            overdue -> Level.OVERDUE
            soon -> Level.SOON
            else -> Level.OK
        }
        return Due(s, level, last, baseKm, baseDate, km, months, dueKm, dueDate, leftKm, leftDays, eta, progress, fromPurchase)
    }

    /** Все пункты: сначала то, что горит. */
    fun all(store: Store): List<Due> =
        Regulation.all.map { calc(it, store) }
            .sortedWith(compareBy<Due> { it.level.order }.thenByDescending { it.progress })
}

// ---------- Форматирование ----------

private val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun date(d: LocalDate): String = d.format(dateFmt)

fun num(n: Int): String {
    val s = abs(n).toString().reversed().chunked(3).joinToString(" ").reversed()
    return if (n < 0) "-$s" else s
}

fun km(n: Int): String = num(n) + " км"

fun rub(n: Int): String = num(n) + " ₽"

fun months(m: Int): String = when {
    m % 12 == 0 -> plural(m / 12, "год", "года", "лет")
    else -> plural(m, "месяц", "месяца", "месяцев")
}

fun period(days: Long): String = when {
    days < 14 -> plural(days.toInt(), "день", "дня", "дней")
    days < 60 -> plural((days / 7).toInt(), "неделя", "недели", "недель")
    days < 730 -> plural((days / 30).toInt(), "месяц", "месяца", "месяцев")
    else -> plural((days / 365).toInt(), "год", "года", "лет")
}

fun plural(n: Int, one: String, few: String, many: String): String {
    val m10 = n % 10
    val m100 = n % 100
    val w = when {
        m10 == 1 && m100 != 11 -> one
        m10 in 2..4 && m100 !in 12..14 -> few
        else -> many
    }
    return "$n $w"
}

/** «каждые 15 000 км или 1 год» */
fun interval(k: Int?, m: Int?): String = when {
    k != null && m != null -> "каждые ${km(k)} или ${months(m)}"
    k != null -> "каждые ${km(k)}"
    m != null -> "раз в ${months(m)}".replace("раз в 1 год", "раз в год")
    else -> "по состоянию"
}
