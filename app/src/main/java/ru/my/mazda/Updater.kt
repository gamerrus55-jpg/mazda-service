package ru.my.mazda

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Проверка обновлений: смотрит номер последней сборки на GitHub. */
object Updater {
    private const val REPO = "gamerrus55-jpg/mazda-service"
    const val DOWNLOAD_URL = "https://github.com/$REPO/releases/latest/download/MazdaService.apk"

    val currentBuild: Int get() = BuildConfig.VERSION_CODE

    /** Номер последней сборки или null, если нет интернета. */
    suspend fun latestBuild(): Int? = withContext(Dispatchers.IO) {
        try {
            val conn = URL("https://api.github.com/repos/$REPO/releases/latest")
                .openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            try {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                JSONObject(body).optString("tag_name").removePrefix("build-").toIntOrNull()
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            null
        }
    }
}
