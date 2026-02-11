package com.example.background

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun CatRoomScreen(
    cats: List<CatItem>,
    pixelVm: PixelCatViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp.value * configuration.densityDpi / 160f

    // 📸 Graphics Layer for capturing the screenshot for sharing
    val graphicsLayer = rememberGraphicsLayer()

    val activeCats = remember(cats) {
        cats.filter { it.isInRoom && it.stickerPath != it.imagePath }
    }

    val prefs = context.getSharedPreferences("story_kitty_prefs", Context.MODE_PRIVATE)
    val isCreativeMode = prefs.getBoolean("creative_mode", false)
    val isMusicEnabled = prefs.getBoolean("music_enabled", true)

    val maxRooms = 5
    val unlockedCount = if (isCreativeMode) maxRooms else min(maxRooms, (cats.size / 6) + 1)

    val roomBackgrounds = listOf(
        R.drawable.livingroom, R.drawable.bedroom, R.drawable.garden, R.drawable.rooftop, R.drawable.basement
    )
    val roomNames = listOf("Living Room", "Bedroom", "Garden", "Rooftop", "Basement")

    val pagerState = rememberPagerState(pageCount = { maxRooms })
    var isUiVisible by remember { mutableStateOf(true) }
    var catWithDeleteOption by remember { mutableStateOf<CatItem?>(null) }
    var catToHide by remember { mutableStateOf<CatItem?>(null) }

    var isDraggingCat by remember { mutableStateOf(false) }
    var dragEdgeState by remember { mutableStateOf<DragEdge?>(null) }

    val mediaPlayer = remember {
        try { MediaPlayer.create(context, R.raw.cat_music)?.apply { isLooping = true } } catch (e: Exception) { null }
    }

    DisposableEffect(Unit) {
        mediaPlayer?.start()
        onDispose { mediaPlayer?.stop(); mediaPlayer?.release() }
    }
    LaunchedEffect(isMusicEnabled) {
        if (!isMusicEnabled) mediaPlayer?.setVolume(0f, 0f) else mediaPlayer?.setVolume(1f, 1f)
    }

    // MOVE TO STORAGE DIALOG
    if (catToHide != null) {
        Dialog(onDismissRequest = { catToHide = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = CozyCream),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.padding(16.dp).shadow(20.dp, RoundedCornerShape(28.dp))
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Move to Storage?", color = CozyBrown, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This cat will be removed from the room but kept in your residents list.", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        TextButton(onClick = { catToHide = null }) { Text("Cancel", color = CozyBrown) }
                        Button(
                            onClick = {
                                catToHide?.let { pixelVm.toggleCatRoomStatus(context, it, false) }
                                catToHide = null
                                catWithDeleteOption = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CozyCoral),
                            shape = RoundedCornerShape(50.dp)
                        ) { Text("Store", color = Color.White) }
                    }
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black).clickable {
            isUiVisible = !isUiVisible
            catWithDeleteOption = null
        }
    ) {
        Box(modifier = Modifier.fillMaxSize().drawWithContent {
            graphicsLayer.record {
                this@drawWithContent.drawContent()
            }
            drawLayer(graphicsLayer)
        }) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { pageIndex ->
                val roomCats = activeCats.filter { it.roomIndex == pageIndex }
                val bgImage = roomBackgrounds.getOrElse(pageIndex) { R.drawable.livingroom }
                val currentRoomName = roomNames.getOrElse(pageIndex) { "Room ${pageIndex + 1}" }

                val isLocked = (pageIndex + 1) > unlockedCount
                val totalRequired = pageIndex * 6
                val catsHave = cats.size
                val catsNeeded = (totalRequired - catsHave).coerceAtLeast(0)

                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = bgImage),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        colorFilter = if (isLocked) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f); setToScale(0.6f, 0.6f, 0.6f, 1f) }) else null
                    )

                    if (isLocked) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CozyCream),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.padding(32.dp).shadow(16.dp, RoundedCornerShape(24.dp))
                            ) {
                                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = CozyCoral, modifier = Modifier.size(56.dp).background(CozyPeach.copy(alpha = 0.3f), CircleShape).padding(12.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("$currentRoomName Locked", color = CozyBrown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                    Text("You need $catsNeeded more cat${if (catsNeeded != 1) "s" else ""} to unlock!", color = CozyBrown.copy(alpha = 0.7f), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    } else {
                        roomCats.forEach { cat ->
                            val defaultX = 200f
                            val defaultY = 600f
                            val initialPos = if (cat.posX == 0f && cat.posY == 0f) Offset(defaultX, defaultY) else Offset(cat.posX, cat.posY)

                            Box {
                                DraggableCatSticker(
                                    cat = cat,
                                    initialPosition = initialPos,
                                    isEditing = (catWithDeleteOption == cat),
                                    onDeleteClick = { catToHide = cat },
                                    onLongPress = { catWithDeleteOption = cat },
                                    onDragStart = {
                                        isDraggingCat = true
                                        catWithDeleteOption = null
                                    },
                                    onDrag = { offset ->
                                        dragEdgeState = when {
                                            offset.x > screenWidth - 150 -> DragEdge.RIGHT
                                            offset.x < 150 -> DragEdge.LEFT
                                            else -> null
                                        }
                                    },
                                    onDragEnd = { finalPos ->
                                        isDraggingCat = false
                                        if (dragEdgeState == DragEdge.RIGHT && pageIndex < maxRooms - 1) {
                                            if ((pageIndex + 1) < unlockedCount) {
                                                pixelVm.moveCatToRoom(context, cat, pageIndex + 1)
                                                scope.launch { pagerState.animateScrollToPage(pageIndex + 1) }
                                            } else {
                                                Toast.makeText(context, "That room is locked! 🔒", Toast.LENGTH_SHORT).show()
                                                pixelVm.saveCatPosition(context, cat, finalPos.x, finalPos.y)
                                            }
                                        } else if (dragEdgeState == DragEdge.LEFT && pageIndex > 0) {
                                            pixelVm.moveCatToRoom(context, cat, pageIndex - 1)
                                            scope.launch { pagerState.animateScrollToPage(pageIndex - 1) }
                                        } else {
                                            pixelVm.saveCatPosition(context, cat, finalPos.x, finalPos.y)
                                        }
                                        dragEdgeState = null
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // NAVIGATION INDICATORS (DURING DRAG)
        AnimatedVisibility(visible = isDraggingCat && dragEdgeState == DragEdge.RIGHT, modifier = Modifier.align(Alignment.CenterEnd), enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.width(100.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                val isNextLocked = (pagerState.currentPage + 1) >= unlockedCount
                Icon(imageVector = if (isNextLocked) Icons.Rounded.Lock else Icons.Filled.ArrowForward, contentDescription = null, tint = if (isNextLocked) Color.Red else Color.White, modifier = Modifier.size(48.dp))
            }
        }
        AnimatedVisibility(visible = isDraggingCat && dragEdgeState == DragEdge.LEFT, modifier = Modifier.align(Alignment.CenterStart), enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.width(100.dp).fillMaxHeight().background(Color.Black.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
        }

        // OVERLAY UI (VISIBLE TOGGLE)
        AnimatedVisibility(visible = isUiVisible, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {

                // HEADER PILL
                Column(modifier = Modifier.align(Alignment.TopCenter).padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        color = Color.Black.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)) {
                            Text(text = roomNames.getOrElse(pagerState.currentPage) { "Room" }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }

                // SHARE BUTTON
                Surface(
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp).clickable {
                        scope.launch {
                            val bitmap = graphicsLayer.toImageBitmap().asAndroidBitmap()
                            shareRoom(context, bitmap)
                        }
                    },
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = CircleShape,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.padding(10.dp).size(24.dp))
                }

                // PAGINATION DOTS
                Row(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repeat(maxRooms) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(targetValue = if (isSelected) 24.dp else 10.dp)
                        val color = if (isSelected) CozyBrown else Color.White.copy(alpha = 0.5f)
                        Box(modifier = Modifier.height(10.dp).width(width).clip(RoundedCornerShape(50)).background(color).clickable { scope.launch { pagerState.animateScrollToPage(index) } })
                    }
                }
            }
        }
    }
}

enum class DragEdge { LEFT, RIGHT }

@Composable
fun DraggableCatSticker(
    cat: CatItem,
    initialPosition: Offset,
    isEditing: Boolean,
    onDeleteClick: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (Offset) -> Unit,
    onLongPress: () -> Unit
) {
    var offset by remember { mutableStateOf(initialPosition) }

    Box(
        modifier = Modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onLongPress() })
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { onDragStart() },
                    onDragEnd = { onDragEnd(offset) }
                ) { change, dragAmount ->
                    change.consume()
                    offset += dragAmount
                    onDrag(offset)
                }
            }
            .size(128.dp)
    ) {
        val path = cat.stickerPath ?: cat.imagePath ?: ""

        // 1. The Cat Image
        AsyncImage(
            model = path,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 2. The Delete Button
        androidx.compose.animation.AnimatedVisibility(
            visible = isEditing,
            enter = scaleIn() + fadeIn(),
            exit = scaleOut() + fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                onClick = onDeleteClick,
                shape = CircleShape,
                color = CozyCoral,
                border = BorderStroke(2.dp, Color.White),
                shadowElevation = 4.dp,
                modifier = Modifier.offset(x = 8.dp, y = (-8).dp).size(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // 3. The Name Tag
        if (isEditing) {
            Text(
                text = cat.name,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CozyBrown,
                modifier = Modifier
                    // ✨ FIXED: Align to TopStart (Left) so it doesn't hit the X button
                    .align(Alignment.TopStart)
                    .offset(x = (-4).dp, y = (-12).dp)
                    .shadow(4.dp, RoundedCornerShape(50))
                    .background(CozyCream, shape = RoundedCornerShape(50))
                    .border(1.dp, Color.White, RoundedCornerShape(50))
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

fun shareRoom(context: Context, bitmap: Bitmap) {
    try {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val file = File(cachePath, "my_cat_room.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Room"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}