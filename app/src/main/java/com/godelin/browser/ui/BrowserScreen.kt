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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.UUID

private val canvas = Color(0xFF050608)
private val chromeBackground = Color(0xFFF2F9F7)
private val chromeSurface = Color(0xFFD6EAE5)
private val chromeLine = Color(0xFFC3D9D3)
private val chromeForeground = Color(0xFF17201D)
private val chromeMuted = Color(0xFF596661)
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
    val addressFocusRequester = remember { FocusRequester() }
    val context = LocalContext.current
    val updatePreferences = remember(context) {
        context.getSharedPreferences("godelin_updates", android.content.Context.MODE_PRIVATE)
    }
    var updateAvailable by remember {
        mutableStateOf(updatePreferences.getBoolean("update_available", false))
    }
    LaunchedEffect(Unit) {
        val now = System.currentTimeMillis()
        val lastCheck = updatePreferences.getLong("last_check_ms", 0L)
        if (now - lastCheck >= 24L * 60L * 60L * 1000L) {
            val result = withContext(Dispatchers.IO) { checkForGodelinUpdate(context) }
            if (result != null) {
                updatePreferences.edit()
                    .putLong("last_check_ms", now)
                    .putBoolean("update_available", result)
                    .apply()
                updateAvailable = result
            }
        }
    }
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
        Modifier.fillMaxSize().background(chromeBackground)
            .statusBarsPadding().navigationBarsPadding()
    ) {
        // Single premium address bar, sized to remain comfortable on compact phones.
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)
                .height(58.dp).clip(RoundedCornerShape(30.dp))
                .background(chromeSurface)
                .border(1.dp, chromeLine, RoundedCornerShape(30.dp))
                .padding(start = 16.dp, end = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = chromeForeground, modifier = Modifier.size(23.dp))
            Spacer(Modifier.width(14.dp))
            BasicTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f).focusRequester(addressFocusRequester),
                singleLine = true,
                cursorBrush = SolidColor(Color(0xFF4D59D8)),
                textStyle = TextStyle(color = chromeForeground, fontSize = 15.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                decorationBox = { inner ->
                    Box {
                        if (address.isEmpty()) Text("Search or type URL", color = chromeMuted, fontSize = 15.sp)
                        inner()
                    }
                }
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(19.dp), color = Color(0xFFB77BFF), strokeWidth = 1.8.dp)
            } else {
                HeaderAction(Icons.Default.Add, "New tab", onClick = { addTab() })
            }
            HeaderAction(Icons.Default.MoreVert, "More options", onClick = { showMenu = true }, showDot = updateAvailable)
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
                GodelinLaunchPage(
                    onNavigate = { navigate(it) },
                    onAddShortcut = { addTab() },
                    onSearch = {
                        addressFocusRequester.requestFocus()
                        keyboard?.show()
                    }
                )
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

        // Clean mint navigation strip: the search action focuses the real address field.
        Row(
            Modifier.fillMaxWidth().height(64.dp).background(chromeBackground)
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FooterAction(Icons.Default.Home, "Home", true) {
                updateTab(activeTabId) { it.copy(url = "", title = "New tab") }
                address = ""
                progress = 0f
                isLoading = false
                activeWebView?.loadUrl("about:blank")
            }
            FooterAction(Icons.Default.ArrowBack, "Back", activeWebView?.canGoBack() == true) { activeWebView?.goBack() }
            FooterAction(Icons.Default.Search, "Search", true) {
                addressFocusRequester.requestFocus()
                keyboard?.show()
            }
            FooterAction(Icons.Default.Tab, "Tabs", true) { showTabs = true }
            FooterAction(Icons.Default.MoreHoriz, "More", true) { showMenu = true }
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
            SheetAction(Icons.Default.ArrowForward, "Forward page", "Move to the next page in this tab") { showMenu = false; activeWebView?.goForward() }
            SheetAction(Icons.Default.Refresh, "Reload page", "Refresh the current website") { showMenu = false; activeWebView?.reload() }
            SheetAction(Icons.Default.SystemUpdateAlt, "Check for updates", "Find the latest signed Godelin release") { showMenu = false; showUpdateSheet = true }
            SheetAction(Icons.Default.Info, "About Godelin", "Publisher, version and ownership") { showMenu = false; showAboutDialog = true }
            Spacer(Modifier.height(22.dp))
        }
    }

    if (showUpdateSheet) {
        UpdateSheet(
            onDismiss = { showUpdateSheet = false },
            onAvailabilityChanged = { available ->
                updateAvailable = available
                updatePreferences.edit()
                    .putLong("last_check_ms", System.currentTimeMillis())
                    .putBoolean("update_available", available)
                    .apply()
            }
        )
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
        Dialog(
            onDismissRequest = { showTabs = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            TabsOverview(
                tabs = tabs.toList(),
                activeTabId = activeTabId,
                onSelect = { selectTab(it) },
                onClose = { closeTab(it) },
                onNewTab = { addTab() },
                onDismiss = { showTabs = false }
            )
        }
    }
}


