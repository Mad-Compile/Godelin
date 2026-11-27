package com.godelin.browser.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.godelin.browser.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val COSMETICS_PREFERENCES = "godelin_cosmetics"

data class CosmeticsSettings(
    val wallpaperId: String = "aurora",
    val accentId: String = "mint",
    val wallpaperUri: String? = null
)

private data class WallpaperChoice(val id: String, val title: String, val detail: String)
private data class AccentChoice(val id: String, val title: String, val color: Color)

private val wallpaperChoices = listOf(
    WallpaperChoice("dragon", "Dragon", "Ancient guardian"),
    WallpaperChoice("ronin", "Ronin", "Mountain warrior"),
    WallpaperChoice("redline", "Redline", "Midnight drive"),
    WallpaperChoice("saturn", "Saturn", "Ringed world")
)

private val accentChoices = listOf(
    AccentChoice("mint", "Mint", Color(0xFF9DE4D2)),
    AccentChoice("cobalt", "Cobalt", Color(0xFF91A0FF)),
    AccentChoice("coral", "Coral", Color(0xFFFFA6A0)),
    AccentChoice("amber", "Amber", Color(0xFFFFC878))
)

fun cosmeticsAccentColor(accentId: String): Color =
    accentChoices.firstOrNull { it.id == accentId }?.color ?: accentChoices.first().color

fun loadCosmetics(context: Context): CosmeticsSettings {
    val preferences = context.getSharedPreferences(COSMETICS_PREFERENCES, Context.MODE_PRIVATE)
    return CosmeticsSettings(
        wallpaperId = preferences.getString("wallpaper_id", "aurora") ?: "aurora",
        accentId = preferences.getString("accent_id", "mint") ?: "mint",
        wallpaperUri = preferences.getString("wallpaper_uri", null)
    )
}

fun saveCosmetics(context: Context, settings: CosmeticsSettings) {
    context.getSharedPreferences(COSMETICS_PREFERENCES, Context.MODE_PRIVATE).edit().apply {
        putString("wallpaper_id", settings.wallpaperId)
        putString("accent_id", settings.accentId)
        if (settings.wallpaperUri == null) remove("wallpaper_uri")
        else putString("wallpaper_uri", settings.wallpaperUri)
    }.apply()
}

@Composable
fun rememberWallpaperBitmap(uriString: String?): ImageBitmap? {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uriString) {
        value = if (uriString.isNullOrBlank()) null else withContext(Dispatchers.IO) {
            runCatching { decodeWallpaper(context, uriString) }.getOrNull()
        }
    }
    return bitmap?.asImageBitmap()
}

private fun decodeWallpaper(context: Context, uriString: String): Bitmap? {
    val uri = Uri.parse(uriString)
    val resolver = context.contentResolver
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = resolver.openInputStream(uri) ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 1920 || bounds.outHeight / sampleSize > 1920) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

@Composable
fun WallpaperArtwork(
    modifier: Modifier,
    wallpaperId: String,
    accentId: String,
    customBitmap: ImageBitmap? = null,
    dim: Float = 0f
) {
    val drawableId = when (wallpaperId) {
        "ronin" -> R.drawable.wallpaper_ronin
        "redline" -> R.drawable.wallpaper_redline
        "saturn" -> R.drawable.wallpaper_saturn
        else -> R.drawable.wallpaper_dragon
    }
    Box(modifier) {
        if (wallpaperId == "custom" && customBitmap != null) {
            Image(
                bitmap = customBitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Image(
                painter = painterResource(id = drawableId),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (dim > 0f) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim.coerceIn(0f, 1f)))
            )
        }
    }
}

