package com.godelin.browser.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val launchBlack = Color(0xFF050508)
private val launchPurple = Color(0xFF9B55F5)
private val launchSilver = Color(0xFFE8EAF0)

@Composable
fun GodelinLaunchPage(
    onNavigate: (String) -> Unit,
    onAddShortcut: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(launchBlack)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(Brush.verticalGradient(listOf(Color(0xFF050507), Color(0xFF090810), Color(0xFF030305))))
            val aurora = Path().apply {
                moveTo(-w * .1f, h * .61f)
                cubicTo(w * .12f, h * .47f, w * .02f, h * .30f, w * .45f, h * .25f)
                cubicTo(w * .78f, h * .20f, w * 1.03f, h * .18f, w * 1.1f, h * .02f)
            }
            drawPath(aurora, Brush.linearGradient(listOf(Color(0xFF191522), Color(0xFF7652A8), Color(0xFF171321))), style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
            val far = Path().apply {
                moveTo(0f, h * .82f); lineTo(w * .13f, h * .72f); lineTo(w * .24f, h * .79f)
                lineTo(w * .37f, h * .70f); lineTo(w * .51f, h * .81f); lineTo(w * .68f, h * .73f)
                lineTo(w * .82f, h * .80f); lineTo(w, h * .69f); lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(far, Brush.verticalGradient(listOf(Color(0xFF282333), Color(0xFF09080E)), startY = h * .69f, endY = h))
            val near = Path().apply {
                moveTo(0f, h * .88f); lineTo(w * .10f, h * .77f); lineTo(w * .20f, h * .89f)
                lineTo(w * .32f, h * .82f); lineTo(w * .47f, h * .94f); lineTo(w * .62f, h * .84f)
                lineTo(w * .75f, h * .91f); lineTo(w * .90f, h * .79f); lineTo(w, h * .86f)
                lineTo(w, h); lineTo(0f, h); close()
            }
            drawPath(near, Brush.verticalGradient(listOf(Color(0xFF171321), Color(0xFF020204)), startY = h * .78f, endY = h))
            drawCircle(Color(0xFF9B55F5).copy(alpha = .10f), radius = w * .36f, center = androidx.compose.ui.geometry.Offset(w * .5f, h * .88f))
        }

        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1.1f))
            Canvas(Modifier.size(170.dp)) {
                val w = size.width
                val h = size.height
                val g = Path().apply {
                    moveTo(w * .76f, h * .23f)
                    lineTo(w * .34f, h * .23f)
                    cubicTo(w * .04f, h * .23f, w * .04f, h * .77f, w * .34f, h * .77f)
                    lineTo(w * .66f, h * .77f)
                    lineTo(w * .66f, h * .52f)
                    lineTo(w * .48f, h * .52f)
                }
                drawPath(g, Brush.linearGradient(listOf(Color.White, Color(0xFF8C8F9B), Color(0xFFE9EAF0))), style = Stroke(width = w * .12f, cap = StrokeCap.Square))
                val orbit = Path().apply {
                    moveTo(w * .05f, h * .68f)
                    cubicTo(w * .35f, h * .36f, w * .68f, h * .18f, w * .95f, h * .26f)
                    cubicTo(w * .78f, h * .57f, w * .38f, h * .84f, w * .13f, h * .83f)
                }
                drawPath(orbit, Color(0xFFB9B6C7), style = Stroke(width = w * .025f, cap = StrokeCap.Round))
                drawPath(orbit, Color(0xFF9B55F5).copy(alpha = .65f), style = Stroke(width = w * .008f))
            }
            Spacer(Modifier.height(10.dp))
            Text("G O D E L I N", color = launchSilver, fontSize = 19.sp, fontWeight = FontWeight.Light, letterSpacing = 5.sp)
            Spacer(Modifier.weight(.65f))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF17151E).copy(alpha = .82f))
                    .border(1.dp, Color(0xFF4B435A).copy(alpha = .72f), RoundedCornerShape(24.dp))
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LaunchShortcut("YouTube", Icons.Default.PlayArrow) { onNavigate("youtube.com") }
                LaunchShortcut("Discord", Icons.Default.ChatBubble) { onNavigate("discord.com") }
                LaunchShortcut("Roblox", Icons.Default.SportsEsports) { onNavigate("roblox.com") }
                LaunchShortcut("Add", Icons.Default.Add, onAddShortcut)
            }
            Spacer(Modifier.weight(1.35f))
        }
    }
}

@Composable
private fun LaunchShortcut(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(52.dp).clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF201C2B))
                .border(1.dp, Color(0xFF5A526B), RoundedCornerShape(15.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = if (label == "Add") launchPurple else launchSilver, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(7.dp))
        Text(label, color = launchSilver, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
