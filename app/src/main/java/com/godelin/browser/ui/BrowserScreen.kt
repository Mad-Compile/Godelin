package com.godelin.browser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder
import java.util.UUID

private val canvas = Color(0xFF101113)
private val surface = Color(0xFF181A1D)
private val raised = Color(0xFF202226)
private val line = Color(0xFF2B2E33)
private val foreground = Color(0xFFF1F1EF)
private val secondary = Color(0xFF96999F)
private val accent = Color(0xFFE5E7EB)
private val accentInk = Color(0xFF17191C)

private data class BrowserTab(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New tab",
    val url: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    val tabs = remember { mutableStateListOf(BrowserTab()) }
    val webViews = remember { mutableMapOf<String, WebView>() }
    var activeTabId by remember { mutableStateOf(tabs.first().id) }
    var address by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    var showUpdateSheet by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val context = LocalContext.current
    val activeTab = tabs.firstOrNull { it.id == activeTabId } ?: tabs.first()
    val activeWebView = webViews[activeTabId]
    val currentUrl = activeTab.url
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "page-progress"
    )

    fun updateTab(id: String, transform: (BrowserTab) -> BrowserTab) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index >= 0) tabs[index] = transform(tabs[index])
    }

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isBlank()) return
        val destination = when {
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")
        }
        updateTab(activeTabId) { it.copy(url = destination, title = destination) }
        webViews[activeTabId]?.loadUrl(destination)
        address = destination
        progress = 0f
        isLoading = true
        keyboard?.hide()
    }

    fun addTab() {
        val tab = BrowserTab()
        tabs.add(tab)
        activeTabId = tab.id
        address = ""
        progress = 0f
        isLoading = false
        showTabs = false
    }

    fun closeTab(id: String) {
        val index = tabs.indexOfFirst { it.id == id }
        if (index < 0) return
        webViews.remove(id)?.let { view ->
            view.stopLoading()
            (view.parent as? ViewGroup)?.removeView(view)
            view.loadUrl("about:blank")
            view.clearHistory()
            view.destroy()
        }
        tabs.removeAt(index)
        if (tabs.isEmpty()) {
            val replacement = BrowserTab()
            tabs.add(replacement)
            activeTabId = replacement.id
            address = ""
            progress = 0f
            isLoading = false
        } else if (activeTabId == id) {
            val replacement = tabs[index.coerceAtMost(tabs.lastIndex)]
            activeTabId = replacement.id
            address = replacement.url
            progress = 0f
            isLoading = false
        }
    }

    fun selectTab(id: String) {
        activeTabId = id
        address = tabs.firstOrNull { it.id == id }?.url.orEmpty()
        progress = 0f
        isLoading = false
        showTabs = false
    }

    LaunchedEffect(activeTabId) {
        address = tabs.firstOrNull { it.id == activeTabId }?.url.orEmpty()
        progress = 0f
        isLoading = false
    }

    BackHandler(enabled = showMenu || showTabs || activeWebView?.canGoBack() == true) {
        when {
            showMenu -> showMenu = false
            showTabs -> showTabs = false
            else -> activeWebView?.goBack()
        }
    }

    Column(
        Modifier.fillMaxSize().background(canvas)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        // Compact browser chrome
        Row(
            Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(accent),
                    contentAlignment = Alignment.Center
                ) {
                    Text("g", color = accentInk, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(9.dp))
                Text("godelin", color = foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp)
                if (isLoading) {
                    Spacer(Modifier.width(9.dp))
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        color = secondary,
                        strokeWidth = 1.5.dp
                    )
                }
            }
            HeaderAction(Icons.Default.Add, "New tab", ::addTab)
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier.height(34.dp).clip(RoundedCornerShape(10.dp))
                    .background(surface).border(1.dp, line, RoundedCornerShape(10.dp))
                    .clickable { showTabs = true }.padding(horizontal = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Tab, contentDescription = "Open tabs", tint = secondary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tabs.size.toString(), color = foreground, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.width(5.dp))
            HeaderAction(Icons.Default.MoreHoriz, "More options") { showMenu = true }
        }

        // Address field is the primary control, with restrained focus treatment.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                .height(48.dp).clip(RoundedCornerShape(14.dp))
                .background(surface).border(1.dp, line, RoundedCornerShape(14.dp))
                .padding(start = 13.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                contentDescription = if (currentUrl.startsWith("https://")) "Secure connection" else "Search",
                tint = secondary, modifier = Modifier.size(17.dp)
            )
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                cursorBrush = SolidColor(accent),
                textStyle = TextStyle(color = foreground, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                decorationBox = { inner ->
                    Box {
                        if (address.isEmpty()) {
                            Text("Search or enter address", color = secondary, fontSize = 14.sp)
                        }
                        inner()
                    }
                }
            )
            if (address.isNotEmpty()) {
                IconButton(onClick = { address = "" }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear address", tint = secondary, modifier = Modifier.size(17.dp))
                }
            } else {
                Icon(Icons.Default.MicNone, contentDescription = "Voice search", tint = secondary, modifier = Modifier.padding(horizontal = 8.dp).size(18.dp))
            }
        }

        AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = accent,
                trackColor = Color.Transparent
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (currentUrl.isBlank()) {
                StartPage(onNavigate = { navigate(it) })
            }

            tabs.filter { it.url.isNotBlank() }.forEach { tab ->
                BrowserWebView(
                    initialUrl = tab.url,
                    isActive = tab.id == activeTabId,
                    onCreated = { view -> webViews[tab.id] = view },
                    onPageStarted = { url ->
                        updateTab(tab.id) { it.copy(url = url) }
                        if (tab.id == activeTabId) { address = url; isLoading = true; progress = 0f }
                    },
                    onPageFinished = { url ->
                        updateTab(tab.id) { it.copy(url = url) }
                        if (tab.id == activeTabId) { address = url; isLoading = false; progress = 1f }
                    },
                    onTitle = { title ->
                        if (title.isNotBlank()) updateTab(tab.id) { it.copy(title = title) }
                    },
                    onProgress = { value ->
                        if (tab.id == activeTabId) { progress = value / 100f; isLoading = value < 100 }
                    }
                )
            }
        }

        // Navigation controls sit on a quiet, full-width footer rather than a floating capsule.
        HorizontalDivider(color = line, thickness = 1.dp)
        Row(
            Modifier.fillMaxWidth().height(58.dp).padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterAction(Icons.Default.ArrowBack, "Back", activeWebView?.canGoBack() == true) { activeWebView?.goBack() }
            FooterAction(Icons.Default.ArrowForward, "Forward", activeWebView?.canGoForward() == true) { activeWebView?.goForward() }
            FooterAction(
                if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                if (isLoading) "Stop" else "Reload",
                currentUrl.isNotBlank()
            ) {
                if (isLoading) activeWebView?.stopLoading() else activeWebView?.reload()
            }
            FooterAction(Icons.Default.Home, "Home", true) {
                updateTab(activeTabId) { it.copy(url = "", title = "New tab") }
                address = ""
                progress = 0f
                isLoading = false
                activeWebView?.loadUrl("about:blank")
            }
            FooterAction(Icons.Default.BookmarkBorder, "Save", currentUrl.isNotBlank()) { showMenu = true }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = surface,
            contentColor = foreground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = secondary) }
        ) {
            Text("Browser", Modifier.padding(horizontal = 22.dp, vertical = 4.dp), color = foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            SheetAction(Icons.Default.Add, "New tab", "Start a separate browsing session") { showMenu = false; addTab() }
            SheetAction(Icons.Default.Tab, "All tabs", "Switch between open pages") { showMenu = false; showTabs = true }
            SheetAction(Icons.Default.Refresh, "Reload page", "Refresh the current website") { showMenu = false; activeWebView?.reload() }
            SheetAction(Icons.Default.SystemUpdateAlt, "Check for updates", "Find the latest signed Godelin release") { showMenu = false; showUpdateSheet = true }
            SheetAction(Icons.Default.Info, "About Godelin", "Publisher, version and ownership") { showMenu = false; showAboutDialog = true }
            Spacer(Modifier.height(22.dp))
        }
    }

    if (showUpdateSheet) {
        UpdateSheet(onDismiss = { showUpdateSheet = false })
    }

    if (showAboutDialog) {
        val appVersion = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: "Unknown"
        }
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = surface,
            title = { Text("Godelin", color = foreground) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Version $appVersion", color = secondary)
                    Text("Published by Mad Compile™", color = foreground)
                    Text("Based in India", color = secondary)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Copyright (C) Mad Compile. All Rights Reserved.",
                        color = secondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showTabs) {
        ModalBottomSheet(
            onDismissRequest = { showTabs = false },
            containerColor = surface,
            contentColor = foreground,
            dragHandle = { BottomSheetDefaults.DragHandle(color = secondary) }
        ) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tabs", color = foreground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${tabs.size} open", color = secondary, fontSize = 12.sp)
                }
                FilledTonalButton(onClick = { addTab() }, colors = ButtonDefaults.filledTonalButtonColors(containerColor = raised, contentColor = foreground)) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("New tab")
                }
            }
            Spacer(Modifier.height(14.dp))
            tabs.toList().forEach { tab ->
                val selected = tab.id == activeTabId
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(if (selected) raised else Color.Transparent)
                        .border(1.dp, if (selected) Color(0xFF454950) else line, RoundedCornerShape(13.dp))
                        .clickable { selectTab(tab.id) }
                        .padding(start = 13.dp, end = 5.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(surface), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = secondary, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(11.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tab.title, color = foreground, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text(tab.url.ifBlank { "New tab" }, color = secondary, fontSize = 11.sp, maxLines = 1)
                    }
                    IconButton(onClick = { closeTab(tab.id) }) {
                        Icon(Icons.Default.Close, contentDescription = "Close tab", tint = secondary, modifier = Modifier.size(17.dp))
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun StartPage(onNavigate: (String) -> Unit) {
    GodelinHomeFeed(onNavigate = onNavigate)
}

@Composable
private fun HeaderAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = secondary, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun FooterAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Column(
        Modifier.widthIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 7.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) foreground else Color(0xFF55595F), modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(3.dp))
        Text(label, color = if (enabled) secondary else Color(0xFF55595F), fontSize = 9.sp)
    }
}

@Composable
private fun SheetAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(11.dp)).background(raised), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = foreground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = secondary, fontSize = 11.sp)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    initialUrl: String,
    isActive: Boolean,
    onCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitle: (String) -> Unit,
    onProgress: (Int) -> Unit
) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val scheme = request.url.scheme?.lowercase()
                        return scheme != "https" && scheme != "http"
                    }
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { onPageStarted(url) }
                    override fun onPageFinished(view: WebView, url: String) { onPageFinished(url) }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) { onProgress(newProgress) }
                    override fun onReceivedTitle(view: WebView, title: String?) { if (!title.isNullOrBlank()) onTitle(title) }
                }
                setBackgroundColor(android.graphics.Color.rgb(16, 17, 19))
                visibility = if (isActive) View.VISIBLE else View.GONE
                onCreated(this)
                loadUrl(initialUrl)
            }
        },
        update = { view ->
            view.visibility = if (isActive) View.VISIBLE else View.GONE
            if (view.parent == null) onCreated(view)
        }
    )
}
