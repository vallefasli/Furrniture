package com.example.background

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun CatStoryScreenPlaceholder(modifier: Modifier = Modifier) {
    // UPDATED: Using the new "Cozy" colors
    val radialBrush = Brush.radialGradient(
        colors = listOf(Color.White, CozyCream, CozyPeach),
        radius = 800f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = radialBrush),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Cat Room Coming Soon! 🐾",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = CozyBrown
        )
    }
}