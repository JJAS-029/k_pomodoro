package com.jjas.labpomodoro.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import com.jjas.labpomodoro.data.repository.PlaceRepository
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.usecase.FocusStats
import com.jjas.labpomodoro.domain.usecase.StatsCalculator
import com.jjas.labpomodoro.domain.usecase.StatsRange
import com.jjas.labpomodoro.domain.usecase.Streak
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Estado de la sección de lugares (Pro, opcional). */
sealed interface PlacesUi {
    data object NotPro : PlacesUi
    data object Disabled : PlacesUi
    data class Enabled(val places: List<PlaceTotal>) : PlacesUi
}

data class StatsUi(
    val stats: FocusStats,
    val streak: Streak,
    val places: PlacesUi,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range: StateFlow<StatsRange> = _range

    val state: StateFlow<StatsUi?> = _range.flatMapLatest { range ->
        combine(
            sessions.allSessions,
            sessions.streak,
            settings.settings,
            placeRepository.totals(range.days),
        ) { all, streak, prefs, places ->
            StatsUi(
                stats = StatsCalculator.calculate(all, LocalDate.now(clock), range),
                streak = streak,
                places = when {
                    !prefs.isPro -> PlacesUi.NotPro
                    !prefs.placesEnabled -> PlacesUi.Disabled
                    else -> PlacesUi.Enabled(places)
                },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun setRange(range: StatsRange) {
        _range.value = range
    }

    fun setPlacesEnabled(enabled: Boolean) {
        viewModelScope.launch { settings.setPlacesEnabled(enabled) }
    }

    fun renamePlace(id: Long, name: String) {
        viewModelScope.launch { placeRepository.rename(id, name) }
    }
}
