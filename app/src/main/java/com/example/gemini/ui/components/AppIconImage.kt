package com.example.gemini.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.gemini.R

/**
 * Official LimitX app brand logo, rendered cleanly and crisply at any size.
 */
@Composable
fun AppIconImage(contentDescription: String?, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(id = R.drawable.limitx_logo),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
    )
}
