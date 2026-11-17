package com.jjas.labpomodoro.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R

/**
 * La mascota: el koala del ícono, recortado en círculo. La imagen es el dibujo sobre su fondo
 * azul marino, con margen para que las orejas no se corten.
 */
@Composable
fun KoalaAvatar(modifier: Modifier = Modifier, size: Dp = 120.dp) {
    Image(
        painterResource(R.drawable.koala_mascot),
        contentDescription = stringResource(R.string.main_koala_description),
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
    )
}
