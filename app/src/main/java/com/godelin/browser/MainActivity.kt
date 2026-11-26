package com.godelin.browser

import android.os.Bundle
import android.graphics.Color as AndroidColor
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.view.WindowCompat
import com.godelin.browser.ui.BrowserScreen
import com.godelin.browser.ui.theme.GodelinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = AndroidColor.rgb(242, 249, 247)
        window.navigationBarColor = AndroidColor.rgb(242, 249, 247)
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
        setContent {
            GodelinTheme { BrowserScreen() }
        }
    }
}
