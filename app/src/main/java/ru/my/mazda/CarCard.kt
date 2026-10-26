package ru.my.mazda

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Поле карточки машины. hint — где это найти. */
data class CardField(val id: Long, val label: String, val value: String, val hint: String)

object CarCard {
    private var n = 0L
    private fun f(label: String, value: String = "", hint: String = "") = CardField(++n, label, value, hint)

    /** Что уже известно — заполнено, остальное с подсказкой, где посмотреть. */
    val defaults: List<CardField> = listOf(
        f("VIN", hint = "В СТС или на табличке под лобовым стеклом слева"),
        f("Госномер"),
        f("Код краски", hint = "На заводской табличке в моторном отсеке. Красные Mazda — обычно 46V (Soul Red Crystal) " +
            "или 41V (Soul Red). Это многослойные краски, поэтому карандаш «по коду» часто не попадает в цвет — " +
            "надёжнее подбор колеровщиком по месту."),
        f("Шины лето", hint = "Размер — на боковине шины. Штатный — на табличке в проёме водительской двери"),
        f("Шины зима", hint = "Размер, марка, шипы"),
        f("Давление в шинах", hint = "Табличка в проёме водительской двери: перед / зад"),
        f("Колёсные гайки", hint = "Размер ключа, где ключ от секретки"),
        f("Лампы", hint = "Ближний, дальний, ПТФ, габариты — тип есть в мануале или на цоколе старой лампы"),
        f("Предохранители", hint = "Где блоки и какой за что отвечает — раздел «Предохранители» в мануале"),
        f("Аккумулятор", hint = "Тип и размер — на наклейке АКБ. С i-stop обычно EFB Q-85"),
        f("Масло ДВС", "0W-20 · ≈ 4 л · фильтр PE01-14-302A"),
        f("Масло АКПП", "Mazda ATF FZ"),
        f("Антифриз", "Mazda FL-22 (зелёный)"),
        f("Тормозная жидкость", "DOT-4"),
        f("Свечи", "PE5R-18-110 (NGK ILKAR7L11)"),
        f("Воздушный фильтр", "PE07-13-3A0A"),
        f("Салонный фильтр", "KD45-61-J6X"),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CarScreen(store: Store) {
    store.tick.intValue
    val fields = store.card()
    var editing by remember { mutableStateOf<CardField?>(null) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    Text("Карточка машины", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(
                        "Mazda Axela (BM) · ${store.buildDate.year} · 1.5 SkyActiv-G · АКПП",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Image(
                        painterResource(R.drawable.pic_car), null,
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(2.6f)
                    )
                    Text(
                        "Нажми — изменить, удержи — скопировать",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        items(fields, key = { it.id }) { f ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .combinedClickable(
                        onClick = { editing = f },
                        onLongClick = {
                            if (f.value.isNotBlank()) {
                                clipboard.setText(AnnotatedString(f.value))
                                Toast.makeText(context, "Скопировано", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(f.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (f.value.isNotBlank()) {
                    Text(f.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("не заполнено — нажми", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            SoftButton(
                "+ Добавить поле",
                onClick = { editing = CardField(System.currentTimeMillis(), "", "", "") },
                modifier = Modifier.fillMaxWidth(),
                height = 48.dp
            )
        }
    }

    editing?.let { f ->
        FieldDialog(
            field = f,
            isNew = fields.none { it.id == f.id },
            onSave = { store.putField(it); editing = null },
            onDelete = { store.deleteField(f.id); editing = null },
            onClose = { editing = null }
        )
    }
}

@Composable
private fun FieldDialog(
    field: CardField,
    isNew: Boolean,
    onSave: (CardField) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var label by remember { mutableStateOf(field.label) }
    var value by remember { mutableStateOf(field.value) }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (isNew) "Новое поле" else field.label, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (field.hint.isNotBlank()) {
                    Text(field.hint, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Название") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it },
                    label = { Text("Значение") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isNew) {
                    TextButton(onClick = onDelete) {
                        Text("Удалить поле", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = label.isNotBlank(),
                onClick = { onSave(field.copy(label = label.trim(), value = value.trim())) }
            ) { Text("Сохранить", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } }
    )
}
