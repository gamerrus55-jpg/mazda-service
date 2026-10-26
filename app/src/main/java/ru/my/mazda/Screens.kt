package ru.my.mazda

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

// ---------- Первый запуск ----------

@Composable
fun SetupScreen(store: Store, onDone: () -> Unit) {
    var odo by remember { mutableStateOf("") }
    var year by remember { mutableStateOf("2018") }
    var severe by remember { mutableStateOf(true) }
    val odoKm = odo.filter { it.isDigit() }.toIntOrNull()
    val y = year.toIntOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(HeroBrush)
                .padding(20.dp)
        ) {
            Column {
                Text("Моя Mazda", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                Text("Axela · 1.5 SkyActiv · автомат", color = Color.White.copy(alpha = 0.8f))
                Spacer(Modifier.height(10.dp))
                Image(
                    painterResource(R.drawable.pic_car), null,
                    Modifier.fillMaxWidth().aspectRatio(2f)
                )
            }
        }
        Text("Пара вопросов — и регламент посчитается под твою машину.", style = MaterialTheme.typography.titleMedium)

        OutlinedTextField(
            value = odo,
            onValueChange = { odo = it.filter { c -> c.isDigit() }.take(7) },
            label = { Text("Пробег сейчас, км") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = year,
            onValueChange = { year = it.filter { c -> c.isDigit() }.take(4) },
            label = { Text("Год выпуска") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Text("Условия эксплуатации", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill("Тяжёлые", { severe = true }, selected = severe)
            Pill("Обычные", { severe = false }, selected = !severe)
        }
        Text(
            "Тяжёлые — морозы, короткие поездки, пробки, пыль. Для Сибири это обычная жизнь: " +
                "масло и фильтры будут меняться чаще. Можно поменять потом в настройках.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        RedButton(
            "Готово",
            onClick = {
                store.severe = severe
                store.buildDate = LocalDate.of(y ?: 2018, 1, 1)
                store.setOdometer(odoKm ?: 0)
                onDone()
            },
            enabled = odoKm != null && y != null && y in 1990..2030,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            "Дальше открой каждый пункт и отметь, когда его делали последний раз — от этого считаются сроки.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ---------- Главная: регламент ----------

@Composable
fun HomeScreen(store: Store, latestBuild: Int?, onOdometer: () -> Unit, onOpen: (String) -> Unit) {
    store.tick.intValue
    val due = Status.all(store)
    val uri = LocalUriHandler.current
    val overdue = due.count { it.level == Level.OVERDUE }
    val soon = due.count { it.level == Level.SOON }
    val unknown = due.count { it.level == Level.UNKNOWN }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(HeroBrush)
                    .padding(18.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Mazda Axela", color = Color.White, style = MaterialTheme.typography.titleLarge)
                            Text(
                                "1.5 SkyActiv · автомат · " + store.buildDate.year,
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Text(
                            if (store.severe) "тяжёлые условия" else "обычные условия",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.18f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    Image(
                        painterResource(R.drawable.pic_car), null,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.2f)
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Text("Пробег", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.labelMedium)
                            Text(km(store.odometer), color = Color.White, style = MaterialTheme.typography.displaySmall)
                            Text(
                                "обновлён " + date(store.odometerDay) +
                                    (store.kmPerDay()?.let { " · ≈ ${km((it * 30).toInt())} в месяц" } ?: ""),
                                color = Color.White.copy(alpha = 0.75f),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White)
                                .clickable(onClick = onOdometer)
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text("Обновить", color = RedDark, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (latestBuild != null && latestBuild > Updater.currentBuild) {
            item {
                SoftCard(onClick = { uri.openUri(Updater.DOWNLOAD_URL) }) {
                    Text("Есть новая версия приложения", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Нажми, чтобы скачать и поставить поверх — журнал сохранится.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                if (overdue > 0) Tag("Пора: $overdue", Red)
                if (soon > 0) Tag("Скоро: $soon", Amber)
                if (unknown > 0) Tag("Без отметки: $unknown", Gray)
                if (overdue == 0 && soon == 0 && unknown == 0) Tag("Всё в норме", Green)
            }
        }

        items(due, key = { it.service.id }) { d ->
            ServiceCard(d) { onOpen(d.service.id) }
        }

        item {
            Text(
                "Интервалы — по сервисной книжке Mazda. Артикулы перед покупкой сверяй по VIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun PicTile(res: Int, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(PicBg),
        contentAlignment = Alignment.Center
    ) {
        Image(painterResource(res), null, Modifier.size(size * 0.82f))
    }
}

@Composable
private fun ServiceCard(d: Due, onClick: () -> Unit) {
    SoftCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PicTile(d.service.pics.first(), 64.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        d.service.title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(6.dp))
                    Tag(d.level.label, d.level.color)
                }
                Text(
                    d.service.action + " · " + interval(d.intervalKm, d.intervalMonths),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                if (d.level != Level.UNKNOWN) {
                    Bar(d.progress, d.level.color, Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    d.summary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (d.level == Level.OK) MaterialTheme.colorScheme.onSurface else d.level.color
                )
            }
        }
    }
}

// ---------- Подробно об узле ----------

@Composable
fun DetailScreen(
    service: Service,
    store: Store,
    onBack: () -> Unit,
    onDone: () -> Unit,
    onEdit: (Record) -> Unit,
) {
    store.tick.intValue
    val d = Status.calc(service, store)
    val history = store.history(service.id)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircleButton("←", onBack)
            Spacer(Modifier.width(12.dp))
            Text(service.title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
        }

        // Картинка узла
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(PicBg)
                .padding(18.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Image(painterResource(service.pics.first()), service.title, Modifier.size(170.dp))
                service.pics.drop(1).forEach {
                    Image(painterResource(it), null, Modifier.size(110.dp))
                }
            }
        }

        // Состояние
        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Tag(d.level.label, d.level.color)
                Spacer(Modifier.width(8.dp))
                if (!service.official) Tag("рекомендация", Gray)
            }
            Spacer(Modifier.height(10.dp))
            if (d.level == Level.UNKNOWN) {
                Text(
                    "Нет отметки о последней замене. Укажи дату и пробег — и приложение посчитает, когда снова.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Не знаешь, когда делали? Надёжнее сделать сейчас и отметить.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Bar(d.progress, d.level.color, Modifier.fillMaxWidth(), height = 10.dp)
                Spacer(Modifier.height(10.dp))
                Text(d.summary, fontWeight = FontWeight.Bold, color = d.level.color)
                Spacer(Modifier.height(6.dp))
                val last = d.last
                InfoLine(
                    "Последний раз",
                    when {
                        last != null -> "${date(last.date)} · ${km(last.km)}"
                        d.fromPurchase -> "неизвестно — считаем с покупки (${km(store.purchaseKm)}, ${date(store.purchaseDate)})"
                        else -> "новая машина, ${store.buildDate.year}"
                    }
                )
                val next = listOfNotNull(d.dueKm?.let { "на ${km(it)}" }, d.dueDate?.let { "до ${date(it)}" })
                    .joinToString(" или ")
                if (next.isNotEmpty()) InfoLine("Следующий раз", next)
                if (d.etaDays != null) InfoLine("По твоему пробегу", "≈ через ${period(d.etaDays)}")
            }
            if (d.fromPurchase && service.official) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "До покупки история неизвестна. Если сделаешь — отметь, и отсчёт пойдёт от этой даты.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            RedButton("Отметить: ${service.action.lowercase()}", onDone, Modifier.fillMaxWidth())
        }

        // Регламент
        SoftCard {
            Text("Регламент", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            RegLine("Обычные условия", interval(service.km, service.months), !store.severe)
            RegLine("Тяжёлые условия", interval(service.kmHard, service.monthsHard), store.severe)
            if (service.firstKm != null || service.firstMonths != null) {
                RegLine("Первая замена", interval(service.firstKm, service.firstMonths).replace("каждые", "через"), false)
            }
            Spacer(Modifier.height(6.dp))
            Text(service.what, style = MaterialTheme.typography.bodyMedium)
        }

        // Жидкости и запчасти
        if (service.fluid != null || service.volume != null || service.parts.isNotEmpty()) {
            SoftCard {
                Text("Что купить", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                service.fluid?.let { InfoLine("Жидкость", it) }
                service.volume?.let { InfoLine("Объём", it) }
                service.parts.forEach { p ->
                    val isNumber = p.number.any { it.isDigit() }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(enabled = isNumber) {
                                clipboard.setText(AnnotatedString(p.number))
                                Toast.makeText(context, "Скопировано: ${p.number}", Toast.LENGTH_SHORT).show()
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(p.what, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text(
                            p.number,
                            fontWeight = FontWeight.Bold,
                            color = if (isNumber) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (isNumber) Text("  ⧉", color = MaterialTheme.colorScheme.primary)
                    }
                }
                if (service.parts.any { p -> p.number.any { it.isDigit() } }) {
                    Text(
                        "Нажми на артикул — скопируется. Перед покупкой сверь по VIN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (service.tips.isNotBlank()) {
            SoftCard {
                Text("💡 Совет", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(service.tips, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (history.isNotEmpty()) {
            Text("История", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp))
            history.forEach { r -> RecordCard(r) { onEdit(r) } }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun RegLine(label: String, value: String, active: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            label + if (active) " ✓" else "",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.width(150.dp)
        )
        Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

@Composable
fun RecordCard(r: Record, onClick: () -> Unit) {
    val service = Regulation.byId(r.serviceId)
    SoftCard(onClick = onClick, padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (service != null) {
                PicTile(service.pics.first(), 48.dp)
                Spacer(Modifier.width(12.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(service?.title ?: r.title.ifBlank { "Работа" }, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${date(r.date)} · ${km(r.km)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (r.note.isNotBlank()) Text(r.note, style = MaterialTheme.typography.bodySmall)
            }
            r.price?.let { Text(rub(it), fontWeight = FontWeight.Bold) }
        }
    }
}

// ---------- Журнал ----------

@Composable
fun JournalScreen(store: Store, onAdd: () -> Unit, onEdit: (Record) -> Unit) {
    store.tick.intValue
    val records = store.records()
    val total = records.sumOf { it.price ?: 0 }
    val year = LocalDate.now().year
    val totalYear = records.filter { it.date.year == year }.sumOf { it.price ?: 0 }

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text("Журнал", style = MaterialTheme.typography.headlineMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SoftCard(Modifier.weight(1f)) {
                    Text("За $year", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rub(totalYear), style = MaterialTheme.typography.titleLarge)
                }
                SoftCard(Modifier.weight(1f)) {
                    Text("Всего", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(rub(total), style = MaterialTheme.typography.titleLarge)
                }
            }
        }
        item {
            RedButton("+ Добавить запись", onAdd, Modifier.fillMaxWidth())
        }
        if (records.isEmpty()) {
            item {
                Text(
                    "Пока пусто. Отмечай здесь все замены и ремонты — с датой, пробегом и ценой.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
        items(records, key = { it.id }) { r -> RecordCard(r) { onEdit(r) } }
    }
}

// ---------- Настройки ----------

@Composable
fun SettingsScreen(store: Store, latestBuild: Int?, onPurchase: () -> Unit) {
    store.tick.intValue
    val context = LocalContext.current
    val uri = LocalUriHandler.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Настройки", style = MaterialTheme.typography.headlineMedium)

        SoftCard {
            Text("Условия эксплуатации", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pill("Тяжёлые", { store.severe = true }, selected = store.severe)
                Pill("Обычные", { store.severe = false }, selected = !store.severe)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Тяжёлые: морозы, короткие поездки до 10 км, пробки, долгий прогрев, пыльные дороги. " +
                    "Тогда масло, фильтры и тормозная жидкость меняются чаще.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SoftCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Напоминания", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(
                    checked = store.remind,
                    onCheckedChange = {
                        store.remind = it
                        if (it) Reminders.schedule(context) else Reminders.cancel(context)
                    }
                )
            }
            Text(
                "Раз в день приложение проверяет регламент и присылает уведомление, если что-то пора делать.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(10.dp))
            Text("Предупреждать за", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(500, 1000, 1500, 3000).forEach { v ->
                    Pill(num(v), { store.soonKm = v }, selected = store.soonKm == v)
                }
                Text("км", Modifier.align(Alignment.CenterVertically))
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(14, 30, 60).forEach { v ->
                    Pill("$v", { store.soonDays = v }, selected = store.soonDays == v)
                }
                Text("дней", Modifier.align(Alignment.CenterVertically))
            }
            Spacer(Modifier.height(10.dp))
            SoftButton(
                "Проверить сейчас",
                onClick = {
                    val r = Reminders.check(context, force = true)
                    val msg = when {
                        r == null -> "Всё в порядке, ничего не горит"
                        !Reminders.canNotify(context) -> "$r (уведомления выключены в настройках телефона)"
                        else -> r
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }

        SoftCard {
            Text("Покупка", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "Куплена на ${km(store.purchaseKm)}, ${date(store.purchaseDate)}. " +
                    "Всё, что не отмечено в журнале, считается от покупки.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            SoftButton("Изменить", onPurchase, Modifier.fillMaxWidth(), height = 48.dp)
        }

        SoftCard {
            Text("Машина", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Mazda Axela (BM) · 1.5 SkyActiv-G · АКПП · передний привод", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            val built = store.buildDate
            SoftButton(
                "Дата выпуска: ${built.monthValue.toString().padStart(2, '0')}.${built.year}",
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, _ -> store.buildDate = LocalDate.of(y, m + 1, 1) },
                        built.year, built.monthValue - 1, 1
                    ).show()
                },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
            Text(
                "От неё считается первая замена антифриза, если нет отметки в журнале.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        SoftCard {
            Text("Приложение", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("Версия: сборка ${Updater.currentBuild}", style = MaterialTheme.typography.bodyMedium)
            if (latestBuild != null && latestBuild > Updater.currentBuild) {
                Spacer(Modifier.height(8.dp))
                RedButton("Скачать сборку $latestBuild", { uri.openUri(Updater.DOWNLOAD_URL) }, Modifier.fillMaxWidth())
            } else if (latestBuild != null) {
                Text("Это последняя версия", style = MaterialTheme.typography.bodySmall, color = Green)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Интервалы взяты из сервисной книжки Mazda; масло в АКПП и щётки — рекомендации сверх регламента. " +
                    "Объёмы приблизительные, артикулы сверяй по VIN.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ---------- Покупка: от чего считать ----------

@Composable
fun PurchaseScreen(store: Store, onDone: () -> Unit) {
    val context = LocalContext.current
    val existing = store.records().filter { store.isPurchaseRecord(it) }.map { it.serviceId }.toSet()
    var kmText by remember { mutableStateOf(store.purchaseKm.toString()) }
    var day by remember { mutableStateOf(if (store.purchaseSet) store.purchaseDate else LocalDate.now()) }
    var done by remember {
        mutableStateOf(
            if (store.purchaseSet) existing
            else setOf("oil", "atf", "cabin_filter", "air_filter")
        )
    }
    val kmVal = kmText.toIntOrNull()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Покупка машины", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Всё, что не отмечено, приложение считает от пробега и даты покупки.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = kmText,
            onValueChange = { kmText = it.filter { c -> c.isDigit() }.take(7) },
            label = { Text("Пробег при покупке, км") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Box {
            OutlinedTextField(
                value = date(day),
                onValueChange = {},
                readOnly = true,
                label = { Text("Дата покупки") },
                modifier = Modifier.fillMaxWidth()
            )
            Box(
                Modifier
                    .matchParentSize()
                    .clickable {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> day = LocalDate.of(y, m + 1, d) },
                            day.year, day.monthValue - 1, day.dayOfMonth
                        ).show()
                    }
            )
        }

        Text("Что поменял после покупки", style = MaterialTheme.typography.titleMedium)
        Regulation.all.forEach { s ->
            val on = s.id in done
            SoftCard(
                onClick = { done = if (on) done - s.id else done + s.id },
                padding = 10.dp
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PicTile(s.pics.first(), 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(s.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Checkbox(checked = on, onCheckedChange = { done = if (it) done + s.id else done - s.id })
                }
            }
        }
        Text(
            "Отмеченные запишутся в журнал на пробеге покупки. Цену и точную дату можно поправить потом в журнале.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        RedButton(
            "Готово",
            onClick = {
                store.savePurchase(kmVal ?: 0, day, done)
                onDone()
            },
            enabled = kmVal != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
