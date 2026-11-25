package com.godelin.browser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import java.net.URLEncoder

private const val HOME_URL = "https://www.google.com"
private val ink = Color(0xFF08090B)
private val obsidianPanel = Color(0xFF14161A)
private val obsidianRaised = Color(0xFF1B1E23)
private val edge = Color(0xFF30343B)
private val white = Color(0xFFF4F7FC)
private val muted = Color(0xFF9BA8BA)
private val accent = Color(0xFFD9DDE3)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrowserScreen() {
    var address by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showTabs by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 500f),
        label = "page-progress"
    )

    fun navigate(input: String) {
        val value = input.trim()
        if (value.isBlank()) return
        val destination = when {
            value.startsWith("https://", true) || value.startsWith("http://", true) -> value
            value.contains(".") && !value.contains(" ") -> "https://$value"
            else -> "https://www.google.com/search?q=" + URLEncoder.encode(value, "UTF-8")
        }
        address = destination
        currentUrl = destination
        webView?.loadUrl(destination)
        keyboard?.hide()
    }

    BackHandler(enabled = showMenu || showTabs || webView?.canGoBack() == true) {
        when {
            showMenu -> showMenu = false
            showTabs -> showTabs = false
            else -> webView?.goBack()
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.linearGradient(
                listOf(Color(0xFF101114), ink, Color(0xFF10131D))
            )
        )
    ) {
        // Restrained ambient shading gives the obsidian surfaces depth without expensive blur effects.
        Box(
            Modifier.align(Alignment.TopEnd).size(260.dp)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF202329), Color.Transparent)),
                    CircleShape
                )
        )
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.size(38.dp).shadow(12.dp, RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFFF1F2F4), Color(0xFFBFC4CC))), RoundedCornerShape(13.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("G", color = Color(0xFF111317), fontSize = 25.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("GODELIN", color = white, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.5.sp)
                    Text("YOUR SPACE ON THE WEB", color = muted, fontSize = 9.sp, letterSpacing = 1.25.sp)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier.clip(CircleShape).background(obsidianPanel).border(1.dp, edge, CircleShape)
                        .clickable { showMenu = true }.padding(10.dp)
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Browser options", tint = white, modifier = Modifier.size(19.dp))
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(56.dp)
                    .shadow(14.dp, RoundedCornerShape(20.dp))
                    .background(obsidianRaised, RoundedCornerShape(20.dp))
                    .border(1.dp, edge, RoundedCornerShape(20.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (currentUrl.startsWith("https://")) Icons.Default.Lock else Icons.Default.Search,
                    contentDescription = if (currentUrl.startsWith("https://")) "Secure connection" else "Search",
                    tint = accent, modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(11.dp))
                BasicTextField(
                    value = address,
                    onValueChange = { address = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(color = white, fontSize = 14.sp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                    decorationBox = { inner ->
                        Box {
                            if (address.isEmpty()) Text("Search or enter website", color = muted, fontSize = 13.sp)
                            inner()
                        }
                    }
                )
                if (address.isNotEmpty()) {
                    IconButton(onClick = { address = "" }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Clear address", tint = muted, modifier = Modifier.size(17.dp))
                    }
                } else {
                    Icon(Icons.Default.MicNone, contentDescription = "Voice search (coming soon)", tint = muted, modifier = Modifier.size(19.dp))
                }
            }

            AnimatedVisibility(visible = isLoading, enter = fadeIn(), exit = fadeOut()) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(2.dp),
                    color = accent,
                    trackColor = Color.Transparent
                )
            }

            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (currentUrl.isBlank()) {
                    Column(
                        Modifier.fillMaxSize().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            Modifier.size(88.dp).shadow(24.dp, RoundedCornerShape(28.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFFF1F2F4), Color(0xFFBFC4CC))), RoundedCornerShape(28.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("G", color = Color(0xFF111317), fontSize = 61.sp, fontWeight = FontWeight.Black)
                        }
                        Spacer(Modifier.height(25.dp))
                        Text("The web, in your element.", color = white, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(8.dp))
                        Text("A calmer space to explore what's next.", color = muted, fontSize = 13.sp)
                        Spacer(Modifier.height(30.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            QuickLink("Google", Icons.Default.Search) { navigate("https://www.google.com") }
                            QuickLink("YouTube", Icons.Default.PlayArrow) { navigate("https://www.youtube.com") }
                            QuickLink("Wikipedia", Icons.Default.MenuBook) { navigate("https://www.wikipedia.org") }
                        }
                        Spacer(Modifier.height(24.dp))
                        ObsidianCard(Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("Made for focused browsing", color = white, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                    Text("Your browser. Your pace.", color = muted, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                if (currentUrl.isNotBlank()) {
                    BrowserWebView(
                        initialUrl = currentUrl,
                        onCreated = { webView = it },
                        onPageStarted = { url -> currentUrl = url; address = url; isLoading = true },
                        onPageFinished = { url -> currentUrl = url; address = url; isLoading = false; progress = 1f },
                        onProgress = { value -> progress = value / 100f; isLoading = value < 100 }
                    )
                }
            }

            // Floating obsidian navigation dock.
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)
                    .shadow(20.dp, RoundedCornerShape(25.dp))
                    .background(obsidianRaised, RoundedCornerShape(25.dp))
                    .border(1.dp, edge, RoundedCornerShape(25.dp))
                    .padding(horizontal = 7.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DockButton(Icons.Default.Home, "Home", enabled = true) { navigate(HOME_URL) }
                DockButton(Icons.Default.ArrowBack, "Back", enabled = webView?.canGoBack() == true) { webView?.goBack() }
                DockButton(Icons.Default.ArrowForward, "Forward", enabled = webView?.canGoForward() == true) { webView?.goForward() }
                DockButton(Icons.Default.Refresh, "Reload", enabled = currentUrl.isNotBlank()) { webView?.reload() }
                Box(
                    Modifier.clip(RoundedCornerShape(15.dp)).background(Color(0xFF202329))
                        .border(1.dp, Color(0xFF424750), RoundedCornerShape(15.dp))
                        .clickable { showTabs = true }.padding(horizontal = 13.dp, vertical = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Tab, contentDescription = "Tabs", tint = accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("1", color = white, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMenu = false },
            containerColor = Color(0xFF15171B),
            contentColor = white,
            dragHandle = { BottomSheetDefaults.DragHandle(color = muted) }
        ) {
            Text("Browser tools", Modifier.padding(horizontal = 22.dp), color = white, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            SheetAction(Icons.Default.Home, "Go to home", "Return to your start page") {
                showMenu = false
                currentUrl = ""
                address = ""
            }
            SheetAction(Icons.Default.Refresh, "Reload page", "Refresh the current website") {
                showMenu = false
                webView?.reload()
            }
            SheetAction(Icons.Default.Tab, "Tab overview", "View your open-tab workspace") {
                showMenu = false
                showTabs = true
            }
            SheetAction(Icons.Default.Settings, "Settings", "Browser preferences are planned next") {
                showMenu = false
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showTabs) {
        ModalBottomSheet(
            onDismissRequest = { showTabs = false },
            containerColor = Color(0xFF15171B),
            contentColor = white,
            dragHandle = { BottomSheetDefaults.DragHandle(color = muted) }
        ) {
            Text("Your tabs", Modifier.padding(horizontal = 22.dp), color = white, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            ObsidianCard(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Language, contentDescription = null, tint = accent)
                        Spacer(Modifier.width(10.dp))
                        Text(if (currentUrl.isBlank()) "New tab" else currentUrl, color = white, fontSize = 13.sp, maxLines = 1)
                    }
                    Spacer(Modifier.height(9.dp))
                    Text("1 active tab", color = muted, fontSize = 11.sp)
                }
            }
            Text(
                "Multi-tab sessions and tab groups are part of the next browser-engine milestone.",
                Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                color = muted, fontSize = 12.sp
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}

@Composable
private fun QuickLink(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(17.dp)).background(obsidianPanel).border(1.dp, edge, RoundedCornerShape(17.dp))
            .clickable(onClick = onClick).padding(horizontal = 15.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, color = white, fontSize = 10.sp)
    }
}

@Composable
private fun ObsidianCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier.shadow(12.dp, RoundedCornerShape(20.dp))
            .background(obsidianPanel, RoundedCornerShape(20.dp))
            .border(1.dp, edge, RoundedCornerShape(20.dp))
    ) { content() }
}

@Composable
private fun DockButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier.clip(RoundedCornerShape(15.dp)).clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = if (enabled) white else Color(0xFF626873), modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = if (enabled) muted else Color(0xFF626873), fontSize = 9.sp)
    }
}

@Composable
private fun SheetAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(38.dp).background(obsidianPanel, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(13.dp))
        Column {
            Text(title, color = white, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = muted, fontSize = 11.sp)
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
    initialUrl: String,
    onCreated: (WebView) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
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
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                        onPageStarted(url)
                    }
                    override fun onPageFinished(view: WebView, url: String) {
                        onPageFinished(url)
                    }
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) {
                        onProgress(newProgress)
                    }
                }
                setBackgroundColor(android.graphics.Color.rgb(8, 10, 16))
                onCreated(this)
                loadUrl(initialUrl)
            }
        }
    )
}
