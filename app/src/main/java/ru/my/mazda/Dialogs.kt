package ru.my.mazda

import android.app.DatePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.LocalDate

/** Добавить или поправить запись в журнале. */
@Composable
fun RecordDialog(store: Store, draft: Draft, onClose: () -> Unit) {
    val context = LocalContext.current
    val edit = draft.edit
    var serviceId by remember { mutableStateOf(edit?.serviceId ?: draft.serviceId) }
    var title by remember { mutableStateOf(edit?.title ?: draft.title) }
    var day by remember { mutableStateOf(edit?.date ?: LocalDate.now()) }
    var kmText by remember { mutableStateOf((edit?.km ?: store.odometer).toString()) }
    var priceText by remember { mutableStateOf(edit?.price?.toString() ?: "") }
    var note by remember { mutableStateOf(edit?.note ?: "") }
    var confirmDelete by remember { mutableStateOf(false) }

    val kmVal = kmText.toIntOrNull()
    val fixedService = edit == null && draft.serviceId.isNotEmpty()
    val service = Regulation.byId(serviceId)
    val canSave = kmVal != null && (service != null || title.isNotBlank())

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                when {
                    edit != null -> "Запись"
                    service != null && fixedService -> service.title
                    else -> "Новая запись"
                },
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (!fixedService) {
                    Text("Что сделали", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(Regulation.all) { s ->
                            Pill(s.title, { serviceId = s.id }, selected = serviceId == s.id)
                        }
                        item {
                            Pill("Другое", { serviceId = "" }, selected = serviceId.isEmpty())
                        }
                    }
                    if (serviceId.isEmpty()) {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Название работы") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                } else if (service != null) {
                    Text(
                        "${service.action}. Отметь дату и пробег, когда это делали.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Дата — через системный календарь
                Box {
                    OutlinedTextField(
                        value = date(day),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Дата") },
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
                OutlinedTextField(
                    value = kmText,
                    onValueChange = { kmText = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("Пробег, км") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it.filter { c -> c.isDigit() }.take(8) },
                    label = { Text("Цена, ₽ (необязательно)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Заметка: что залили, где делали") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (edit != null) {
                    TextButton(onClick = { confirmDelete = true }) {
                        Text("Удалить запись", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    store.addRecord(
                        Record(
                            id = edit?.id ?: System.currentTimeMillis(),
                            serviceId = serviceId,
                            title = if (service != null) service.title else title.trim(),
                            date = day,
                            km = kmVal ?: 0,
                            price = priceText.toIntOrNull(),
                            note = note.trim(),
                        )
                    )
                    onClose()
                }
            ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Отмена") }
        }
    )

    if (confirmDelete && edit != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Удалить запись?") },
            text = { Text("${edit.title.ifBlank { "Запись" }} от ${date(edit.date)} пропадёт из журнала.") },
            confirmButton = {
                TextButton(onClick = {
                    store.deleteRecord(edit.id)
                    confirmDelete = false
                    onClose()
                }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Отмена") }
            }
        )
    }
}

/** Обновить пробег. */
@Composable
fun OdometerDialog(store: Store, onClose: () -> Unit) {
    var text by remember { mutableStateOf(store.odometer.toString()) }
    val value = text.toIntOrNull()
    val tooLow = value != null && value < store.odometer

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("Пробег сейчас", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter { c -> c.isDigit() }.take(7) },
                    label = { Text("Показания одометра, км") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (tooLow) {
                    Text(
                        "Меньше прежнего (${km(store.odometer)}) — сохраню как исправление.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Amber
                    )
                } else {
                    Text(
                        "Обновляй раз в неделю-две — так напоминания по километрам будут точнее.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = value != null, onClick = {
                store.setOdometer(value ?: 0)
                onClose()
            }) { Text("Сохранить", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            TextButton(onClick = onClose) { Text("Отмена") }
        }
    )
}
