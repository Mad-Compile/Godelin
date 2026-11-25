package com.godelin.browser

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.godelin.browser.ui.BrowserScreen
import com.godelin.browser.ui.theme.GodelinTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GodelinTheme { BrowserScreen() }
        }
    }
}
