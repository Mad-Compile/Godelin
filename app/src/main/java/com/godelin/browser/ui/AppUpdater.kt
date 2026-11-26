package com.godelin.browser.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Locale

private const val LATEST_RELEASE_API =
    "https://api.github.com/repos/Mad-Compile/Godelin/releases/latest"
private const val MAX_APK_BYTES = 100L * 1024L * 1024L

private data class GodelinRelease(
    val version: String,
    val notes: String,
    val apkName: String,
    val apkUrl: String,
    val checksumUrl: String
)

private sealed interface UpdateState {
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data object NoRelease : UpdateState
    data class Available(val release: GodelinRelease) : UpdateState
    data class Downloading(val release: GodelinRelease) : UpdateState
    data class ReadyToInstall(val release: GodelinRelease, val apk: File) : UpdateState
    data class Failed(val message: String) : UpdateState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UpdateSheet(
    onDismiss: () -> Unit,
    onAvailabilityChanged: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<UpdateState>(UpdateState.Checking) }
    var checkAttempt by remember { mutableIntStateOf(0) }
    var installHint by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(checkAttempt) {
        state = UpdateState.Checking
        installHint = null
        val checkedState = withContext(Dispatchers.IO) {
            runCatching { findLatestRelease(context) }
                .fold(
                    onSuccess = { it },
                    onFailure = { UpdateState.Failed(it.message ?: "Could not check for updates.") }
                )
        }
        state = checkedState
        if (checkedState !is UpdateState.Failed) {
            onAvailabilityChanged(checkedState is UpdateState.Available)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Godelin updates",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Stable releases published by Mad Compile™",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            when (val current = state) {
                UpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Checking GitHub Releases…", modifier = Modifier.padding(start = 12.dp))
                    }
                }
                UpdateState.UpToDate -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("You're using the latest stable version.", modifier = Modifier.padding(start = 10.dp))
                    }
                    Text(
                        text = "Installed version: ${installedVersionName(context)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                UpdateState.NoRelease -> {
                    Text("No stable release has been published yet.")
                    Text(
                        "Development builds can be newer than the first signed public release. Stable updates appear here after a GitHub Release is published.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                    TextButton(onClick = { checkAttempt++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("Check again", modifier = Modifier.padding(start = 6.dp))
                    }
                }
                is UpdateState.Available -> {
                    Text("Version ${current.release.version} is available", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = current.release.notes.ifBlank { "A new version of Godelin is ready." }.take(1600),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                state = UpdateState.Downloading(current.release)
                                state = withContext(Dispatchers.IO) {
                                    runCatching { downloadAndVerify(context, current.release) }
                                        .fold(
                                            onSuccess = { UpdateState.ReadyToInstall(current.release, it) },
                                            onFailure = { UpdateState.Failed(it.message ?: "Download failed.") }
                                        )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Text("Download update", modifier = Modifier.padding(start = 8.dp))
                    }
                }
                is UpdateState.Downloading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text("Downloading and verifying ${current.release.version}…", modifier = Modifier.padding(start = 12.dp))
                    }
                    Text(
                        "The APK is checked against its SHA-256 checksum and the installed signing certificate before Android is asked to install it.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                is UpdateState.ReadyToInstall -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Download verified", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 9.dp))
                    }
                    Text(
                        "Android will ask you to confirm the installation. Your browsing data stays on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Button(
                        onClick = {
                            installHint = runCatching { installDownloadedApk(context, current.apk) }
                                .getOrElse { "Couldn't open Android's installer: ${it.message ?: "unknown error"}" }
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Install update")
                    }
                    installHint?.let {
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                    }
                }
                is UpdateState.Failed -> {
                    Text(current.message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { checkAttempt++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Text("Try again", modifier = Modifier.padding(start = 6.dp))
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Text(
                text = "Updates are optional. Godelin downloads only after you tap Download, checks the package, then hands installation to Android.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

internal fun checkForGodelinUpdate(context: Context): Boolean? =
    runCatching { findLatestRelease(context) is UpdateState.Available }.getOrNull()

private fun findLatestRelease(context: Context): UpdateState {
    val installedVersion = installedVersionName(context)
    val response = requestText(LATEST_RELEASE_API, accept = "application/vnd.github+json", allow404 = true)
    if (response == null) return UpdateState.NoRelease
    val json = JSONObject(response)
    val tag = json.optString("tag_name").removePrefix("v")
    if (tag.isBlank() || compareVersions(tag, installedVersion) <= 0) {
        return UpdateState.UpToDate
    }

    val assets = json.optJSONArray("assets") ?: error("The latest release has no downloadable assets.")
    val expectedApkName = "Godelin-v$tag.apk"
    var apkUrl: String? = null
    var checksumUrl: String? = null
    for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        val name = asset.optString("name")
        val url = asset.optString("browser_download_url")
        if (name == expectedApkName && isGitHubAssetUrl(url)) {
            apkUrl = url
        }
    }
    val checksumName = "$expectedApkName.sha256"
    for (i in 0 until assets.length()) {
        val asset = assets.optJSONObject(i) ?: continue
        if (asset.optString("name") == checksumName) {
            val url = asset.optString("browser_download_url")
            if (isGitHubAssetUrl(url)) checksumUrl = url
        }
    }
    return UpdateState.Available(
        GodelinRelease(
            version = tag,
            notes = json.optString("body"),
            apkName = expectedApkName,
            apkUrl = requireNotNull(apkUrl),
            checksumUrl = requireNotNull(checksumUrl) { "This release is missing its APK checksum file." }
        )
    )
}

private fun downloadAndVerify(context: Context, release: GodelinRelease): File {
    val checksumText = requestText(release.checksumUrl) ?: error("The release checksum file could not be downloaded.")
    val expectedHash = Regex("(?i)^([a-f0-9]{64})").find(checksumText.trim())?.groupValues?.get(1)
        ?: error("The release checksum file is invalid.")

    val updateDir = File(context.cacheDir, "updates").apply { mkdirs() }
    updateDir.listFiles()?.filter { it.isFile && it.extension == "apk" }?.forEach { it.delete() }
    val apk = File(updateDir, release.apkName)
    val connection = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 15000
        readTimeout = 30000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "Godelin-Android")
        setRequestProperty("Accept", "application/octet-stream")
    }
    try {
        val status = connection.responseCode
        if (status !in 200..299) error("APK download failed (HTTP $status).")
        val length = connection.contentLengthLong
        if (length > MAX_APK_BYTES) error("The downloaded APK is larger than the allowed size.")
        FileOutputStream(apk).use { output ->
            connection.inputStream.use { input ->
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var total = 0L
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > MAX_APK_BYTES) error("The downloaded APK exceeded the allowed size.")
                    output.write(buffer, 0, count)
                }
            }
        }
    } catch (error: Throwable) {
        apk.delete()
        throw error
    } finally {
        connection.disconnect()
    }

    val actualHash = sha256(apk)
    if (!actualHash.equals(expectedHash, ignoreCase = true)) {
        apk.delete()
        error("APK checksum verification failed. The file was discarded.")
    }
    verifyPackageAndSigner(context, apk)
    return apk
}

