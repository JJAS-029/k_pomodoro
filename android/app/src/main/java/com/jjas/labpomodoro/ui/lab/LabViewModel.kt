package com.jjas.labpomodoro.ui.lab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.InventoryItem
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.data.repository.LabRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.usecase.Fusion
import com.jjas.labpomodoro.domain.usecase.Streak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LabUi(
    /** Los 118, en orden de número atómico. */
    val items: List<InventoryItem>,
    val streak: Streak,
    val totalWorkSeconds: Long,
) {
    val discoveredCount: Int get() = items.count { it.discovered }
    val quantities: Map<Int, Int> get() = items.associate { it.element.atomicNumber to it.quantity }

    /** Elementos que se pueden fabricar ahora mismo con lo que hay. */
    val craftable: List<Element> get() {
        val q = quantities
        return items.map { it.element }.filter { Fusion.recipesFor(it.atomicNumber, q).isNotEmpty() }
    }
}

val InventoryItem.discovered: Boolean get() = firstObtainedAtMillis != null

sealed interface LabEvent {
    data class Fused(val result: Element) : LabEvent
    data object FusionFailed : LabEvent
}

@HiltViewModel
class LabViewModel @Inject constructor(
    inventory: InventoryRepository,
    sessions: SessionRepository,
    private val lab: LabRepository,
) : ViewModel() {

    val state: StateFlow<LabUi?> = combine(inventory.items, sessions.streak, sessions.totalWorkSeconds) { items, streak, total ->
        LabUi(items, streak, total)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Los hallazgos que aún no se habían visto al abrir la pantalla, para marcarlos como nuevos. */
    private val _newAtomicNumbers = MutableStateFlow<Set<Int>>(emptySet())
    val newAtomicNumbers: StateFlow<Set<Int>> = _newAtomicNumbers

    private val _events = Channel<LabEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _newAtomicNumbers.value = lab.unseen.first().map { it.element.atomicNumber }.toSet()
            lab.markAllSeen()
        }
    }

    fun fuse(recipe: Fusion.Recipe) {
        viewModelScope.launch {
            val ok = lab.fuse(recipe)
            if (ok) lab.markAllSeen()
            _events.send(if (ok) LabEvent.Fused(recipe.result) else LabEvent.FusionFailed)
        }
    }
}
