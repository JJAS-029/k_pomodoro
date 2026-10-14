package com.jjas.labpomodoro.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.ui.components.GlassLook
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.LiquidPalette
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.VesselView
import com.jjas.labpomodoro.ui.theme.LabPomodoroTheme

/**
 * Compara los estilos de vidrio con las seis formas. Extra "light" = true: tema claro;
 * "big" = true: solo Liquid Glass, tres por fila y más altos, para revisar bocas y bordes.
 */
class GlassPreviewActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val light = intent.getBooleanExtra("light", false)
        val big = intent.getBooleanExtra("big", false)
        val looks = if (big) listOf(GlassLook.LIQUID_GLASS) else GlassLook.entries
        val perRow = if (big) 3 else VesselShape.entries.size
        setContent {
            LabPomodoroTheme(darkTheme = !light) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    Column(Modifier.safeDrawingPadding().padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        looks.forEach { look ->
                            Text(look.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
                            VesselShape.entries.chunked(perRow).forEachIndexed { row, shapes ->
                            Row(Modifier.fillMaxWidth().height(if (big) 330.dp else 170.dp)) {
                                shapes.forEachIndexed { j, shape ->
                                    val i = row * perRow + j
                                    VesselView(
                                        shape = shape,
                                        fill = 0.6f,
                                        liquidColor = LiquidPalette.liquid(SessionType.WORK, 3, i),
                                        bubbleColor = LiquidPalette.bubble(SessionType.WORK, 3, i),
                                        effect = LiquidEffect.NONE,
                                        animate = false,
                                        look = look,
                                        modifier = Modifier.weight(1f).fillMaxSize(),
                                    )
                                }
                            }
                            }
                        }
                    }
                }
            }
        }
    }
}
