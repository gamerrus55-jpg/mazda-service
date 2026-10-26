package ru.my.mazda

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate

/** Прогноз на день. */
data class Day(val date: LocalDate, val min: Double, val max: Double, val snow: Double) {
    val mean: Double get() = (min + max) / 2
}

enum class TireAdvice(val color: Color) {
    CHANGE_NOW(Red),
    SOON(Amber),
    OK(Green),
}

data class Advice(val level: TireAdvice, val title: String, val text: String)

/**
 * Погода для Омска — бесплатный сервис Open-Meteo, без ключа.
 * Правило шиномонтажников: переобуваться, когда средняя суточная держится около +5 °C и ниже
 * или обещают снег и ночные заморозки. Весной — когда стабильно выше +5…+7 и без ночных минусов.
 */
object Weather {
    // Омск
    private const val LAT = 54.99
    private const val LON = 73.37
    private const val URL_STR = "https://api.open-meteo.com/v1/forecast?latitude=$LAT&longitude=$LON" +
        "&daily=temperature_2m_max,temperature_2m_min,snowfall_sum&forecast_days=10&timezone=Asia%2FOmsk"

    /** Свежий прогноз; null — нет интернета. */
    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        try {
            val conn = URL(URL_STR).openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun parse(json: String?): List<Day> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val daily = JSONObject(json).getJSONObject("daily")
            val time = daily.getJSONArray("time")
            val tmax = daily.getJSONArray("temperature_2m_max")
            val tmin = daily.getJSONArray("temperature_2m_min")
            val snow = daily.optJSONArray("snowfall_sum")
            (0 until time.length()).map { i ->
                Day(
                    date = LocalDate.parse(time.getString(i)),
                    min = tmin.optDouble(i, 0.0),
                    max = tmax.optDouble(i, 0.0),
                    snow = snow?.optDouble(i, 0.0) ?: 0.0,
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Обновить кэш, если он старше 3 часов. */
    suspend fun refresh(store: Store, force: Boolean = false): List<Day> {
        val age = System.currentTimeMillis() - store.weatherTime
        if (force || age > 3 * 60 * 60 * 1000L || store.weatherJson.isBlank()) {
            fetch()?.let { store.saveWeather(it) }
        }
        return cached(store)
    }

    fun cached(store: Store): List<Day> {
        val today = LocalDate.now()
        return parse(store.weatherJson).filter { !it.date.isBefore(today) }
    }

    fun advice(days: List<Day>, winterOn: Boolean): Advice? {
        val week = days.take(7)
        if (week.size < 3) return null
        val cold = week.count { it.mean <= 5.0 }
        val frost = week.any { it.min <= -2.0 }
        val snow = week.firstOrNull { it.snow > 0.0 }
        val minDay = week.minBy { it.min }

        return if (!winterOn) {
            when {
                snow != null || cold >= 4 || (frost && cold >= 2) -> Advice(
                    TireAdvice.CHANGE_NOW,
                    "Пора на зимнюю резину",
                    when {
                        snow != null -> "${dayName(snow.date)} обещают снег. Летняя резина на снегу и льду — как на коньках."
                        frost -> "Ночью до ${t(minDay.min)}, днём холодно. Летняя резина дубеет уже ниже +5 °C."
                        else -> "Средняя температура держится ниже +5 °C — летняя резина теряет сцепление."
                    }
                )
                cold >= 1 || week.any { it.min <= 0.0 } -> Advice(
                    TireAdvice.SOON,
                    "Скоро переобуваться",
                    "Впереди холодные дни и ночи около нуля. Самое время записаться на шиномонтаж, пока нет очередей."
                )
                else -> Advice(TireAdvice.OK, "Летняя резина ещё в порядке", "Неделю держится тепло — можно не спешить.")
            }
        } else {
            val month = LocalDate.now().monthValue
            val warm = week.count { it.mean >= 7.0 }
            val noFrost = week.all { it.min > -1.0 }
            when {
                month in 3..6 && warm >= 5 && noFrost -> Advice(
                    TireAdvice.CHANGE_NOW,
                    "Можно на летнюю",
                    "Неделю тепло и без ночных заморозков. Зимняя резина на тёплом асфальте быстро стирается и хуже тормозит."
                )
                month in 3..6 && warm >= 3 -> Advice(
                    TireAdvice.SOON,
                    "Скоро на летнюю",
                    "Теплеет, но ночью ещё бывают минуса. Подожди, пока перестанут обещать заморозки."
                )
                else -> Advice(TireAdvice.OK, "Зимняя резина — то что надо", "Холодно, переобуваться рано.")
            }
        }
    }

    private val days = listOf("пн", "вт", "ср", "чт", "пт", "сб", "вс")

    fun weekday(d: LocalDate): String = days[d.dayOfWeek.value - 1]

    fun dayName(d: LocalDate): String {
        val today = LocalDate.now()
        return when (d) {
            today -> "Сегодня"
            today.plusDays(1) -> "Завтра"
            else -> "В ${weekday(d)} ${d.dayOfMonth}.${d.monthValue.toString().padStart(2, '0')}"
        }
    }

    fun t(v: Double): String {
        val r = Math.round(v).toInt()
        return (if (r > 0) "+$r" else "$r") + "°"
    }
}

/** Пункт зимнего чек-листа. */
data class WinterItem(val id: String, val emoji: String, val title: String, val hint: String)

object WinterChecklist {
    val items = listOf(
        WinterItem("battery", "🔋", "Аккумулятор: зарядить и проверить",
            "В мороз ёмкость падает почти вдвое. Нагрузочный тест, чистые клеммы. Старше 4–5 лет — присмотри замену."),
        WinterItem("tires", "🛞", "Зимняя резина и давление",
            "На холоде давление падает: примерно 0,1 бар на каждые 10 °C. Проверяй на холодных колёсах."),
        WinterItem("washer", "💧", "Незамерзайка −30 °C",
            "Слей летнюю воду из бачка и прокачай форсунки, чтобы не замёрзли."),
        WinterItem("wipers", "🧹", "Зимние щётки",
            "Летние каркасные щётки забиваются льдом. Зимние или бескаркасные чистят лучше."),
        WinterItem("coolant", "🌡️", "Антифриз: уровень и защита",
            "Проверь уровень на холодном двигателе и защиту от мороза — нужно не хуже −40 °C."),
        WinterItem("oil", "🛢️", "Масло 0W-20 / 5W-30",
            "0W-20 легче всего крутится в мороз. Если замена скоро — сделай до холодов."),
        WinterItem("cabin", "🌬️", "Салонный фильтр",
            "Забитый фильтр — запотевшие стёкла и слабая печка."),
        WinterItem("brakes", "🛑", "Тормоза и тормозная жидкость",
            "Старая жидкость набирает влагу — зимой это опасно. Проверь колодки."),
        WinterItem("heater", "🔥", "Подогрев или автозапуск",
            "Ниже −25…−30 °C пуск без подогрева тяжёлый и для двигателя, и для АКБ."),
        WinterItem("seals", "🚪", "Уплотнители — силиконом",
            "Чтобы двери и багажник не примерзали."),
        WinterItem("locks", "🔑", "Размораживатель замков — в кармане",
            "Не в бардачке: в машину без него не попадёшь."),
        WinterItem("kit", "🧰", "Щётка, лопата, трос, провода",
            "Провода «прикурить» и трос в Омске зимой точно пригодятся — себе или соседу."),
        WinterItem("fuel", "⛽", "Держать бак полным",
            "Меньше конденсата в баке и запас, если встанешь в пробке в мороз."),
        WinterItem("lights", "💡", "Фары и все лампы",
            "Темнеет рано. Протри и проверь фары, габариты, стопы, ПТФ."),
        WinterItem("underbody", "🧽", "Мойка днища и антикор",
            "Реагенты съедают металл. В оттепель мой днище и арки."),
    )
}
