package ru.my.mazda

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** Раз в день проверяет регламент и присылает уведомление, если что-то пора делать. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val ctx = applicationContext
        Reminders.check(ctx, force = false)
        // Погода: подсказка, когда переобуваться
        val store = Store(ctx)
        if (store.isSetUp) {
            val days = Weather.refresh(store, force = true)
            Reminders.tires(ctx, store, Weather.advice(days, store.tiresWinter))
        }
        CarWidget.updateAll(ctx)
        return Result.success()
    }
}

object Reminders {
    private const val CHANNEL = "service"
    private const val WORK = "daily-check"
    private const val NOTIFY_ID = 1

    /** Уведомление про резину — один раз на каждый новый совет. */
    @SuppressLint("MissingPermission")
    fun tires(context: Context, store: Store, advice: Advice?) {
        if (advice == null || advice.level != TireAdvice.CHANGE_NOW || !store.remind) return
        val key = (if (store.tiresWinter) "summer-" else "winter-") + store.winterSeason()
        if (store.lastTireNotice == key || !canNotify(context)) return
        channel(context)
        val open = PendingIntent.getActivity(
            context, 3,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFFD71920.toInt())
            .setContentTitle("🛞 " + advice.title)
            .setContentText(advice.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(advice.text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(2, n)
            store.lastTireNotice = key
        } catch (e: SecurityException) {
        }
    }

    fun schedule(context: Context) {
        val req = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(1, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.KEEP, req)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun channel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "Напоминания о ТО", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    /**
     * Проверка и уведомление. force — показать сразу (кнопка «Проверить сейчас»),
     * иначе не чаще раза в 3 дня.
     * Возвращает текст уведомления или null, если всё в порядке.
     */
    @SuppressLint("MissingPermission")
    fun check(context: Context, force: Boolean): String? {
        val store = Store(context)
        if (!store.isSetUp || (!store.remind && !force)) return null
        val today = LocalDate.now()
        if (!force && today.toEpochDay() - store.lastNotifyDay < 3) return null

        val due = Status.all(store)
        val overdue = due.filter { it.level == Level.OVERDUE }
        val soon = due.filter { it.level == Level.SOON }
        val staleDays = ChronoUnit.DAYS.between(store.odometerDay, today)

        val title: String
        val text: String
        when {
            overdue.isNotEmpty() -> {
                title = "Пора на ТО: " + overdue.joinToString(", ") { it.service.title.lowercase() }
                text = (overdue + soon).joinToString("\n") { "• ${it.service.title}: ${it.summary}" }
            }
            soon.isNotEmpty() -> {
                title = "Скоро ТО: " + soon.joinToString(", ") { it.service.title.lowercase() }
                text = soon.joinToString("\n") { "• ${it.service.title}: ${it.summary}" }
            }
            staleDays >= 14 -> {
                title = "Обнови пробег"
                text = "Пробег не обновлялся ${period(staleDays)} — без него напоминания по километрам неточные."
            }
            else -> return null
        }

        if (!canNotify(context)) return title
        channel(context)
        val open = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFFD71920.toInt())
            .setContentTitle(title)
            .setContentText(text.lineSequence().first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, n)
            store.lastNotifyDay = today.toEpochDay()
        } catch (e: SecurityException) {
            // Разрешение отозвали — молча пропускаем
        }
        return title
    }
}
