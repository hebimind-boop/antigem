package com.example.gemini.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Creates an animated linear gradient shimmer brush for skeleton loading states.
 */
@Composable
fun rememberShimmerBrush(
    shimmerColors: List<Color>? = null,
    durationMillis: Int = 1100
): Brush {
    val baseColors = shimmerColors ?: listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.20f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    )
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    return Brush.linearGradient(
        colors = baseColors,
        start = Offset(translateAnim - 400f, translateAnim - 400f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Reusable modifier extension to apply an animated shimmer background to any composable.
 */
@Composable
fun Modifier.shimmer(
    visible: Boolean = true,
    shape: Shape = RoundedCornerShape(4.dp),
    brush: Brush? = null
): Modifier {
    if (!visible) return this
    val shimmerBrush = brush ?: rememberShimmerBrush()
    return this
        .clip(shape)
        .background(shimmerBrush)
}

/**
 * Reusable shimmer placeholder box with rounded corners.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(6.dp),
    brush: Brush? = null
) {
    val b = brush ?: rememberShimmerBrush()
    Box(
        modifier = modifier
            .clip(shape)
            .background(b)
    )
}

/**
 * Reusable shimmer line for text placeholders.
 */
@Composable
fun ShimmerTextLine(
    modifier: Modifier = Modifier,
    widthFraction: Float = 1.0f,
    height: Dp = 13.dp,
    shape: Shape = RoundedCornerShape(4.dp),
    brush: Brush? = null
) {
    val b = brush ?: rememberShimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(shape)
            .background(b)
    )
}

/**
 * Reusable circular shimmer placeholder for avatar/icon loading states.
 */
@Composable
fun ShimmerCircle(
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    brush: Brush? = null
) {
    val b = brush ?: rememberShimmerBrush()
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(b)
    )
}
