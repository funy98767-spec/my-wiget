package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.IndigoPrimary

@Composable
fun AuroraAmbientBackdrop(
    themeMode: String,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = when (themeMode) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }
    val transition = rememberInfiniteTransition(label = "aurora_drift")
    val drift by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "drift"
    )

    val bgBase = MaterialTheme.colorScheme.background
    val orb1 = if (isDark) IndigoPrimary.copy(alpha = 0.28f) else IndigoPrimary.copy(alpha = 0.14f)
    val orb2 = if (isDark) CyanAccent.copy(alpha = 0.22f) else CyanAccent.copy(alpha = 0.14f)
    val orb3 = if (isDark) Color(0xFFF43F5E).copy(alpha = 0.14f) else Color(0xFFF43F5E).copy(alpha = 0.08f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb1, Color.Transparent),
                    center = Offset(size.width * (0.2f + 0.12f * drift), size.height * 0.14f),
                    radius = size.width * 0.72f
                ),
                radius = size.width * 0.72f,
                center = Offset(size.width * (0.2f + 0.12f * drift), size.height * 0.14f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb2, Color.Transparent),
                    center = Offset(size.width * (0.85f - 0.15f * drift), size.height * 0.42f),
                    radius = size.width * 0.65f
                ),
                radius = size.width * 0.65f,
                center = Offset(size.width * (0.85f - 0.15f * drift), size.height * 0.42f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(orb3, Color.Transparent),
                    center = Offset(size.width * 0.35f, size.height * 0.85f),
                    radius = size.width * 0.58f
                ),
                radius = size.width * 0.58f,
                center = Offset(size.width * 0.35f, size.height * 0.85f)
            )
        }
        content()
    }
}

@Composable
fun GlassSurfaceCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    tonalAlpha: Float = 0.78f,
    borderColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = MaterialTheme.colorScheme.surface.copy(alpha = tonalAlpha),
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 2.dp
    ) {
        content()
    }
}
