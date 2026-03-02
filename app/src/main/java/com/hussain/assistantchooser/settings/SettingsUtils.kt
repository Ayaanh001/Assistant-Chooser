package com.hussain.assistantchooser.settings

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

fun performHapticFeedback(context: Context) {
    val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator ?: return
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION") v.vibrate(50)
    }
}

fun isNewerVersion(latest: String, current: String): Boolean {
    val l = latest.split(".").mapNotNull { it.toIntOrNull() }
    val c = current.split(".").mapNotNull { it.toIntOrNull() }
    for (i in 0 until maxOf(l.size, c.size)) {
        val diff = (l.getOrElse(i) { 0 }) - (c.getOrElse(i) { 0 })
        if (diff != 0) return diff > 0
    }
    return false
}

suspend fun checkLatestVersionFromGitHub(owner: String, repo: String): String? = runCatching {
    val url  = URL("https://api.github.com/repos/$owner/$repo/releases/latest")
    val conn = (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "GET"
        setRequestProperty("Accept", "application/vnd.github+json")
        connectTimeout = 10_000
        readTimeout    = 10_000
    }
    try {
        if (conn.responseCode in 200..299)
            JSONObject(conn.inputStream.bufferedReader().readText()).optString("tag_name", null)
        else null
    } finally { conn.disconnect() }
}.getOrNull()

