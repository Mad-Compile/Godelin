package com.godelin.browser.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
private val pageBackground = Color(0xFF08090B)
private val panel = Color(0xFF15171B)
private val muted = Color(0xFF969CA6)
private val bright = Color(0xFFF4F5F6)

@Composable
fun BrowserScreen() {
    var address by remember { mutableStateOf("") }
    var currentUrl by remember { mutableStateOf("") }
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val keyboard = LocalSoftwareKeyboardController.current

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

    BackHandler(enabled = webView?.canGoBack() == true) { webView?.goBack() }

    Column(Modifier.fillMaxSize().background(pageBackground)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 18.dp, end = 12.dp, top = 18.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(34.dp).background(bright, RoundedCornerShape(11.dp)), contentAlignment = Alignment.Center) {
                Text("G", color = pageBackground, fontSize = 23.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text("GODELIN", color = bright, fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.2.sp)
                Text("A clearer way to browse", color = muted, fontSize = 10.sp)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { navigate(HOME_URL) }) {
                Icon(Icons.Default.Home, contentDescription = "Home", tint = muted)
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(54.dp)
                .background(panel, RoundedCornerShape(18.dp)).padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, contentDescription = "Connection", tint = muted, modifier = Modifier.size(17.dp))
            Spacer(Modifier.width(10.dp))
            BasicTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = bright, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { navigate(address) }),
                decorationBox = { inner ->
                    Box {
                        if (address.isEmpty()) Text("Search Google or enter address", color = muted, fontSize = 13.sp)
                        inner()
                    }
                }
            )
            if (address.isNotEmpty()) {
                IconButton(onClick = { address = "" }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = muted, modifier = Modifier.size(17.dp))
                }
            } else Icon(Icons.Default.Search, contentDescription = "Search", tint = muted, modifier = Modifier.size(19.dp))
        }

        if (isLoading) {
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                Modifier.fillMaxWidth().padding(top = 4.dp),
                color = bright,
                trackColor = pageBackground
            )
        } else Spacer(Modifier.height(4.dp))

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (currentUrl.isBlank()) {
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 26.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(76.dp).background(bright, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                        Text("G", color = pageBackground, fontSize = 54.sp, fontWeight = FontWeight.Black)
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("The web, uncluttered.", color = bright, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Text("Search the web or enter a website above.", color = muted, fontSize = 13.sp)
                    Spacer(Modifier.height(26.dp))
                    Surface(color = panel, shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, contentDescription = null, tint = muted, modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Powered by Google Search", color = muted, fontSize = 12.sp)
                        }
                    }
                }
            }
            BrowserWebView(
                onCreated = { webView = it },
                onPageStarted = { url -> currentUrl = url; address = url; isLoading = true },
                onPageFinished = { url -> currentUrl = url; address = url; isLoading = false; progress = 1f },
                onProgress = { value -> progress = value / 100f; isLoading = value < 100 }
            )
        }

        Row(
            Modifier.fillMaxWidth().background(Color(0xFF101216)).padding(horizontal = 18.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() }) {
                Icon(Icons.Default.ArrowBack, "Back", tint = if (webView?.canGoBack() == true) bright else muted)
            }
            IconButton(onClick = { if (webView?.canGoForward() == true) webView?.goForward() }) {
                Icon(Icons.Default.ArrowForward, "Forward", tint = if (webView?.canGoForward() == true) bright else muted)
            }
            IconButton(onClick = { webView?.reload() }) { Icon(Icons.Default.Refresh, "Reload", tint = bright) }
            IconButton(onClick = { navigate(HOME_URL) }) { Icon(Icons.Default.Home, "Home", tint = bright) }
            IconButton(onClick = { /* More actions arrive in a later milestone. */ }) {
                Icon(Icons.Default.MoreVert, "More options", tint = muted)
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun BrowserWebView(
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
                    override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) = onPageStarted(url)
                    override fun onPageFinished(view: WebView, url: String) = onPageFinished(url)
                }
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView, newProgress: Int) = onProgress(newProgress)
                }
                onCreated(this)
            }
        }
    )
}
