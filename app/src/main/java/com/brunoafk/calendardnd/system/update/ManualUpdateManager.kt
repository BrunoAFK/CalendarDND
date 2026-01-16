package com.brunoafk.calendardnd.system.update

import android.content.Context
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object ManualUpdateManager {

    data class ReleaseInfo(
        val versionName: String,
        val versionCode: Int?,
        val apkUrl: String,
        val releaseNotes: String?
    )

    data class UpdateMetadata(
        val releases: List<ReleaseInfo>
    )

    enum class UpdateType {
        MAJOR,
        MINOR,
        PATCH,
        NONE
    }

    data class UpdatePrompt(
        val info: ReleaseInfo,
        val updateType: UpdateType
    )

    suspend fun checkForUpdate(context: Context): UpdatePrompt? {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            return null
        }

        val metadata = fetchUpdateMetadata() ?: return null
        val latest = metadata.releases.firstOrNull() ?: return null
        val currentVersion = parseSemver(BuildConfig.VERSION_NAME) ?: return null
        val latestVersion = parseSemver(latest.versionName) ?: return null

        val updateType = determineUpdateType(currentVersion, latestVersion)
        if (updateType == UpdateType.NONE || updateType == UpdateType.PATCH) {
            return null
        }

        val settingsStore = SettingsStore(context)
        var showInApp = false
        if (updateType == UpdateType.MINOR || updateType == UpdateType.MAJOR) {
            val lastInAppVersion = settingsStore.getLastInAppUpdateVersion()
            if (latest.versionName != lastInAppVersion) {
                showInApp = true
                settingsStore.setLastInAppUpdateVersion(latest.versionName)
            }
        }

        return if (showInApp) UpdatePrompt(latest, updateType) else null
    }

    suspend fun fetchUpdateMetadata(): UpdateMetadata? {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            return null
        }
        val urls = BuildConfig.MANUAL_UPDATE_URLS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (urls.isEmpty()) {
            return null
        }
        return fetchUpdateMetadata(urls)
    }

    private suspend fun fetchUpdateMetadata(urls: List<String>): UpdateMetadata? {
        return withContext(Dispatchers.IO) {
            for (url in urls) {
                val parsed = runCatching { fetchUpdateMetadata(URL(url)) }.getOrNull()
                if (parsed != null) {
                    return@withContext parsed
                }
            }
            null
        }
    }

    private fun fetchUpdateMetadata(url: URL): UpdateMetadata? {
        val connection = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
        }

        return try {
            if (connection.responseCode !in 200..299) {
                return null
            }
            val body = BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
                reader.readText()
            }
            parseUpdateMetadata(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parseUpdateMetadata(json: String): UpdateMetadata? {
        val obj = JSONObject(json)
        val releases = mutableListOf<ReleaseInfo>()
        val releaseArray = obj.optJSONArray("releases")
        if (releaseArray != null) {
            for (i in 0 until releaseArray.length()) {
                val entry = releaseArray.optJSONObject(i) ?: continue
                val parsed = parseRelease(entry) ?: continue
                releases.add(parsed)
            }
        }

        if (releases.isEmpty()) {
            val fallback = parseRelease(obj) ?: return null
            releases.add(fallback)
        }

        return UpdateMetadata(releases = releases)
    }

    private fun parseRelease(obj: JSONObject): ReleaseInfo? {
        val versionName = obj.optString("versionName").trim()
        val apkUrl = obj.optString("apkUrl").trim()
        if (versionName.isEmpty() || apkUrl.isEmpty()) {
            return null
        }

        val versionCode = if (obj.has("versionCode")) obj.optInt("versionCode") else null
        val releaseNotes = obj.optString("releaseNotes").trim().ifEmpty { null }

        return ReleaseInfo(
            versionName = versionName,
            versionCode = versionCode,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes
        )
    }

    private data class Semver(val major: Int, val minor: Int, val patch: Int)

    private fun parseSemver(versionName: String): Semver? {
        val clean = versionName.substringBefore("-").trim()
        val parts = clean.split(".")
        if (parts.size < 3) {
            return null
        }
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts[2].toIntOrNull() ?: return null
        return Semver(major, minor, patch)
    }

    private fun determineUpdateType(current: Semver, latest: Semver): UpdateType {
        return when {
            latest.major > current.major -> UpdateType.MAJOR
            latest.major == current.major && latest.minor > current.minor -> UpdateType.MINOR
            latest.major == current.major &&
                latest.minor == current.minor &&
                latest.patch > current.patch -> UpdateType.PATCH
            else -> UpdateType.NONE
        }
    }
}
