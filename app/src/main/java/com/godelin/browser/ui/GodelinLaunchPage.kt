package com.godelin.browser.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val launchBg = Color(0xFF050608)
private val launchPanel = Color(0xFF111419)
private val launchMint = Color(0xFFD6EAE5)
private val launchText = Color(0xFFF4F6F5)
private val launchMuted = Color(0xFF9AA5A2)
private val launchBlue = Color(0xFF7185F5)
private val launchPink = Color(0xFFFFA6C5)

@Composable
fun GodelinLaunchPage(
    onNavigate: (String) -> Unit,
    onAddShortcut: () -> Unit,
    onSearch: () -> Unit,
    wallpaperId: String = "aurora",
    accentId: String = "mint",
    wallpaperBitmap: ImageBitmap? = null
) {
    var showQuickPanel by remember { mutableStateOf(true) }
    val accentColor = cosmeticsAccentColor(accentId)
    Box(Modifier.fillMaxSize().background(launchBg)) {
        WallpaperArtwork(
            modifier = Modifier.fillMaxSize(),
            wallpaperId = wallpaperId,
            accentId = accentId,
            customBitmap = wallpaperBitmap,
            dim = 0.68f
        )
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=20.dp).padding(top=16.dp,bottom=24.dp)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Row(Modifier.weight(1f),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield,null,tint=accentColor,modifier=Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Privacy overview",color=launchText,fontSize=14.sp,fontWeight=FontWeight.SemiBold)
                }
                Text("GODELIN",color=launchMuted,fontSize=11.sp,letterSpacing=2.4.sp,fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                PrivacyMetric("Protection","Standard","No tracker blocking",Modifier.weight(1f),accentColor)
                PrivacyMetric("Data saved","—","Not measured",Modifier.weight(1f))
                PrivacyMetric("Time saved","—","Not measured",Modifier.weight(1f))
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                Text("Quick access",color=launchText,fontSize=20.sp,fontWeight=FontWeight.Bold,modifier=Modifier.weight(1f))
                Text("EDIT",color=accentColor,fontSize=11.sp,letterSpacing=1.2.sp,modifier=Modifier.clip(RoundedCornerShape(10.dp)).clickable(onClick=onAddShortcut).padding(10.dp))
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Shortcut("Add new",Icons.Default.Add,accentColor,Modifier.weight(1f),onAddShortcut)
                Shortcut("YouTube",Icons.Default.PlayArrow,launchPink,Modifier.weight(1f)){onNavigate("youtube.com")}
                Shortcut("Wikipedia",Icons.Default.Language,launchBlue,Modifier.weight(1f)){onNavigate("wikipedia.org")}
                Shortcut("Bookmarks",Icons.Default.BookmarkBorder,accentColor,Modifier.weight(1f)){onNavigate("https://www.google.com")}
            }
            Spacer(Modifier.height(24.dp))
            Text("Your browser,\nwithout the noise.",color=launchText,fontSize=29.sp,lineHeight=35.sp,fontWeight=FontWeight.Bold,letterSpacing=(-1).sp)
            Spacer(Modifier.height(7.dp))
            Text("A calmer place to explore the web.",color=launchMuted,fontSize=14.sp)
            Spacer(Modifier.height(15.dp))
            Box(Modifier.fillMaxWidth().height(270.dp).clip(RoundedCornerShape(26.dp)).background(Color(0xFF11151D)).border(1.dp,Color(0xFF303746),RoundedCornerShape(26.dp))) {
                Canvas(Modifier.fillMaxSize()) {
                    val w=size.width; val h=size.height
                    drawCircle(Brush.radialGradient(listOf(Color(0x557185F5),Color.Transparent),center=Offset(w*.5f,h*.5f),radius=w*.52f),w*.52f,Offset(w*.5f,h*.5f))
                    val shape=Path().apply{moveTo(w*.5f,h*.14f);lineTo(w*.78f,h*.28f);lineTo(w*.7f,h*.5f);lineTo(w*.82f,h*.76f);lineTo(w*.5f,h*.88f);lineTo(w*.18f,h*.76f);lineTo(w*.3f,h*.5f);lineTo(w*.22f,h*.28f);close()}
                    drawPath(shape,Color(0xFF7185F5).copy(alpha=.14f))
                    drawCircle(Color(0xFF9EADFF).copy(alpha=.45f),w*.19f,Offset(w*.5f,h*.5f),style=Stroke(width=2.dp.toPx()))
                    drawCircle(Color(0xFF9EADFF).copy(alpha=.22f),w*.26f,Offset(w*.5f,h*.5f),style=Stroke(width=1.dp.toPx()))
                    drawLine(Color(0xFF7185F5).copy(alpha=.6f),Offset(w*.5f,h*.2f),Offset(w*.5f,h*.8f),1.dp.toPx(),cap=StrokeCap.Round)
                    drawLine(Color(0xFF7185F5).copy(alpha=.6f),Offset(w*.2f,h*.5f),Offset(w*.8f,h*.5f),1.dp.toPx(),cap=StrokeCap.Round)
                }
                Column(Modifier.align(Alignment.Center),horizontalAlignment=Alignment.CenterHorizontally) {
                    GodelinMark(Modifier.size(82.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("GODELIN",color=launchText,fontSize=18.sp,fontWeight=FontWeight.Light,letterSpacing=5.sp)
                    Text("Explore at your own pace",color=launchMuted,fontSize=12.sp)
                }
                Row(Modifier.align(Alignment.TopStart).padding(14.dp).clip(CircleShape).background(Color(0xFF202938)).padding(horizontal=10.dp,vertical=7.dp),verticalAlignment=Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield,null,tint=accentColor,modifier=Modifier.size(14.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("YOUR SPACE",color=accentColor,fontSize=10.sp,letterSpacing=1.sp,fontWeight=FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))
            AnimatedVisibility(showQuickPanel,enter=fadeIn(),exit=fadeOut()) {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(25.dp)).background(Color(0xFFF7FAF9)).padding(15.dp)) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color(0xFFEAF0EE)).clickable(onClick=onSearch).padding(horizontal=13.dp,vertical=15.dp),verticalAlignment=Alignment.CenterVertically) {
                        Icon(Icons.Default.Search,null,tint=Color(0xFF26312E))
                        Spacer(Modifier.width(10.dp))
                        Text("Search Godelin or enter a website",color=Color(0xFF596661),fontSize=13.sp,modifier=Modifier.weight(1f),maxLines=1,overflow=TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(13.dp))
                    Text("Ready for your next discovery?",color=Color(0xFF202624),fontSize=16.sp,fontWeight=FontWeight.Bold)
                    Text("Search the web, revisit a page, or open a shortcut.",color=Color(0xFF596661),fontSize=13.sp,lineHeight=18.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.End,verticalAlignment=Alignment.CenterVertically) {
                        Text("HIDE",color=Color(0xFF596661),fontSize=12.sp,fontWeight=FontWeight.SemiBold,modifier=Modifier.clip(RoundedCornerShape(12.dp)).clickable{showQuickPanel=false}.padding(11.dp))
                        Row(Modifier.clip(RoundedCornerShape(22.dp)).background(accentColor).clickable(onClick=onSearch).padding(horizontal=17.dp,vertical=11.dp),verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.Search,null,tint=Color.White,modifier=Modifier.size(17.dp));Spacer(Modifier.width(6.dp))
                            Text("Start searching",color=Color.White,fontSize=13.sp,fontWeight=FontWeight.SemiBold)
                        }
                    }
                }
            }
            if(!showQuickPanel) Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(launchPanel).clickable{showQuickPanel=true}.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.History,null,tint=launchMint);Spacer(Modifier.width(9.dp));Text("Show quick search",color=launchText,fontWeight=FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun PrivacyMetric(title:String,value:String,detail:String,modifier:Modifier=Modifier,highlightColor:Color=launchMint) {
    Column(modifier.padding(vertical=10.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Text(value,color=if(title=="Protection")highlightColor else launchBlue,fontSize=18.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
        Text(title,color=launchText,fontSize=11.sp,fontWeight=FontWeight.Medium,maxLines=1)
        Text(detail,color=launchMuted,fontSize=9.sp,maxLines=1)
    }
}

@Composable
private fun Shortcut(label:String,icon:androidx.compose.ui.graphics.vector.ImageVector,tint:Color,modifier:Modifier=Modifier,onClick:()->Unit) {
    Column(modifier.clip(RoundedCornerShape(17.dp)).clickable(onClick=onClick).padding(vertical=7.dp),horizontalAlignment=Alignment.CenterHorizontally) {
        Box(Modifier.size(48.dp).clip(RoundedCornerShape(15.dp)).background(Color(0xFF171D27)).border(1.dp,Color(0xFF303A4B),RoundedCornerShape(15.dp)),contentAlignment=Alignment.Center) {
            Icon(icon,contentDescription=label,tint=tint,modifier=Modifier.size(22.dp))
        }
        Spacer(Modifier.height(6.dp));Text(label,color=launchText,fontSize=10.sp,maxLines=1,overflow=TextOverflow.Ellipsis)
    }
}

@Composable
private fun GodelinMark(modifier:Modifier=Modifier) {
    Canvas(modifier) {
        val w=size.width;val h=size.height
        val shield=Path().apply{moveTo(w*.5f,h*.04f);cubicTo(w*.72f,h*.04f,w*.94f,h*.22f,w*.92f,h*.48f);cubicTo(w*.89f,h*.73f,w*.68f,h*.91f,w*.5f,h*.98f);cubicTo(w*.32f,h*.91f,w*.11f,h*.73f,w*.08f,h*.48f);cubicTo(w*.06f,h*.22f,w*.28f,h*.04f,w*.5f,h*.04f);close()}
        drawPath(shield,Brush.linearGradient(listOf(Color(0xFFB8C4FF),Color(0xFF596FD8))))
        val cut=Path().apply{moveTo(w*.27f,h*.33f);lineTo(w*.43f,h*.36f);lineTo(w*.37f,h*.52f);lineTo(w*.5f,h*.62f);lineTo(w*.63f,h*.52f);lineTo(w*.57f,h*.36f);lineTo(w*.73f,h*.33f);lineTo(w*.65f,h*.68f);lineTo(w*.5f,h*.79f);lineTo(w*.35f,h*.68f);close()}
        drawPath(cut,Color(0xFF18233A))
    }
}
