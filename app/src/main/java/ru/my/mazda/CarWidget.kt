package ru.my.mazda

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews

/** Виджет на рабочем столе: пробег, два ближайших ТО и подсказка по резине. */
class CarWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = build(context)
        ids.forEach { manager.updateAppWidget(it, views) }
    }

    companion object {
        const val EXTRA_ODO = "odo"

        fun updateAll(context: Context) {
            try {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, CarWidget::class.java))
                if (ids.isEmpty()) return
                val views = build(context)
                ids.forEach { manager.updateAppWidget(it, views) }
            } catch (e: Exception) {
                // Виджет не главное — не роняем приложение
            }
        }

        private fun open(context: Context, odo: Boolean): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                .putExtra(EXTRA_ODO, odo)
            return PendingIntent.getActivity(
                context, if (odo) 2 else 1, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        private fun dot(level: Level): String = when (level) {
            Level.OVERDUE -> "🔴"
            Level.SOON -> "🟡"
            Level.UNKNOWN -> "⚪"
            Level.OK -> "🟢"
        }

        fun build(context: Context): RemoteViews {
            val store = Store(context)
            val v = RemoteViews(context.packageName, R.layout.widget)
            v.setOnClickPendingIntent(R.id.root, open(context, false))
            v.setOnClickPendingIntent(R.id.odo_btn, open(context, true))

            if (!store.isSetUp) {
                v.setTextViewText(R.id.odo, "Открой приложение")
                v.setViewVisibility(R.id.row1, View.GONE)
                v.setViewVisibility(R.id.row2, View.GONE)
                v.setViewVisibility(R.id.tires, View.GONE)
                return v
            }

            v.setTextViewText(R.id.odo, km(store.odometer))

            val due = Status.all(store)
            val rows = listOf(
                Triple(R.id.row1, R.id.pic1, R.id.text1),
                Triple(R.id.row2, R.id.pic2, R.id.text2),
            )
            rows.forEachIndexed { i, (row, pic, text) ->
                val d = due.getOrNull(i)
                if (d == null) {
                    v.setViewVisibility(row, View.GONE)
                } else {
                    v.setViewVisibility(row, View.VISIBLE)
                    v.setImageViewResource(pic, d.service.pics.first())
                    v.setTextViewText(text, "${dot(d.level)} ${d.service.title}\n${d.summary}")
                }
            }

            val advice = Weather.advice(Weather.cached(store), store.tiresWinter)
            if (advice != null && advice.level == TireAdvice.CHANGE_NOW) {
                v.setViewVisibility(R.id.tires, View.VISIBLE)
                v.setTextViewText(R.id.tires, "🛞 " + advice.title)
            } else {
                v.setViewVisibility(R.id.tires, View.GONE)
            }
            return v
        }
    }
}