@Composable
fun CosmeticsScreen(
    settings: CosmeticsSettings,
    onApply: (CosmeticsSettings) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var draft by remember(settings) { mutableStateOf(settings) }
    val previewBitmap = rememberWallpaperBitmap(draft.wallpaperUri)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            draft = draft.copy(wallpaperId = "custom", wallpaperUri = uri.toString())
        }
    }
    val selectedAccent = cosmeticsAccentColor(draft.accentId)

    Column(
        Modifier.fillMaxSize().background(Color(0xFF0B100F))
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = 18.dp)
    ) {
        Row(
            Modifier.fillMaxWidth().height(62.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss, modifier = Modifier.size(46.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Close cosmetics", tint = Color(0xFFE9F0ED))
            }
            Spacer(Modifier.width(4.dp))
            Text("Cosmetics", color = Color(0xFFF1F5F3), fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Text("GODELIN", color = Color(0xFF87958F), fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Make it yours.", color = Color(0xFFF2F6F4), fontSize = 29.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.8).sp)
            Spacer(Modifier.height(5.dp))
            Text("Set the atmosphere for every new tab.", color = Color(0xFF9AA8A2), fontSize = 13.sp)
            Spacer(Modifier.height(18.dp))

            Box(
                Modifier.fillMaxWidth().height(214.dp).clip(RoundedCornerShape(27.dp))
            ) {
                WallpaperArtwork(
                    modifier = Modifier.fillMaxSize(),
                    wallpaperId = draft.wallpaperId,
                    accentId = draft.accentId,
                    customBitmap = previewBitmap
                )
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.10f), Color.Black.copy(alpha = 0.58f)))
                    )
                )
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = selectedAccent, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(7.dp))
                        Text("NEW TAB PREVIEW", color = Color.White.copy(alpha = 0.88f), fontSize = 10.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        Text("A little more you.", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.4).sp)
                        Spacer(Modifier.height(9.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.91f))
                                .padding(horizontal = 13.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38433E), modifier = Modifier.size(17.dp))
                            Spacer(Modifier.width(9.dp))
                            Text("Search or enter a website", color = Color(0xFF53615B), fontSize = 12.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(23.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Backgrounds", color = Color(0xFFF0F4F2), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(if (draft.wallpaperId == "custom") "PERSONAL PHOTO" else "BUILT IN", color = selectedAccent, fontSize = 9.sp, letterSpacing = 1.25.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(11.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 3.dp)
            ) {
                items(wallpaperChoices, key = { it.id }) { choice ->
                    val selected = draft.wallpaperId == choice.id
                    Column(
                        Modifier.width(116.dp).clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF171E1C))
                            .border(
                                if (selected) 1.5.dp else 1.dp,
                                if (selected) selectedAccent else Color(0xFF303A35),
                                RoundedCornerShape(18.dp)
                            )
                            .selectable(
                                selected = selected,
                                onClick = { draft = draft.copy(wallpaperId = choice.id, wallpaperUri = null) },
                                role = Role.RadioButton
                            )
                            .padding(7.dp)
                    ) {
                        Box(Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(12.dp))) {
                            WallpaperArtwork(
                                modifier = Modifier.fillMaxSize(),
                                wallpaperId = choice.id,
                                accentId = draft.accentId
                            )
                            if (selected) {
                                Box(Modifier.align(Alignment.TopEnd).padding(6.dp).size(21.dp).clip(CircleShape).background(selectedAccent), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF14201C), modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(choice.title, color = Color(0xFFF0F4F2), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(choice.detail, color = Color(0xFF8D9A94), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(21.dp))
            Text("Accent colour", color = Color(0xFFF0F4F2), fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text("A small detail for your new tab.", color = Color(0xFF8D9A94), fontSize = 12.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                accentChoices.forEach { choice ->
                    val selected = draft.accentId == choice.id
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(15.dp))
                            .background(if (selected) Color(0xFF202A26) else Color(0xFF141A18))
                            .border(1.dp, if (selected) choice.color.copy(alpha = 0.85f) else Color(0xFF29332F), RoundedCornerShape(15.dp))
                            .selectable(selected = selected, onClick = { draft = draft.copy(accentId = choice.id) }, role = Role.RadioButton)
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(25.dp).clip(CircleShape).background(choice.color)
                                .border(if (selected) 2.dp else 0.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(choice.title, color = if (selected) Color.White else Color(0xFF9AA8A2), fontSize = 10.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                    }
                }
            }

            Spacer(Modifier.height(17.dp))
            OutlinedButton(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(15.dp)
            ) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (draft.wallpaperId == "custom") "Change personal photo" else "Choose a photo from your device")
            }
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { onApply(draft) },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp).height(53.dp),
            shape = RoundedCornerShape(17.dp),
            colors = ButtonDefaults.buttonColors(containerColor = selectedAccent, contentColor = Color(0xFF14201C))
        ) {
            Text("Apply to Godelin", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
