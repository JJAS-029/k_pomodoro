package com.jjas.labpomodoro.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.ui.components.KoalaAvatar

/** Se muestra una sola vez, cuando el usuario descubre los 118 elementos. */
@Composable
fun TableCompleteDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.el_table_complete_confirm)) } },
        title = {
            Text(
                stringResource(R.string.el_table_complete_title),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KoalaAvatar(size = 96.dp)
                Text(
                    stringResource(R.string.el_table_complete_body1, PeriodicTable.SIZE),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.el_table_complete_body2),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.el_table_complete_body3),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.el_table_complete_body4),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.el_signature),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        },
    )
}
