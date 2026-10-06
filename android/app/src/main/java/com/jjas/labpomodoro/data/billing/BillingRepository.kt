package com.jjas.labpomodoro.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.jjas.labpomodoro.core.di.ApplicationScope
import com.jjas.labpomodoro.data.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Una forma de comprar Pro, con el precio ya formateado por Google Play en la moneda local. */
data class ProOffer(
    val plan: ProPlan,
    val price: String,
    /** Precio en millonésimas de la moneda, para calcular el ahorro del plan anual. */
    val priceMicros: Long,
    internal val details: ProductDetails,
    internal val offerToken: String?,
)

enum class ProPlan { MONTHLY, YEARLY, LIFETIME }

sealed interface StoreState {
    data object Connecting : StoreState

    /** La tienda respondió pero no hay productos (por ejemplo, la app aún no está en Google Play). */
    data object NoProducts : StoreState

    data class Unavailable(val message: String) : StoreState

    data class Ready(val offers: List<ProOffer>) : StoreState
}

/**
 * Compras con Google Play. Pro se puede tener de dos formas:
 * - Suscripción [PRO_SUBSCRIPTION] con los planes base [BASE_PLAN_MONTHLY] y [BASE_PLAN_YEARLY].
 * - Pago único [PRO_LIFETIME].
 *
 * El resultado se guarda en DataStore para que Pro funcione sin conexión; solo se quita cuando
 * Google Play confirma que ya no hay compra vigente (suscripción cancelada y vencida o reembolso).
 */
