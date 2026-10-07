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
import com.jjas.labpomodoro.domain.model.Mastery
import com.jjas.labpomodoro.ui.components.GoldColor
import com.jjas.labpomodoro.ui.components.KoalaAvatar
import com.jjas.labpomodoro.ui.components.metalColor

/** Una vez por nivel: cuando los 118 elementos llegan a bronce, a plata o a oro. */
@Composable
fun MasteryTableDialog(level: Mastery, onDismiss: () -> Unit) {
    val (title, body) = when (level) {
        Mastery.BRONZE -> R.string.el_mastery_table_bronze_title to listOf(
            R.string.el_mastery_table_bronze_body1,
            R.string.el_mastery_table_bronze_body2,
        )
        Mastery.SILVER -> R.string.el_mastery_table_silver_title to listOf(
            R.string.el_mastery_table_silver_body1,
            R.string.el_mastery_table_silver_body2,
        )
        else -> R.string.el_mastery_table_gold_title to listOf(
            R.string.el_mastery_table_gold_body1,
            R.string.el_mastery_table_gold_body2,
            R.string.el_mastery_table_gold_body3,
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.el_mastery_table_confirm)) } },
        title = {
            Text(
                stringResource(title),
                textAlign = TextAlign.Center,
                color = level.metalColor() ?: GoldColor,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KoalaAvatar(size = 88.dp)
                body.forEach {
                    Text(stringResource(it), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                }
                Text(stringResource(R.string.el_signature), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
        },
    )
}
