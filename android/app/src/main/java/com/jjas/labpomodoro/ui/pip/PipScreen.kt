package com.jjas.labpomodoro.ui.pip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.service.formatMinutesSeconds
import com.jjas.labpomodoro.ui.main.TimerUi
import com.jjas.labpomodoro.ui.main.TimerViewModel
import com.jjas.labpomodoro.ui.main.Vessel
import com.jjas.labpomodoro.ui.main.VesselUi
import com.jjas.labpomodoro.ui.theme.JetBrainsMono

/** Ventana flotante (PiP): solo el recipiente y el tiempo, reutilizando el mismo VesselView. */
@Composable
fun PipScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val timer = state.timer

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Vessel(
                vessel = (timer as? TimerUi.Active)?.vessel ?: VesselUi.Resting,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            Text(
                text = when (timer) {
                    is TimerUi.Active -> formatMinutesSeconds(timer.remainingMillis)
                    is TimerUi.Finished -> stringResource(R.string.main_pip_done)
                    else -> ""
                },
                color = (timer as? TimerUi.Active)?.vessel?.liquid ?: MaterialTheme.colorScheme.primary,
                fontFamily = JetBrainsMono,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}
