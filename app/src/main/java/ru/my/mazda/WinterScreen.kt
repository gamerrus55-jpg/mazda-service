package ru.my.mazda

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun WinterScreen(store: Store) {
    store.tick.intValue
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var confirmSwap by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        loading = true
        Weather.refresh(store)
        loading = false
    }

    val days = Weather.cached(store)
    val advice = Weather.advice(days, store.tiresWinter)
    val checked = store.checked()
    val season = store.winterSeason()
    val lastSwap = store.lastSwap()

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text("Зима ${season}/${(season + 1) % 100}", style = MaterialTheme.typography.headlineMedium)
        }

        // Совет по резине
        item {
            val c = advice?.level?.color ?: Gray
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (advice?.level == TireAdvice.CHANGE_NOW) HeroBrush else
                        androidx.compose.ui.graphics.Brush.linearGradient(listOf(c.copy(alpha = 0.16f), c.copy(alpha = 0.08f))))
                    .padding(18.dp)
            ) {
                val onRed = advice?.level == TireAdvice.CHANGE_NOW
                val fg = if (onRed) Color.White else MaterialTheme.colorScheme.onSurface
                Column {
                    Text("🛞 Резина", color = fg.copy(alpha = 0.8f), style = MaterialTheme.typography.labelLarge)
                    Text(
                        when {
                            advice != null -> advice.title
                            loading -> "Загружаю прогноз…"
                            else -> "Нет прогноза — проверь интернет"
                        },
                        color = fg,
                        style = MaterialTheme.typography.titleLarge
                    )
                    if (advice != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(advice.text, color = fg, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Сейчас стоит: " + (if (store.tiresWinter) "зимняя" else "летняя") +
                            (lastSwap?.let { " · переобувка ${date(it.date)}" } ?: ""),
                        color = fg.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (onRed) Color.White else MaterialTheme.colorScheme.primary)
                            .clickable { confirmSwap = true }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        Text(
                            if (store.tiresWinter) "Я переобулся на летнюю" else "Я переобулся на зимнюю",
                            color = if (onRed) RedDark else Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Прогноз
        if (days.isNotEmpty()) {
            item {
                Text("Омск, 10 дней", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(days) { d -> DayChip(d) }
                }
            }
            item {
                Text(
                    "Правило: средняя за сутки около +5 °C и ниже или снег — пора на зимнюю. " +
                        "Весной — когда неделю выше +5…+7 и без ночных минусов.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Чек-лист
        item {
            SoftCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Подготовка к зиме", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${checked.size}/${WinterChecklist.items.size}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(8.dp))
                Bar(checked.size.toFloat() / WinterChecklist.items.size, Red, Modifier.fillMaxWidth())
                if (checked.size == WinterChecklist.items.size) {
                    Spacer(Modifier.height(8.dp))
                    Text("Машина готова к зиме 💪", fontWeight = FontWeight.Bold, color = Green)
                }
            }
        }

        items(WinterChecklist.items, key = { it.id }) { w ->
            val on = w.id in checked
            SoftCard(onClick = { store.toggleCheck(w.id) }, padding = 12.dp) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(w.emoji, fontSize = 24.sp, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            w.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (on) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                        )
                        Text(w.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Checkbox(checked = on, onCheckedChange = { store.toggleCheck(w.id) })
                }
            }
        }

        item {
            TextButton(onClick = { store.resetChecklist() }) {
                Text("Сбросить отметки", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    if (confirmSwap) {
        val toWinter = !store.tiresWinter
        AlertDialog(
            onDismissRequest = { confirmSwap = false },
            title = { Text(if (toWinter) "Переобулся на зимнюю?" else "Переобулся на летнюю?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Запишу в журнал на пробеге ${km(store.odometer)} — заодно это перестановка колёс по регламенту. Если пробег устарел, сначала обнови его.")
            },
            confirmButton = {
                TextButton(onClick = {
                    store.swapTires(toWinter)
                    confirmSwap = false
                    scope.launch { Weather.refresh(store) }
                }) { Text("Да, записать", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmSwap = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun DayChip(d: Day) {
    val icon = when {
        d.snow > 0.0 -> "❄️"
        d.max <= 0.0 -> "🥶"
        d.min <= 0.0 -> "🌨️"
        d.max >= 15.0 -> "☀️"
        else -> "🌥️"
    }
    val cold = d.mean <= 5.0
    Column(
        Modifier
            .width(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, if (cold) Red.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(Weather.weekday(d.date), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${d.date.dayOfMonth}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(icon, fontSize = 22.sp, modifier = Modifier.padding(vertical = 4.dp))
        Text(Weather.t(d.max), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(Weather.t(d.min), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
