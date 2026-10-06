package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R

/**
 * La mascota: el koala del ícono, recortado en círculo. La imagen trae un marco verde cuadrado;
 * se amplía un poco para que el círculo oscuro llene el recorte y no se vean las esquinas.
 */
@Composable
fun KoalaAvatar(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Image(
        painterResource(R.drawable.koala_mascot),
        contentDescription = "Koala, la mascota de Lab Pomodoro",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .graphicsLayer {
                scaleX = KOALA_ZOOM
                scaleY = KOALA_ZOOM
            },
    )
}

private const val KOALA_ZOOM = 1.3f