@Singleton
class BillingRepository @Inject constructor(
    @ApplicationContext context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<StoreState>(StoreState.Connecting)
    val state: StateFlow<StoreState> = _state.asStateFlow()

    /** Mensaje para el usuario tras una compra (pendiente, error, gracias). */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val refreshLock = Mutex()

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingResponseCode.OK -> scope.launch { handlePurchases(purchases.orEmpty(), fromCheckout = true) }
            BillingResponseCode.USER_CANCELED -> Unit
            BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> _message.value = "No se pudo completar la compra (${result.debugMessage.ifBlank { result.responseCode.toString() }})."
        }
    }

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesListener)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** Se conecta y revisa compras; se llama al abrir la app. */
    fun start() {
        if (client.isReady) {
            refresh()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingResponseCode.OK) {
                    refresh()
                } else {
                    _state.value = StoreState.Unavailable(unavailableMessage(result))
                }
            }

            // Con enableAutoServiceReconnection la librería reconecta sola al siguiente uso
            override fun onBillingServiceDisconnected() = Unit
        })
    }

    /** Vuelve a consultar productos y compras (al abrir la pantalla Pro o "Restaurar compras"). */
    fun refresh() {
        scope.launch {
            refreshLock.withLock {
                if (!client.isReady) return@withLock
                // Un fallo de la tienda nunca debe cerrar la app
                runCatching {
                    loadOffers()
                    syncPurchases()
                }.onFailure { _state.value = StoreState.Unavailable("La tienda no respondió.") }
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    /** Abre la hoja de pago de Google Play. */
    fun purchase(activity: Activity, offer: ProOffer) {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(offer.details)
            .apply { offer.offerToken?.let(::setOfferToken) }
            .build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingResponseCode.OK) {
            _message.value = "No se pudo abrir el pago (${result.responseCode})."
        }
    }

    private suspend fun loadOffers() {
        // Google Play pide una consulta por tipo de producto
        val results = listOf(
            PRO_SUBSCRIPTION to ProductType.SUBS,
            PRO_LIFETIME to ProductType.INAPP,
        ).map { (id, type) ->
            client.queryProductDetails(
                QueryProductDetailsParams.newBuilder().setProductList(listOf(product(id, type))).build()
            )
        }
        results.firstOrNull { it.billingResult.responseCode != BillingResponseCode.OK }?.let {
            _state.value = StoreState.Unavailable(unavailableMessage(it.billingResult))
            return
        }
        val offers = results.flatMap { it.productDetailsList.orEmpty() }.flatMap(::toOffers).sortedBy { it.plan }
        _state.value = if (offers.isEmpty()) StoreState.NoProducts else StoreState.Ready(offers)
    }

    private fun toOffers(details: ProductDetails): List<ProOffer> = when (details.productId) {
        PRO_LIFETIME -> listOfNotNull(
            details.oneTimePurchaseOfferDetailsList?.firstOrNull()?.let {
                ProOffer(ProPlan.LIFETIME, it.formattedPrice, it.priceAmountMicros, details, it.offerToken)
            }
        )
        PRO_SUBSCRIPTION -> details.subscriptionOfferDetails.orEmpty()
            // Solo los planes base, sin ofertas especiales (offerId nulo)
            .filter { it.offerId == null }
            .mapNotNull { offer ->
                val plan = when (offer.basePlanId) {
                    BASE_PLAN_MONTHLY -> ProPlan.MONTHLY
                    BASE_PLAN_YEARLY -> ProPlan.YEARLY
                    else -> return@mapNotNull null
                }
                // La última fase es el precio normal (las anteriores serían pruebas gratis o descuentos)
                val phase = offer.pricingPhases.pricingPhaseList.lastOrNull() ?: return@mapNotNull null
                ProOffer(plan, phase.formattedPrice, phase.priceAmountMicros, details, offer.offerToken)
            }
        else -> emptyList()
    }

    /** Revisa lo que el usuario tiene comprado y actualiza Pro. */
    private suspend fun syncPurchases() {
        val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
        val inApp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        // Si alguna consulta falla no se toca nada: mejor no quitarle Pro a alguien por un error de red
        if (subs.billingResult.responseCode != BillingResponseCode.OK ||
            inApp.billingResult.responseCode != BillingResponseCode.OK
        ) return
        handlePurchases(subs.purchasesList + inApp.purchasesList, fromCheckout = false)
    }

    private suspend fun handlePurchases(purchases: List<Purchase>, fromCheckout: Boolean) {
        val ours = purchases.filter { p -> p.products.any { it == PRO_SUBSCRIPTION || it == PRO_LIFETIME } }
        val owned = ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        // Google reembolsa las compras que no se confirman en 3 días
        owned.filter { !it.isAcknowledged }.forEach { purchase ->
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
            )
        }
        when {
            owned.isNotEmpty() -> {
                settings.setProPurchased(true)
                if (fromCheckout) _message.value = "¡Gracias! Ya tienes Lab Pomodoro Pro."
            }
            ours.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                _message.value = "Tu pago está pendiente. Pro se activará en cuanto se confirme."
            // Desde el pago llegan solo las compras nuevas; la lista completa viene de syncPurchases
            !fromCheckout -> settings.setProPurchased(false)
        }
    }

    private fun product(id: String, type: String) =
        QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(type).build()

    private fun unavailableMessage(result: BillingResult) = when (result.responseCode) {
        BillingResponseCode.SERVICE_UNAVAILABLE, BillingResponseCode.NETWORK_ERROR -> "Sin conexión con Google Play. Revisa tu internet."
        // Pasa cuando no hay cuenta de Google Play o la app aún no está publicada
        BillingResponseCode.BILLING_UNAVAILABLE, BillingResponseCode.SERVICE_DISCONNECTED,
        BillingResponseCode.ITEM_UNAVAILABLE, BillingResponseCode.DEVELOPER_ERROR ->
            "La compra no está disponible por ahora. Revisa que tengas una cuenta de Google Play en el teléfono."
        else -> "La tienda no respondió (${result.responseCode})."
    }

    companion object {
        const val PRO_SUBSCRIPTION = "pro_subscription"
        const val BASE_PLAN_MONTHLY = "monthly"
        const val BASE_PLAN_YEARLY = "yearly"
        const val PRO_LIFETIME = "pro_lifetime"
    }
}
