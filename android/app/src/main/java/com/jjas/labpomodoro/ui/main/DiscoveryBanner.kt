package com.jjas.labpomodoro.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.Discovery
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.domain.model.DiscoverySource
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.components.localizedName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoveryViewModel @Inject constructor(private val lab: LabRepository) : ViewModel() {

    /** Recompensas que aún no se han visto (las de la fusión se ven en el momento). */
    val unseen: StateFlow<List<Discovery>> = lab.unseen
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun dismiss() {
        viewModelScope.launch { lab.markAllSeen() }
    }
}

/** Aviso de elementos nuevos ganados con el tiempo de enfoque. */
@Composable
fun DiscoveryBanner(
    discoveries: List<Discovery>,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rewards = discoveries.filter { it.source != DiscoverySource.FUSION }
    if (rewards.isEmpty()) return
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 8.dp,
    ) {
        Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Text(
                text = if (rewards.size == 1) {
                    val locale = LocalConfiguration.current.locales[0]
                    stringResource(R.string.el_banner_discovered, rewards[0].element.localizedName().lowercase(locale))
                } else {
                    pluralStringResource(R.plurals.el_banner_new_elements, rewards.size, rewards.size)
                },
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
                    rewards.take(MAX_TILES).forEach { ElementTile(it.element, discovered = true, size = 40.dp) }
                    if (rewards.size > MAX_TILES) {
                        Text(
                            "+${rewards.size - MAX_TILES}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterVertically),
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.el_banner_close)) }
                TextButton(onClick = onOpen) { Text(stringResource(R.string.el_banner_view)) }
            }
        }
    }
}

private const val MAX_TILES = 4
