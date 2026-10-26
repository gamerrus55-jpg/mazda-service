package ru.my.mazda

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** Запись в журнале: что сделали, когда, на каком пробеге и за сколько. */
data class Record(
    val id: Long,
    /** id из регламента или пусто для своей работы. */
    val serviceId: String,
    val title: String,
    val date: LocalDate,
    val km: Int,
    val price: Int?,
    val note: String,
)

private const val PURCHASE_ID_BASE = 1_000_000L

/** Всё хранится на телефоне. */
class Store(context: Context) {

    private val prefs = context.getSharedPreferences("car", Context.MODE_PRIVATE)

    /** Меняется при любой правке — экраны по нему перерисовываются. */
    val tick = mutableIntStateOf(0)
    private fun changed() { tick.intValue++ }

    // ---------- Машина и пробег ----------

    val isSetUp: Boolean get() = prefs.getBoolean("setup", false)

    fun finishSetup() { prefs.edit().putBoolean("setup", true).apply(); changed() }

    val odometer: Int get() = prefs.getInt("odo", 0)

    /** Год и месяц выпуска — от них считается первая замена антифриза. */
    var buildDate: LocalDate
        get() = LocalDate.ofEpochDay(prefs.getLong("built", LocalDate.of(2018, 1, 1).toEpochDay()))
        set(v) { prefs.edit().putLong("built", v.withDayOfMonth(1).toEpochDay()).apply(); changed() }

    /** Тяжёлые условия: морозы, короткие поездки, пробки, пыль. */
    var severe: Boolean
        get() = prefs.getBoolean("severe", true)
        set(v) { prefs.edit().putBoolean("severe", v).apply(); changed() }

    var remind: Boolean
        get() = prefs.getBoolean("remind", true)
        set(v) { prefs.edit().putBoolean("remind", v).apply(); changed() }

    /** За сколько км и дней предупреждать. */
    var soonKm: Int
        get() = prefs.getInt("soonKm", 1500)
        set(v) { prefs.edit().putInt("soonKm", v).apply(); changed() }

    var soonDays: Int
        get() = prefs.getInt("soonDays", 30)
        set(v) { prefs.edit().putInt("soonDays", v).apply(); changed() }

    var lastNotifyDay: Long
        get() = prefs.getLong("notifyDay", 0)
        set(v) { prefs.edit().putLong("notifyDay", v).apply() }

    /** История показаний одометра: [день, км]. Нужна, чтобы считать средний пробег. */
    private fun odoLog(): MutableList<Pair<Long, Int>> {
        val arr = JSONArray(prefs.getString("odoLog", "[]"))
        val out = mutableListOf<Pair<Long, Int>>()
        for (i in 0 until arr.length()) {
            val a = arr.getJSONArray(i)
            out += a.getLong(0) to a.getInt(1)
        }
        return out
    }

    fun setOdometer(km: Int, date: LocalDate = LocalDate.now()) {
        val log = odoLog()
        val day = date.toEpochDay()
        log.removeAll { it.first == day }
        log += day to km
        log.sortBy { it.first }
        while (log.size > 120) log.removeAt(0)
        val arr = JSONArray()
        log.forEach { arr.put(JSONArray().put(it.first).put(it.second)) }
        // Сегодняшнее показание — просто ставим (можно исправить ошибку), старое — только если больше
        val newOdo = if (date == LocalDate.now()) km else maxOf(odometer, km)
        prefs.edit()
            .putString("odoLog", arr.toString())
            .putInt("odo", newOdo)
            .putLong("odoDay", LocalDate.now().toEpochDay())
            .apply()
        changed()
    }

    /** Когда последний раз обновляли пробег. */
    val odometerDay: LocalDate get() = LocalDate.ofEpochDay(prefs.getLong("odoDay", LocalDate.now().toEpochDay()))

    /** Средний пробег в день по последним полугоду, или null, если данных мало. */
    fun kmPerDay(): Double? {
        val today = LocalDate.now().toEpochDay()
        val log = odoLog().filter { it.first >= today - 200 }
        if (log.size < 2) return null
        val first = log.first()
        val last = log.last()
        val days = last.first - first.first
        val km = last.second - first.second
        if (days < 7 || km <= 0) return null
        return km.toDouble() / days
    }

    // ---------- Покупка машины ----------

    /** Указаны ли пробег и дата покупки. От них считается всё, что не отмечено в журнале. */
    val purchaseSet: Boolean get() = prefs.getBoolean("purchaseSet", false)

    val purchaseKm: Int get() = prefs.getInt("purchaseKm", 61_000)

    val purchaseDate: LocalDate
        get() = LocalDate.ofEpochDay(prefs.getLong("purchaseDate", LocalDate.now().toEpochDay()))

    /** id записей «сделал после покупки»: постоянные, чтобы повторное сохранение не плодило дубли. */
    fun purchaseRecordId(serviceId: String): Long =
        PURCHASE_ID_BASE + Regulation.all.indexOfFirst { it.id == serviceId }

    fun isPurchaseRecord(r: Record): Boolean = r.id in PURCHASE_ID_BASE until PURCHASE_ID_BASE + 100

    fun savePurchase(km: Int, date: LocalDate, done: Set<String>) {
        prefs.edit()
            .putBoolean("purchaseSet", true)
            .putInt("purchaseKm", km)
            .putLong("purchaseDate", date.toEpochDay())
            .apply()
        val kept = records().filter { !isPurchaseRecord(it) }
        val added = Regulation.all.filter { it.id in done }.map { s ->
            Record(
                id = purchaseRecordId(s.id),
                serviceId = s.id,
                title = s.title,
                date = date,
                km = km,
                price = null,
                note = "после покупки",
            )
        }
        save(kept + added)
        if (km > odometer) setOdometer(km)
    }

    // ---------- Журнал ----------

    fun records(): List<Record> {
        val arr = JSONArray(prefs.getString("records", "[]"))
        val out = mutableListOf<Record>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Record(
                id = o.getLong("id"),
                serviceId = o.optString("s"),
                title = o.optString("t"),
                date = LocalDate.ofEpochDay(o.getLong("d")),
                km = o.getInt("km"),
                price = if (o.has("p")) o.getInt("p") else null,
                note = o.optString("n"),
            )
        }
        return out.sortedWith(compareByDescending<Record> { it.date }.thenByDescending { it.km })
    }

    private fun save(list: List<Record>) {
        val arr = JSONArray()
        list.forEach { r ->
            val o = JSONObject()
                .put("id", r.id)
                .put("s", r.serviceId)
                .put("t", r.title)
                .put("d", r.date.toEpochDay())
                .put("km", r.km)
                .put("n", r.note)
            if (r.price != null) o.put("p", r.price)
            arr.put(o)
        }
        prefs.edit().putString("records", arr.toString()).apply()
        changed()
    }

    fun addRecord(r: Record) {
        save(records().filter { it.id != r.id } + r)
        // Запись свежее текущего пробега — значит, пробег вырос
        if (r.km > odometer) setOdometer(r.km, r.date)
    }

    fun deleteRecord(id: Long) = save(records().filter { it.id != id })

    fun lastRecord(serviceId: String): Record? =
        records().filter { it.serviceId == serviceId }.maxWithOrNull(compareBy<Record> { it.date }.thenBy { it.km })

    fun history(serviceId: String): List<Record> = records().filter { it.serviceId == serviceId }
}
