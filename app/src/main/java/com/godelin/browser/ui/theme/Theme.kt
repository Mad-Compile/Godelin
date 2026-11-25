package com.godelin.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val GodelinColors = darkColorScheme(
    primary = GodelinAccent,
    onPrimary = GodelinBlack,
    secondary = GodelinMuted,
    onSecondary = GodelinBlack,
    background = GodelinBlack,
    onBackground = GodelinWhite,
    surface = GodelinSurface,
    onSurface = GodelinWhite,
    surfaceVariant = GodelinElevated,
    onSurfaceVariant = GodelinMuted,
    outline = GodelinMuted
)

@Composable
fun GodelinTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = GodelinColors, content = content)
}
