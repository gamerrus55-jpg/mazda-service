package ru.my.mazda

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Tab(val emoji: String, val title: String) {
    PLAN("🔧", "Регламент"),
    LOG("📒", "Журнал"),
    WINTER("❄️", "Зима"),
    CAR("🚗", "Машина"),
    SETTINGS("⚙️", "Настройки"),
}

sealed interface Screen {
    data class Main(val tab: Tab) : Screen
    data class Detail(val serviceId: String) : Screen
}

/** Черновик записи: для пункта регламента или своей работы. */
data class Draft(val serviceId: String, val title: String, val edit: Record? = null)

@Composable
fun App(store: Store, openOdo: Boolean = false) {
    AppTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            val context = LocalContext.current
            // Подписка на изменения в хранилище
            store.tick.intValue

            var screen by remember { mutableStateOf<Screen>(Screen.Main(Tab.PLAN)) }
            var draft by remember { mutableStateOf<Draft?>(null) }
            var odoDialog by remember { mutableStateOf(openOdo && store.isSetUp) }
            var latestBuild by remember { mutableStateOf<Int?>(null) }
            var purchaseEdit by remember { mutableStateOf(false) }

            val notifPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }

            LaunchedEffect(Unit) {
                latestBuild = Updater.latestBuild()
                if (Build.VERSION.SDK_INT >= 33 && store.isSetUp && store.remind && !Reminders.canNotify(context)) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
            ) {
                if (!store.isSetUp) {
                    SetupScreen(store) {
                        store.finishSetup()
                        Reminders.schedule(context)
                        if (Build.VERSION.SDK_INT >= 33) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else if (!store.purchaseSet || purchaseEdit) {
                    if (purchaseEdit) BackHandler { purchaseEdit = false }
                    PurchaseScreen(store) {
                        purchaseEdit = false
                        screen = Screen.Main(Tab.PLAN)
                    }
                } else when (val s = screen) {
                    is Screen.Main -> Column(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).padding(horizontal = 16.dp)) {
                            when (s.tab) {
                                Tab.PLAN -> HomeScreen(
                                    store = store,
                                    latestBuild = latestBuild,
                                    onOdometer = { odoDialog = true },
                                    onOpen = { screen = Screen.Detail(it) },
                                )
                                Tab.LOG -> JournalScreen(
                                    store = store,
                                    onAdd = { draft = Draft("", "") },
                                    onEdit = { r -> draft = Draft(r.serviceId, r.title, r) },
                                )
                                Tab.WINTER -> WinterScreen(store)
                                Tab.CAR -> CarScreen(store)
                                Tab.SETTINGS -> SettingsScreen(store, latestBuild, onPurchase = { purchaseEdit = true })
                            }
                        }
                        BottomBar(s.tab) { screen = Screen.Main(it) }
                    }

                    is Screen.Detail -> {
                        val back = { screen = Screen.Main(Tab.PLAN) }
                        BackHandler(onBack = back)
                        val service = Regulation.byId(s.serviceId)
                        if (service == null) back() else Box(Modifier.padding(horizontal = 16.dp)) {
                            DetailScreen(
                                service = service,
                                store = store,
                                onBack = back,
                                onDone = { draft = Draft(service.id, service.title) },
                                onEdit = { r -> draft = Draft(r.serviceId, r.title, r) },
                            )
                        }
                    }
                }

                draft?.let { d ->
                    RecordDialog(store, d, onClose = { draft = null })
                }
                if (odoDialog) {
                    OdometerDialog(store, onClose = { odoDialog = false })
                }
            }
        }
    }
}

@Composable
private fun BottomBar(current: Tab, onTab: (Tab) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Tab.entries.forEach { t ->
            val sel = t == current
            Column(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (sel) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .clickable { onTab(t) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(t.emoji, fontSize = 20.sp)
                Text(
                    t.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                    color = if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
