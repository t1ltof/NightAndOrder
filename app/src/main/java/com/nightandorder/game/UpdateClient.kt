package com.nightandorder.game

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class RemoteRelease(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val notes: String,
    val size: Long,
)

enum class UpdatePhase {
    IDLE,
    CHECKING,
    AVAILABLE,
    DOWNLOADING,
    READY,
    FAILED,
}

class UpdateClient(private val context: Context) {
    @Volatile var phase: UpdatePhase = UpdatePhase.CHECKING
        private set
    @Volatile var remote: RemoteRelease? = null
        private set
    @Volatile var error: String? = null
        private set
    @Volatile var progress: Float = 0f
        private set
    @Volatile var apkFile: File? = null
        private set

    private var pendingInstall: File? = null

    fun check() {
        phase = UpdatePhase.CHECKING
        error = null
        Thread({
            try {
                val body = httpGet(RELEASES_URL) ?: run {
                    phase = UpdatePhase.IDLE
                    return@Thread
                }
                val parsed = parse(body)
                if (parsed != null && isNewer(parsed)) {
                    remote = parsed
                    phase = UpdatePhase.AVAILABLE
                } else {
                    phase = UpdatePhase.IDLE
                }
            } catch (e: Exception) {
                error = e.message
                phase = UpdatePhase.IDLE
            }
        }, "update-check").start()
    }

    fun dismiss() {
        if (phase == UpdatePhase.AVAILABLE || phase == UpdatePhase.FAILED) {
            phase = UpdatePhase.IDLE
        }
    }

    fun download() {
        val rel = remote ?: return
        if (phase == UpdatePhase.DOWNLOADING) return
        phase = UpdatePhase.DOWNLOADING
        progress = 0f
        error = null
        Thread({
            try {
                val dir = File(context.cacheDir, "updates").apply { mkdirs() }
                val dest = File(dir, "NightAndOrder-${rel.versionName}.apk")
                downloadTo(rel.apkUrl, dest, rel.size)
                apkFile = dest
                progress = 1f
                phase = UpdatePhase.READY
            } catch (e: Exception) {
                error = e.message ?: "Не удалось скачать"
                phase = UpdatePhase.FAILED
            }
        }, "update-dl").start()
    }

    fun installOrRequestPermission(activity: Activity) {
        val file = apkFile ?: pendingInstall ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !activity.packageManager.canRequestPackageInstalls()
        ) {
            pendingInstall = file
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${activity.packageName}"),
            )
            activity.startActivity(intent)
            return
        }
        pendingInstall = null
        val uri = FileProvider.getUriForFile(
            activity,
            "${activity.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        activity.startActivity(intent)
    }

    fun retryPendingInstall(activity: Activity) {
        if (pendingInstall != null &&
            (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
                activity.packageManager.canRequestPackageInstalls())
        ) {
            installOrRequestPermission(activity)
        }
    }

    companion object {
        const val GITHUB_OWNER = "t1ltof"
        const val GITHUB_REPO = "NightAndOrder"
        private const val RELEASES_URL =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        fun isNewer(remote: RemoteRelease): Boolean {
            if (remote.versionCode > BuildConfig.VERSION_CODE) return true
            return compareSemver(remote.versionName, BuildConfig.VERSION_NAME) > 0
        }

        fun compareSemver(a: String, b: String): Int {
            val pa = parseSemver(a)
            val pb = parseSemver(b)
            for (i in 0 until 3) {
                val d = pa[i] - pb[i]
                if (d != 0) return d
            }
            return 0
        }

        private fun parseSemver(raw: String): IntArray {
            val clean = raw.trim().removePrefix("v").substringBefore("-")
            val parts = clean.split('.')
            return intArrayOf(
                parts.getOrNull(0)?.toIntOrNull() ?: 0,
                parts.getOrNull(1)?.toIntOrNull() ?: 0,
                parts.getOrNull(2)?.toIntOrNull() ?: 0,
            )
        }

        private fun parse(body: String): RemoteRelease? {
            val json = JSONObject(body)
            val tag = json.optString("tag_name")
            if (tag.isBlank()) return null
            val versionName = tag.removePrefix("v").trim()
            val notes = json.optString("body")
            var versionCode = 0
            val codeMatch = Regex("""versionCode\s*[:=]\s*(\d+)""").find(notes)
            if (codeMatch != null) {
                versionCode = codeMatch.groupValues[1].toInt()
            }
            val assets = json.optJSONArray("assets") ?: return null
            var apkUrl = ""
            var size = 0L
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                val name = a.optString("name")
                if (name.endsWith(".apk", ignoreCase = true)) {
                    apkUrl = a.optString("browser_download_url")
                    size = a.optLong("size")
                    break
                }
            }
            if (apkUrl.isBlank()) return null
            return RemoteRelease(tag, versionName, versionCode, apkUrl, notes, size)
        }

        private fun httpGet(url: String): String? {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("User-Agent", "NightAndOrder/${BuildConfig.VERSION_NAME}")
                setRequestProperty("Accept", "application/vnd.github+json")
                instanceFollowRedirects = true
            }
            return try {
                val code = conn.responseCode
                if (code == 404) return null
                if (code !in 200..299) throw IllegalStateException("HTTP $code")
                conn.inputStream.bufferedReader().use { it.readText() }
            } finally {
                conn.disconnect()
            }
        }

    }

    private fun downloadTo(url: String, dest: File, expected: Long) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 12000
            readTimeout = 30000
            setRequestProperty("User-Agent", "NightAndOrder/${BuildConfig.VERSION_NAME}")
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode !in 200..299) {
                throw IllegalStateException("HTTP ${conn.responseCode}")
            }
            val total = if (expected > 0) expected else conn.contentLengthLong
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(16 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        if (total > 0L) {
                            progress = (read.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                        }
                    }
                }
            }
        } finally {
            conn.disconnect()
        }
    }
}