@Composable
private fun TabsOverview(
    tabs: List<BrowserTab>,
    activeTabId: String,
    onSelect: (String) -> Unit,
    onClose: (String) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filteredTabs = tabs.filter {
        query.isBlank() || it.title.contains(query, true) || it.url.contains(query, true)
    }

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0D1110))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = Color(0xFF202725), shape = RoundedCornerShape(22.dp)) {
                Row(Modifier.padding(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Color(0xFF35413D), shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tab, contentDescription = null, tint = Color(0xFFBDEFE4), modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(7.dp))
                            Text(tabs.size.toString(), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Icon(Icons.Default.GridView, contentDescription = "Grid view", tint = Color(0xFFE6ECE9), modifier = Modifier.padding(horizontal = 13.dp).size(22.dp))
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close tab overview", tint = Color(0xFFB8C2BE))
            }
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp),
            placeholder = { Text("Search your tabs", color = Color(0xFFB4BFBA)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFB4BFBA)) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFF303735),
                unfocusedContainerColor = Color(0xFF303735),
                focusedBorderColor = Color.Transparent,
                unfocusedBorderColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF8ADBCB)
            )
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredTabs, key = { it.id }) { tab ->
                val selected = tab.id == activeTabId
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp))
                        .background(if (selected) Color(0xFF83D8C9) else Color(0xFF303735))
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) Color(0xFF83D8C9) else Color(0xFF414946),
                            shape = RoundedCornerShape(25.dp)
                        )
                        .clickable { onSelect(tab.id) }
                        .padding(6.dp)
                ) {
                    Row(Modifier.fillMaxWidth().height(42.dp).padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = if (selected) Color(0xFF173C36) else Color(0xFFE5EFEB), modifier = Modifier.size(21.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            tab.title.ifBlank { "New tab" },
                            modifier = Modifier.weight(1f),
                            color = if (selected) Color(0xFF173C36) else Color(0xFFF0F4F2),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        IconButton(onClick = { onClose(tab.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Close tab", tint = if (selected) Color(0xFF24564D) else Color(0xFFCFD8D4), modifier = Modifier.size(18.dp))
                        }
                    }
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(0.82f).clip(RoundedCornerShape(18.dp))
                            .background(if (tab.url.isBlank()) Color(0xFF090D0C) else Color(0xFF1B2421)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tab.url.isBlank()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF202B27)), contentAlignment = Alignment.Center) {
                                    Text("G", color = Color(0xFFBDEFE4), fontSize = 27.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.height(9.dp))
                                Text("Godelin", color = Color(0xFF9BA9A3), fontSize = 11.sp)
                            }
                        } else {
                            Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Language, contentDescription = null, tint = Color(0xFFBDEFE4), modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        runCatching { java.net.URI(tab.url).host ?: tab.url }.getOrDefault(tab.url),
                                        color = Color(0xFFDCE5E1), fontSize = 10.sp, maxLines = 1
                                    )
                                }
                                Text(tab.title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 3)
                            }
                        }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(70.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            FilledIconButton(
                onClick = onNewTab,
                modifier = Modifier.size(58.dp),
                shape = RoundedCornerShape(20.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF83D8C9), contentColor = Color(0xFF123A34))
            ) {
                Icon(Icons.Default.Add, contentDescription = "New tab", modifier = Modifier.size(30.dp))
            }
        }
    }
}

@Composable
private fun HeaderAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    showDot: Boolean = false
) {
    Box(
        Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = label, tint = chromeMuted, modifier = Modifier.size(19.dp))
        if (showDot) {
            Box(
                Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 5.dp)
                    .size(7.dp).clip(CircleShape).background(accent)
            )
        }
    }
}

@Composable
private fun FooterAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = label,
            tint = if (enabled) chromeForeground else Color(0xFF9DAAA6),
            modifier = Modifier.size(22.dp)
        )
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
