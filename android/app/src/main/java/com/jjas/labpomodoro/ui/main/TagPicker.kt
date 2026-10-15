package com.jjas.labpomodoro.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.TagColors

/** Lo que se puede hacer con las etiquetas desde la pantalla principal. */
data class TagActions(
    val onSelect: (Long?) -> Unit = {},
    val onCreate: (String, Int) -> Unit = { _, _ -> },
    val onUpdate: (Long, String, Int) -> Unit = { _, _, _ -> },
    val onDelete: (Long) -> Unit = {},
    val onOpenPro: () -> Unit = {},
)

fun TagUi.swatch(): Color = Color(TagColors.of(color))

/**
 * Fila de etiquetas: "Sin etiqueta", las del usuario y "+ Etiqueta". Tocar elige; mantener
 * presionada una etiqueta la edita. Sin Pro, al llegar al límite "+" lleva a Pro.
 */
@Composable
fun TagChips(ui: TagsUi, selected: Long?, actions: TagActions, modifier: Modifier = Modifier) {
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TagChip(stringResource(R.string.tag_none), null, selected == null, onClick = { actions.onSelect(null) })
        ui.tags.forEach { tag ->
            TagChip(
                tag.name,
                tag.swatch(),
                selected == tag.id,
                onClick = { actions.onSelect(tag.id) },
                onLongClick = { editing = tag.id },
            )
        }
        TagChip(
            stringResource(if (ui.canAdd) R.string.tag_add else R.string.tag_add_pro),
            null,
            selected = false,
            onClick = { if (ui.canAdd) creating = true else actions.onOpenPro() },
        )
    }
    if (creating) {
        TagDialog(
            initial = null,
            // El siguiente color libre, para que cada etiqueta se distinga
            suggestedColor = ui.tags.size,
            onSave = { name, color -> actions.onCreate(name, color) },
            onDismiss = { creating = false },
        )
    }
    ui.find(editing)?.let { tag ->
        TagDialog(
            initial = tag,
            suggestedColor = tag.color,
            onSave = { name, color -> actions.onUpdate(tag.id, name, color) },
            onDelete = { actions.onDelete(tag.id) },
            onDismiss = { editing = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TagChip(label: String, color: Color?, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (selected) scheme.secondaryContainer else Color.Transparent,
        modifier = Modifier
            .border(1.dp, if (selected) Color.Transparent else scheme.outline, CircleShape)
            .clip(CircleShape)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            if (color != null) {
                Box(Modifier.size(9.dp).background(color, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) scheme.onSecondaryContainer else scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Crear o editar una etiqueta: nombre y color; al editar, también borrarla. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagDialog(
    initial: TagUi?,
    suggestedColor: Int,
    onSave: (String, Int) -> Unit,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name.orEmpty()) }
    var color by rememberSaveable { mutableIntStateOf(suggestedColor.mod(TagColors.all.size)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.tag_new_title else R.string.tag_edit_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(TagColors.MAX_NAME) },
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.tag_name_hint)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                // Se acomodan en dos filas si el diálogo es angosto
                FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp), maxItemsInEachRow = 4) {
                    TagColors.all.forEachIndexed { index, argb ->
                        val chosen = index == color
                        Box(
                            Modifier
                                .size(28.dp)
                                .background(Color(argb), CircleShape)
                                .border(if (chosen) 3.dp else 0.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                .clickable { color = index },
                        )
                    }
                }
                if (onDelete != null) {
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { onDelete(); onDismiss() }) {
                        Text(stringResource(R.string.tag_delete), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, color); onDismiss() }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.tag_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.tag_cancel)) } },
    )
}

/** La etiqueta del plan en curso, bajo el reloj; tocarla deja cambiarla. */
@Composable
fun CurrentTag(tag: TagUi?, onClick: () -> Unit) {
    Row(
        Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).background(tag?.swatch() ?: MaterialTheme.colorScheme.outline, CircleShape))
        Spacer(Modifier.width(6.dp))
        Text(
            tag?.name ?: stringResource(R.string.tag_none),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
