package ru.my.mazda

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {

    private lateinit var store: Store

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        store = Store(this)
        if (store.remind) Reminders.schedule(this)
        val openOdo = intent?.getBooleanExtra(CarWidget.EXTRA_ODO, false) ?: false
        setContent { App(store, openOdo) }
    }
}