private fun verifyPackageAndSigner(context: Context, apk: File) {
    val packageManager = context.packageManager
    val flags = if (Build.VERSION.SDK_INT >= 28) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }
    val archive = packageManager.getPackageArchiveInfo(apk.absolutePath, flags)
        ?: error("Android could not read the downloaded APK.")
    if (archive.packageName != context.packageName) {
        error("The downloaded APK is not the Godelin application.")
    }
    val installed = packageManager.getPackageInfo(context.packageName, flags)
    val archiveVersion = packageVersionCode(archive)
    val installedVersion = packageVersionCode(installed)
    if (archiveVersion <= installedVersion) {
        error("The downloaded package is not newer than the installed version.")
    }
    val installedCertificates = certificateDigests(installed)
    val downloadedCertificates = certificateDigests(archive)
    if (installedCertificates.isEmpty() || installedCertificates != downloadedCertificates) {
        error("Signing certificate mismatch. The update was rejected to protect this installation.")
    }
}

@Suppress("DEPRECATION")
private fun packageVersionCode(info: PackageInfo): Long =
    if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

@Suppress("DEPRECATION")
private fun certificateDigests(info: PackageInfo): Set<String> {
    val signatures = if (Build.VERSION.SDK_INT >= 28) {
        info.signingInfo?.apkContentsSigners ?: emptyArray()
    } else {
        info.signatures ?: emptyArray()
    }
    return signatures.map { sha256(it.toByteArray()) }.toSet()
}

private fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            digest.update(buffer, 0, read)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
}

private fun sha256(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(Locale.ROOT, it) }

private fun requestText(
    url: String,
    accept: String = "text/plain",
    allow404: Boolean = false
): String? {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        instanceFollowRedirects = true
        connectTimeout = 12000
        readTimeout = 15000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "Godelin-Android")
        setRequestProperty("Accept", accept)
    }
    try {
        val status = connection.responseCode
        if (allow404 && status == 404) return null
        if (status !in 200..299) error("GitHub returned HTTP $status.")
        return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
    } finally {
        connection.disconnect()
    }
}

private fun isGitHubAssetUrl(value: String): Boolean = runCatching {
    val uri = Uri.parse(value)
    uri.scheme == "https" && uri.host == "github.com"
}.getOrDefault(false)

private fun compareVersions(left: String, right: String): Int {
    val leftParts = versionParts(left)
    val rightParts = versionParts(right)
    val count = maxOf(leftParts.size, rightParts.size)
    for (index in 0 until count) {
        val a = leftParts.getOrElse(index) { 0 }
        val b = rightParts.getOrElse(index) { 0 }
        if (a != b) return a.compareTo(b)
    }
    val leftIsPrerelease = left.contains('-')
    val rightIsPrerelease = right.contains('-')
    return when {
        leftIsPrerelease == rightIsPrerelease -> 0
        leftIsPrerelease -> -1
        else -> 1
    }
}

private fun versionParts(value: String): List<Int> =
    value.removePrefix("v")
        .substringBefore('-')
        .substringBefore('+')
        .split('.')
        .map { it.toIntOrNull() ?: 0 }

private fun installedVersionName(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

private fun installDownloadedApk(context: Context, apk: File): String {
    if (!context.packageManager.canRequestPackageInstalls()) {
        val settingsIntent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(settingsIntent)
        return "Allow installs from Godelin in Android settings, return here, then tap Install update again."
    }

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apk
    )
    val installIntent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(installIntent)
    return "Android's package installer opened."
}
