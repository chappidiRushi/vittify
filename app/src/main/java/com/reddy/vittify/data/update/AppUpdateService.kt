package com.reddy.vittify.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import com.reddy.vittify.BuildConfig
import com.reddy.vittify.data.preferences.UserPreferencesRepository
import com.reddy.vittify.data.update.model.AppUpdateInfo
import com.reddy.vittify.data.update.model.GithubRelease
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdateService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesRepository: UserPreferencesRepository
) {
    companion object {
        private const val TAG = "AppUpdateService"
        private const val GITHUB_OWNER = "chappidiRushi"
        private const val GITHUB_REPO = "vittify"
        private const val GITHUB_API_BASE = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Checks GitHub for updates.
     * If [BuildConfig.IS_BETA] is true, checks the dev rolling pre-release.
     * If false, checks the latest stable release.
     *
     * @param isManualCheck If true, ignores previously skipped update versions.
     */
    suspend fun checkForUpdate(isManualCheck: Boolean = false): AppUpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val isBeta = BuildConfig.IS_BETA
            val url = if (isBeta) {
                "$GITHUB_API_BASE/releases/tags/pre-release"
            } else {
                "$GITHUB_API_BASE/releases/latest"
            }

            Log.d(TAG, "Checking update from $url (isBeta=$isBeta)")
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "Vittify-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "GitHub API returned status ${response.code}")
                return@withContext null
            }

            val bodyString = response.body?.string() ?: return@withContext null
            val release = json.decodeFromString<GithubRelease>(bodyString)

            // Extract release version
            val releaseVersionStr = extractVersion(release) ?: return@withContext null
            val currentVersionStr = BuildConfig.VERSION_NAME

            Log.d(TAG, "Found release: version=$releaseVersionStr, current=$currentVersionStr, commit=${release.targetCommitish}")

            val hasNewUpdate = if (isBeta) {
                isBetaUpdateAvailable(release, releaseVersionStr, currentVersionStr)
            } else {
                isStableUpdateAvailable(releaseVersionStr, currentVersionStr)
            }

            if (!hasNewUpdate) {
                Log.d(TAG, "No newer update found.")
                return@withContext null
            }

            val updateKey = getUpdateIdentifier(release, releaseVersionStr)
            if (!isManualCheck) {
                val skippedVersion = userPreferencesRepository.skippedUpdateVersion.first()
                if (skippedVersion == updateKey) {
                    Log.d(TAG, "Update $updateKey was skipped by user.")
                    return@withContext null
                }
            }

            // Find best matching APK
            val asset = findBestApkAsset(release) ?: run {
                Log.w(TAG, "No APK asset found in release ${release.name}")
                return@withContext null
            }

            val cleanChangelog = formatChangelog(release.body)

            AppUpdateInfo(
                release = release,
                versionName = releaseVersionStr,
                isBeta = isBeta,
                changelog = cleanChangelog,
                downloadUrl = asset.downloadUrl,
                apkName = asset.name,
                apkSize = asset.size
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking for update", e)
            null
        }
    }

    /**
     * Downloads and installs the given APK update.
     */
    suspend fun downloadAndInstallApk(
        updateInfo: AppUpdateInfo,
        onProgress: (Float) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val updatesDir = File(context.cacheDir, "updates").apply { mkdirs() }
            val destinationFile = File(updatesDir, updateInfo.apkName)

            // Clean previous downloads
            if (destinationFile.exists()) {
                destinationFile.delete()
            }

            val request = Request.Builder()
                .url(updateInfo.downloadUrl)
                .header("User-Agent", "Vittify-Android-App")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Failed to download APK: HTTP ${response.code}"))
            }

            val responseBody = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val totalBytes = responseBody.contentLength()

            responseBody.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8 * 1024)
                    var bytesRead: Int
                    var totalRead: Long = 0

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalBytes > 0) {
                            val progress = (totalRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            onProgress(progress)
                        }
                    }
                    output.flush()
                }
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading APK", e)
            Result.failure(e)
        }
    }

    /**
     * Prompts the Android OS package installer to install the downloaded APK.
     */
    fun triggerInstall(apkFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Error launching package installer", e)
        }
    }

    suspend fun skipThisUpdate(updateInfo: AppUpdateInfo) {
        val key = getUpdateIdentifier(updateInfo.release, updateInfo.versionName)
        userPreferencesRepository.setSkippedUpdateVersion(key)
    }

    private fun getUpdateIdentifier(release: GithubRelease, versionName: String): String {
        return release.targetCommitish?.take(7)?.let { "$versionName-$it" } ?: versionName
    }

    private fun isStableUpdateAvailable(releaseVersion: String, currentVersion: String): Boolean {
        val rel = parseVersionComponents(releaseVersion)
        val cur = parseVersionComponents(currentVersion)
        return rel > cur
    }

    private fun isBetaUpdateAvailable(
        release: GithubRelease,
        releaseVersion: String,
        currentVersion: String
    ): Boolean {
        // First compare semver/patch numbers
        val rel = parseVersionComponents(releaseVersion)
        val cur = parseVersionComponents(currentVersion)
        if (rel > cur) return true
        if (rel < cur) return false

        // If versions are equal, compare commit hashes
        val currentCommit = BuildConfig.GIT_COMMIT_HASH
        val targetCommit = release.targetCommitish
        if (!currentCommit.isNullOrBlank() && !targetCommit.isNullOrBlank()) {
            return !targetCommit.startsWith(currentCommit) && !currentCommit.startsWith(targetCommit)
        }

        return false
    }

    private fun extractVersion(release: GithubRelease): String? {
        val regex = Regex("""[vV]?([0-9]+\.[0-9]+\.[0-9]+(?:-[a-zA-Z0-9.]+)*)""")
        val releaseName = release.name ?: ""
        val match = regex.find(releaseName) ?: regex.find(release.tagName)
        if (match != null) return match.groupValues[1]

        for (asset in release.assets) {
            val assetMatch = regex.find(asset.name)
            if (assetMatch != null) return assetMatch.groupValues[1]
        }

        return release.tagName.removePrefix("v").takeIf { it.isNotBlank() }
    }

    private data class VersionComponents(val major: Int, val minor: Int, val patch: Int) : Comparable<VersionComponents> {
        override fun compareTo(other: VersionComponents): Int {
            if (this.major != other.major) return this.major.compareTo(other.major)
            if (this.minor != other.minor) return this.minor.compareTo(other.minor)
            return this.patch.compareTo(other.patch)
        }
    }

    private fun parseVersionComponents(version: String): VersionComponents {
        val clean = version.removePrefix("v").substringBefore("-")
        val parts = clean.split(".").mapNotNull { it.toIntOrNull() }
        return VersionComponents(
            major = parts.getOrElse(0) { 0 },
            minor = parts.getOrElse(1) { 0 },
            patch = parts.getOrElse(2) { 0 }
        )
    }

    private fun findBestApkAsset(release: GithubRelease): com.reddy.vittify.data.update.model.GithubAsset? {
        val apkAssets = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        if (apkAssets.isEmpty()) return null

        val supportedAbis = Build.SUPPORTED_ABIS.toList()
        for (abi in supportedAbis) {
            val matched = apkAssets.firstOrNull { it.name.contains(abi, ignoreCase = true) }
            if (matched != null) return matched
        }

        // Fallback to universal
        val universal = apkAssets.firstOrNull { it.name.contains("universal", ignoreCase = true) }
        if (universal != null) return universal

        return apkAssets.firstOrNull()
    }

    private fun formatChangelog(rawBody: String?): String {
        if (rawBody.isNullOrBlank()) return "• General bug fixes and stability improvements."

        val lines = rawBody.lines()
        val filtered = lines.filter { line ->
            val trimmed = line.trim()
            !trimmed.startsWith("# Vittify") &&
            !trimmed.startsWith("# Release") &&
            !trimmed.startsWith("> ⚠️") &&
            !trimmed.startsWith("> **Commit**") &&
            !trimmed.startsWith("### Installation") &&
            !trimmed.startsWith("Download the APK") &&
            !trimmed.startsWith("- **Universal APK**") &&
            !trimmed.startsWith("- **Architecture-specific") &&
            trimmed != "---"
        }

        val result = filtered.joinToString("\n").trim()
        return if (result.isBlank()) {
            "• Performance improvements and bug fixes."
        } else {
            result
        }
    }
}
