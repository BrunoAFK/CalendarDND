package com.brunoafk.calendardnd.system.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.brunoafk.calendardnd.BuildConfig
import com.brunoafk.calendardnd.data.prefs.DebugLogLevel
import com.brunoafk.calendardnd.data.prefs.DebugLogStore
import com.brunoafk.calendardnd.data.prefs.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object ManualUpdateManager {

    private const val TAG = "ManualUpdateManager"
    private const val DEFAULT_RELEASE_NOTES_URL =
        "https://github.com/BrunoAFK/CalendarDND/releases/latest/download/update.json"
    private val allowedUpdateHosts = setOf("github.com", "githubusercontent.com")

    data class ReleaseInfo(
        val versionName: String,
        val versionCode: Int?,
        val apkUrl: String,
        val releaseNotes: String?,
        val sha256: String?
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

    data class UpdateStatus(
        val info: ReleaseInfo,
        val updateType: UpdateType
    )

    data class UpdateEvaluation(
        val isNewer: Boolean,
        val updateType: UpdateType
    )

    data class UpdateCheckResult(
        val status: UpdateStatus?,
        val prompt: UpdatePrompt?
    )

    suspend fun checkForUpdates(context: Context): UpdateCheckResult {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            logUpdate(context, DebugLogLevel.INFO, "Manual updates disabled for this build.")
            return UpdateCheckResult(null, null)
        }
        if (!ensureSignaturePinned(context)) {
            logUpdate(context, DebugLogLevel.WARNING, "Signature pin mismatch; skipping update checks.")
            Log.w(TAG, "Signature pin mismatch; skipping update checks.")
            return UpdateCheckResult(null, null)
        }

        logUpdate(context, DebugLogLevel.INFO, "Checking for updates…")
        val metadata = fetchUpdateMetadata(context) ?: run {
            logUpdate(context, DebugLogLevel.WARNING, "No update metadata available.")
            return UpdateCheckResult(null, null)
        }
        val latest = metadata.releases.firstOrNull() ?: run {
            logUpdate(context, DebugLogLevel.WARNING, "Update metadata contained no releases.")
            return UpdateCheckResult(null, null)
        }
        val evaluation = evaluateUpdate(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE, latest)
            ?: run {
                logUpdate(context, DebugLogLevel.WARNING, "Failed to evaluate version ${latest.versionName}.")
                return UpdateCheckResult(null, null)
            }

        if (!evaluation.isNewer || evaluation.updateType == UpdateType.NONE || evaluation.updateType == UpdateType.PATCH) {
            logUpdate(
                context,
                DebugLogLevel.INFO,
                "No update. Latest=${latest.versionName} type=${evaluation.updateType} newer=${evaluation.isNewer}"
            )
            return UpdateCheckResult(null, null)
        }

        val settingsStore = SettingsStore(context)
        var prompt: UpdatePrompt? = null
        if (evaluation.updateType == UpdateType.MINOR || evaluation.updateType == UpdateType.MAJOR) {
            val lastInAppVersion = settingsStore.getLastInAppUpdateVersion()
            if (latest.versionName != lastInAppVersion) {
                settingsStore.setLastInAppUpdateVersion(latest.versionName)
                prompt = UpdatePrompt(latest, evaluation.updateType)
            }
        }

        return UpdateCheckResult(
            status = UpdateStatus(latest, evaluation.updateType),
            prompt = prompt
        )
    }

    suspend fun fetchUpdateMetadata(): UpdateMetadata? {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            return null
        }
        val urls = BuildConfig.MANUAL_UPDATE_URLS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { isAllowedUpdateUrl(it) }
        if (urls.isEmpty()) {
            return null
        }
        return fetchUpdateMetadata(urls, null)
    }

    suspend fun fetchUpdateMetadata(context: Context): UpdateMetadata? {
        if (!BuildConfig.MANUAL_UPDATE_ENABLED) {
            logUpdate(context, DebugLogLevel.INFO, "Manual updates disabled for this build.")
            return null
        }
        val urls = BuildConfig.MANUAL_UPDATE_URLS
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { isAllowedUpdateUrl(it) }
        if (urls.isEmpty()) {
            logUpdate(context, DebugLogLevel.WARNING, "No update URLs configured.")
            return null
        }
        logUpdate(context, DebugLogLevel.INFO, "Update URLs: ${urls.joinToString(", ")}")
        return fetchUpdateMetadata(urls, context)
    }

    suspend fun fetchReleaseNotesMetadata(
        urls: List<String> = listOf(DEFAULT_RELEASE_NOTES_URL)
    ): UpdateMetadata? {
        val filtered = urls
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { isAllowedUpdateUrl(it) }
        if (filtered.isEmpty()) {
            return null
        }
        return fetchUpdateMetadata(filtered, null)
    }

    private suspend fun fetchUpdateMetadata(
        urls: List<String>,
        context: Context?
    ): UpdateMetadata? {
        return withContext(Dispatchers.IO) {
            for (url in urls) {
                logUpdate(context, DebugLogLevel.INFO, "Fetching update metadata from $url")
                val parsed = runCatching { fetchUpdateMetadata(URL(url)) }.getOrNull()
                if (parsed != null) {
                    logUpdate(
                        context,
                        DebugLogLevel.INFO,
                        "Loaded update metadata (${parsed.releases.size} releases)."
                    )
                    return@withContext parsed
                }
                logUpdate(context, DebugLogLevel.WARNING, "Failed to load update metadata from $url")
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
        if (versionName.isEmpty() || apkUrl.isEmpty() || !isAllowedUpdateUrl(apkUrl)) {
            return null
        }

        val versionCode = if (obj.has("versionCode")) obj.optInt("versionCode") else null
        val releaseNotes = obj.optString("releaseNotes").trim().ifEmpty { null }
        val sha256 = obj.optString("sha256").trim().ifEmpty { null }

        return ReleaseInfo(
            versionName = versionName,
            versionCode = versionCode,
            apkUrl = apkUrl,
            releaseNotes = releaseNotes,
            sha256 = sha256
        )
    }

    private data class Semver(val major: Int, val minor: Int, val patch: Int)

    private fun parseSemver(versionName: String): Semver? {
        val clean = versionName.substringBefore("-").trim()
        val parts = clean.split(".")
        if (parts.size < 2) {
            return null
        }
        val major = parts[0].toIntOrNull() ?: return null
        val minor = parts[1].toIntOrNull() ?: return null
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
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

    fun evaluateUpdate(
        currentVersionName: String,
        currentVersionCode: Int,
        latest: ReleaseInfo
    ): UpdateEvaluation? {
        val currentVersion = parseSemver(currentVersionName) ?: return null
        val latestVersion = parseSemver(latest.versionName) ?: return null

        val versionCodeNewer = latest.versionCode?.let { it > currentVersionCode }
        val isNewer = versionCodeNewer ?: (compareSemver(latestVersion, currentVersion) > 0)
        val updateType = if (isNewer) {
            determineUpdateType(currentVersion, latestVersion)
        } else {
            UpdateType.NONE
        }

        return UpdateEvaluation(isNewer = isNewer, updateType = updateType)
    }

    fun isReleaseOlderThanCurrent(currentVersionName: String, release: ReleaseInfo): Boolean {
        val currentVersion = parseSemver(currentVersionName) ?: return false
        val releaseVersion = parseSemver(release.versionName) ?: return false
        return compareSemver(releaseVersion, currentVersion) < 0
    }

    suspend fun downloadAndVerifyApk(
        context: android.content.Context,
        info: ReleaseInfo
    ): java.io.File? {
        val expectedHash = info.sha256?.lowercase()?.trim().orEmpty()
        if (expectedHash.isBlank()) {
            logUpdate(context, DebugLogLevel.WARNING, "Missing SHA-256 for ${info.versionName}.")
            return null
        }
        if (!isAllowedUpdateUrl(info.apkUrl)) {
            logUpdate(context, DebugLogLevel.WARNING, "Blocked APK URL: ${info.apkUrl}")
            return null
        }

        return withContext(Dispatchers.IO) {
            val url = URL(info.apkUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 10000
                readTimeout = 20000
                requestMethod = "GET"
            }

            val updatesDir = java.io.File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = java.io.File(updatesDir, "CalendarDND-${info.versionName}.apk")
            val digest = java.security.MessageDigest.getInstance("SHA-256")

            try {
                if (connection.responseCode !in 200..299) {
                    logUpdate(
                        context,
                        DebugLogLevel.WARNING,
                        "Download failed (${connection.responseCode}) for ${info.versionName}."
                    )
                    return@withContext null
                }
                logUpdate(context, DebugLogLevel.INFO, "Downloading APK ${info.versionName}…")
                connection.inputStream.use { input ->
                    outFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var read = input.read(buffer)
                        while (read > 0) {
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            read = input.read(buffer)
                        }
                    }
                }
            } catch (_: Exception) {
                logUpdate(context, DebugLogLevel.ERROR, "Download failed for ${info.versionName}.")
                outFile.delete()
                return@withContext null
            } finally {
                connection.disconnect()
            }

            val actualHash = digest.digest().joinToString("") { "%02x".format(it) }
            if (actualHash != expectedHash) {
                logUpdate(
                    context,
                    DebugLogLevel.ERROR,
                    "SHA-256 mismatch for ${info.versionName}."
                )
                outFile.delete()
                return@withContext null
            }

            if (!verifyApkSignature(context, outFile)) {
                logUpdate(
                    context,
                    DebugLogLevel.ERROR,
                    "Signature mismatch for ${info.versionName}."
                )
                outFile.delete()
                return@withContext null
            }

            logUpdate(context, DebugLogLevel.INFO, "APK verified for ${info.versionName}.")
            outFile
        }
    }

    fun canRequestPackageInstalls(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()
    }

    fun createInstallPermissionIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return null
        }
        return Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createInstallIntent(
        context: android.content.Context,
        apkFile: java.io.File
    ): android.content.Intent {
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )
        return android.content.Intent(android.content.Intent.ACTION_INSTALL_PACKAGE).apply {
            data = uri
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun compareSemver(left: Semver, right: Semver): Int {
        return when {
            left.major != right.major -> left.major - right.major
            left.minor != right.minor -> left.minor - right.minor
            else -> left.patch - right.patch
        }
    }

    private fun isAllowedUpdateUrl(raw: String): Boolean {
        val url = runCatching { URL(raw) }.getOrNull() ?: return false
        if (url.protocol.lowercase() != "https") {
            return false
        }
        val host = url.host.lowercase()
        return allowedUpdateHosts.any { host == it || host.endsWith(".$it") }
    }

    private fun verifyApkSignature(context: Context, apkFile: java.io.File): Boolean {
        val packageManager = context.packageManager
        val installedDigests = getSigningCertDigestsForPackage(packageManager, context.packageName)
        if (installedDigests.isEmpty()) {
            return false
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val archiveInfo = packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags) ?: return false
        if (archiveInfo.packageName != context.packageName) {
            return false
        }
        archiveInfo.applicationInfo?.sourceDir = apkFile.absolutePath
        archiveInfo.applicationInfo?.publicSourceDir = apkFile.absolutePath

        val apkDigests = getSigningCertDigests(archiveInfo)
        return apkDigests.isNotEmpty() && apkDigests == installedDigests
    }

    private fun getSigningCertDigestsForPackage(
        packageManager: PackageManager,
        packageName: String
    ): Set<String> {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNING_CERTIFICATES
        } else {
            PackageManager.GET_SIGNATURES
        }
        val info = packageManager.getPackageInfo(packageName, flags)
        return getSigningCertDigests(info)
    }

    private fun getSigningCertDigests(info: android.content.pm.PackageInfo): Set<String> {
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.signingInfo?.apkContentsSigners?.toList()
        } else {
            info.signatures?.toList()
        }
        return signatures
            ?.map { sha256(it.toByteArray()) }
            ?.toSet()
            ?: emptySet()
    }

    private fun sha256(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    suspend fun ensureSignaturePinned(context: Context): Boolean {
        val packageManager = context.packageManager
        val digests = getSigningCertDigestsForPackage(packageManager, context.packageName)
        if (digests.isEmpty()) {
            return false
        }
        val fingerprint = digests.sorted().joinToString(",")
        val settingsStore = SettingsStore(context)
        val stored = settingsStore.getSigningCertFingerprint()
        if (stored.isNullOrBlank()) {
            settingsStore.setSigningCertFingerprint(fingerprint)
            return true
        }
        return stored == fingerprint
    }

    private suspend fun logUpdate(
        context: Context?,
        level: DebugLogLevel,
        message: String
    ) {
        if (context == null) return
        try {
            DebugLogStore(context).appendLog(level, "UPDATE: $message")
        } catch (_: Exception) {
            // Ignore logging failures.
        }
    }
}
