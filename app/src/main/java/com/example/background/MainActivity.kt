package com.example.background

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
// ✨ FIX: This import allows .rotate() to work
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { StoryKittyApp() }
    }
}

@Composable
fun StoryKittyApp() {
    val context = LocalContext.current
    val selectedScreen = remember { mutableStateOf<Screen>(CameraScreen) }

    val db = remember { CatDatabase.getDatabase(context) }
    val catList by db.catDao().getAllCats().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val navItems = listOf(
        NavItem("Home", Icons.Filled.Home, Icons.Outlined.Home, CameraScreen),
        NavItem("Scrapbook", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder, CollectionScreen),
        NavItem("Room", Icons.Filled.Place, Icons.Outlined.Place, CatStoryScreen)
    )

    MaterialTheme {
        Scaffold(
            containerColor = CozyCream,
            bottomBar = {
                if (selectedScreen.value !is AddCatScreen && selectedScreen.value !is SettingsScreen) {
                    InstagramBottomBar(
                        items = navItems,
                        selected = selectedScreen.value,
                        onSelect = { selectedScreen.value = it }
                    )
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when (selectedScreen.value) {
                    is CameraScreen -> HomeScreen(
                        onNavigateToAddCat = { selectedScreen.value = AddCatScreen },
                        onNavigateToSettings = { selectedScreen.value = SettingsScreen }
                    )

                    is CollectionScreen -> CollectionScreen(catList)

                    // ✨ FIXED: This connects properly to the new AddCatScreen logic
                    is AddCatScreen -> AddCatScreen(
                        onCatSaved = {
                            selectedScreen.value = CollectionScreen
                        }
                    )

                    is CatStoryScreen -> CatRoomScreen(cats = catList)

                    is SettingsScreen -> SettingsScreen(
                        onBack = { selectedScreen.value = CameraScreen },
                        onDeleteAllCats = {
                            scope.launch(Dispatchers.IO) {
                                db.clearAllTables()
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun InstagramBottomBar(items: List<NavItem>, selected: Screen, onSelect: (Screen) -> Unit) {
    Column {
        Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
        NavigationBar(
            containerColor = Color.White,
            tonalElevation = 0.dp,
            modifier = Modifier.height(60.dp)
        ) {
            items.forEach { item ->
                val isSelected = item.screen == selected
                NavigationBarItem(
                    icon = { Icon(imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon, contentDescription = item.title, modifier = Modifier.size(28.dp)) },
                    selected = isSelected,
                    onClick = { onSelect(item.screen) },
                    colors = NavigationBarItemDefaults.colors(indicatorColor = Color.Transparent, selectedIconColor = Color.Black, unselectedIconColor = Color.Gray),
                    label = null
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onNavigateToAddCat: () -> Unit, onNavigateToSettings: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        IconButton(
            onClick = onNavigateToSettings,
            modifier = Modifier.align(Alignment.TopEnd).padding(24.dp).background(Color.White, CircleShape).shadow(4.dp, CircleShape)
        ) {
            Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = CozyBrown)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier.size(180.dp).shadow(12.dp, CircleShape).clip(CircleShape).background(Color.White)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text("Story Kitty 🐾", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = CozyBrown)
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onNavigateToAddCat,
                colors = ButtonDefaults.buttonColors(containerColor = CozyCoral),
                modifier = Modifier.size(220.dp, 70.dp).shadow(8.dp, RoundedCornerShape(20.dp)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text("📸  Rescue a Cat", fontSize = 18.sp, color = Color.White)
            }
        }
    }
}

@Composable
fun CollectionScreen(cats: List<CatItem>) {
    Box(modifier = Modifier.fillMaxSize().background(CozyCream)) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text(
                "My Scrapbook",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = CozyBrown,
                fontFamily = FontFamily.Cursive,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp).align(Alignment.CenterHorizontally)
            )

            if (cats.isEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Page is empty... \nGo rescue some cats! ✂️", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 4.dp, end = 4.dp)
                ) {
                    items(cats) { cat ->
                        ScrapbookItem(cat)
                    }
                }
            }
        }
    }
}

@Composable
fun ScrapbookItem(cat: CatItem) {
    // ✨ FIXED ROTATION: Explicitly convert Int to Float to avoid confusion
    val rotation = remember(cat.id) { (cat.id % 10).toFloat() - 5f }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .rotate(rotation) // This should work now
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(2.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.aspectRatio(1f).fillMaxWidth().background(Color.LightGray)) {
                    AsyncImage(
                        model = cat.imagePath,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = cat.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = CozyBrown,
                    fontFamily = FontFamily.Cursive
                )

                Text(
                    text = cat.breed ?: "Unknown Breed",
                    fontSize = 12.sp,
                    color = CozyCoral,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Place, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(10.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = cat.location,
                        fontSize = 10.sp,
                        color = Color.Gray,
                        maxLines = 1
                    )
                }
                Text(
                    text = cat.date,
                    fontSize = 10.sp,
                    color = Color.LightGray
                )
            }
        }

        // Washi Tape
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .width(40.dp)
                .height(12.dp)
                .background(CozyPeach.copy(alpha = 0.8f))
        )

        // Pixel Sticker Overlay
        if (cat.stickerPath != null) {
            AsyncImage(
                model = cat.stickerPath,
                contentDescription = "Sticker",
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 10.dp, y = 10.dp)
                    .size(48.dp)
                    .rotate(-10f)
                    .background(Color.White, CircleShape)
                    .padding(2.dp)
                    .shadow(2.dp, CircleShape)
            )
        }
    }
}