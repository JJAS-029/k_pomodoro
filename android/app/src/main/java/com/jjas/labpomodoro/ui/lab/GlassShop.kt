package com.jjas.labpomodoro.ui.lab

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.domain.model.BurnerStyle
import com.jjas.labpomodoro.ui.components.BunsenBurner
import com.jjas.labpomodoro.ui.components.LiquidEffect
import com.jjas.labpomodoro.ui.components.VesselShape
import com.jjas.labpomodoro.ui.components.VesselView

/**
 * Lo que vende la tienda de cristalería, con su precio en elementos repetidos. [key] es el nombre
 * del recipiente o del mechero, como se guarda en los ajustes.
 */
enum class ShopItem(
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    val price: Int,
    val shape: VesselShape? = null,
    val burner: BurnerStyle? = null,
) {
    CRUCIBLE(R.string.shop_crucible, R.string.shop_crucible_desc, 10, shape = VesselShape.CRUCIBLE),
    CRYSTALLIZER(R.string.shop_crystallizer, R.string.shop_crystallizer_desc, 12, shape = VesselShape.CRYSTALLIZER),
    KITASATO(R.string.shop_kitasato, R.string.shop_kitasato_desc, 15, shape = VesselShape.KITASATO),
    DISTILLATION_FLASK(R.string.shop_distillation, R.string.shop_distillation_desc, 20, shape = VesselShape.DISTILLATION_FLASK),
    SEPARATORY_FUNNEL(R.string.shop_separatory, R.string.shop_separatory_desc, 25, shape = VesselShape.SEPARATORY_FUNNEL),
    ALCOHOL_LAMP(R.string.shop_burner_alcohol, R.string.shop_burner_alcohol_desc, 15, burner = BurnerStyle.ALCOHOL_LAMP),
    MEKER(R.string.shop_burner_meker, R.string.shop_burner_meker_desc, 20, burner = BurnerStyle.MEKER),
    HOT_PLATE(R.string.shop_burner_hot_plate, R.string.shop_burner_hot_plate_desc, 30, burner = BurnerStyle.HOT_PLATE),
    ;

    val key: String get() = shape?.name ?: burner!!.name

    companion object {
        val vessels = entries.filter { it.shape != null }
        val burners = entries.filter { it.burner != null }
    }
}

/**
 * Pestaña Cristalería del laboratorio: cambia elementos repetidos por recipientes nuevos (entran
 * en la rotación) y mecheros (se elige cuál usar).
 */
@Composable
fun GlassShop(
    state: LabUi,
    onBuy: (ShopItem) -> Unit,
    onUseBurner: (BurnerStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by rememberSaveable { mutableStateOf<ShopItem?>(null) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.shop_intro), style = MaterialTheme.typography.bodyMedium)
        Text(
            pluralStringResource(R.plurals.shop_repeats, state.repeats, state.repeats),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        SectionHeader(R.string.shop_section_vessels, R.string.shop_section_vessels_desc)
        ShopItem.vessels.forEachIndexed { i, item ->
            val owned = item.key in state.ownedGlassware
            ShopRow(
                item = item,
                preview = {
                    VesselView(
                        shape = item.shape!!,
                        fill = 0.6f,
                        liquidColor = Color.hsl(PREVIEW_HUES[i % PREVIEW_HUES.size], 0.8f, 0.6f),
                        bubbleColor = Color.Transparent,
                        effect = LiquidEffect.NONE,
                        animate = false,
                        modifier = Modifier.size(width = 64.dp, height = 72.dp),
                    )
                },
            ) {
                if (owned) {
                    OwnedLabel(R.string.shop_owned)
                } else {
                    Button(onClick = { confirm = item }, enabled = state.repeats >= item.price) {
                        Text(stringResource(R.string.shop_buy, item.price))
                    }
                }
            }
        }

        SectionHeader(R.string.shop_section_burners, R.string.shop_section_burners_desc)
        // El Bunsen viene de serie, primero en la lista
        BurnerRow(
            nameRes = R.string.shop_burner_bunsen,
            descriptionRes = R.string.shop_burner_bunsen_desc,
            style = BurnerStyle.BUNSEN,
            inUse = state.burner == BurnerStyle.BUNSEN,
            owned = true,
            price = 0,
            canAfford = true,
            onBuy = {},
            onUse = { onUseBurner(BurnerStyle.BUNSEN) },
        )
        ShopItem.burners.forEach { item ->
            BurnerRow(
                nameRes = item.nameRes,
                descriptionRes = item.descriptionRes,
                style = item.burner!!,
                inUse = state.burner == item.burner,
                owned = item.key in state.ownedGlassware,
                price = item.price,
                canAfford = state.repeats >= item.price,
                onBuy = { confirm = item },
                onUse = { onUseBurner(item.burner) },
            )
        }
    }

    confirm?.let { item ->
        val name = stringResource(item.nameRes)
        AlertDialog(
            onDismissRequest = { confirm = null },
            title = { Text(stringResource(R.string.shop_confirm_title, name)) },
            text = { Text(pluralStringResource(R.plurals.shop_confirm_text, item.price, item.price)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    onBuy(item)
                }) { Text(stringResource(R.string.shop_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.shop_cancel)) } },
        )
    }
}

@Composable
private fun SectionHeader(@StringRes title: Int, @StringRes description: Int) {
    Column(Modifier.padding(top = 8.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BurnerRow(
    @StringRes nameRes: Int,
    @StringRes descriptionRes: Int,
    style: BurnerStyle,
    inUse: Boolean,
    owned: Boolean,
    price: Int,
    canAfford: Boolean,
    onBuy: () -> Unit,
    onUse: () -> Unit,
) {
    ShopCard(
        nameRes = nameRes,
        descriptionRes = descriptionRes,
        preview = {
            // Encendido, para que se vea la llama (o la placa al rojo)
            Box(Modifier.size(width = 64.dp, height = 72.dp), contentAlignment = Alignment.BottomCenter) {
                BunsenBurner(lit = true, style = style, modifier = Modifier.size(width = 64.dp, height = 40.dp))
            }
        },
    ) {
        when {
            inUse -> OwnedLabel(R.string.shop_in_use)
            owned -> OutlinedButton(onClick = onUse) { Text(stringResource(R.string.shop_use)) }
            else -> Button(onClick = onBuy, enabled = canAfford) { Text(stringResource(R.string.shop_buy, price)) }
        }
    }
}

@Composable
private fun ShopRow(item: ShopItem, preview: @Composable () -> Unit, action: @Composable () -> Unit) =
    ShopCard(item.nameRes, item.descriptionRes, preview, action)

@Composable
private fun ShopCard(
    @StringRes nameRes: Int,
    @StringRes descriptionRes: Int,
    preview: @Composable () -> Unit,
    action: @Composable () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            preview()
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(nameRes), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                action()
            }
        }
    }
}

@Composable
private fun OwnedLabel(@StringRes text: Int) {
    Text("✓ " + stringResource(text), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
}

/** Un tono distinto para cada recipiente de muestra. */
private val PREVIEW_HUES = listOf(190f, 280f, 35f, 140f, 330f)
