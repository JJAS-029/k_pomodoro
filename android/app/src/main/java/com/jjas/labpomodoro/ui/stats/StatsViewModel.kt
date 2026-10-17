package com.jjas.labpomodoro.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.local.entity.PlaceTotal
import com.jjas.labpomodoro.data.repository.MedalRepository
import com.jjas.labpomodoro.data.repository.PlaceRepository
import com.jjas.labpomodoro.data.repository.TagRepository
import com.jjas.labpomodoro.data.local.entity.TagTotal
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.MedalProgress
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
import com.jjas.labpomodoro.domain.usecase.HeatDay
import com.jjas.labpomodoro.domain.usecase.YearHeatmap
import com.jjas.labpomodoro.domain.model.SessionType
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
    val tags: List<TagTotal> = emptyList(),
    val medals: List<MedalProgress>,
    /** Mapa de calor del último año (no depende del periodo elegido). */
    val year: List<List<HeatDay?>> = emptyList(),
) {
    val activeDays: Int get() = year.sumOf { week -> week.count { (it?.minutes ?: 0) > 0 } }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class StatsViewModel @Inject constructor(
    sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val placeRepository: PlaceRepository,
    private val tagRepository: TagRepository,
    private val medalRepository: MedalRepository,
    private val clock: Clock,
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.WEEK)
    val range: StateFlow<StatsRange> = _range

    val state: StateFlow<StatsUi?> = _range.flatMapLatest { range ->
        combine(
            sessions.allSessions,
            sessions.streak,
            settings.settings,
            combine(placeRepository.totals(range.days), tagRepository.totals(range.days), ::Pair),
            medalRepository.progress,
        ) { all, streak, prefs, (places, tags), medals ->
            StatsUi(
                stats = StatsCalculator.calculate(all, LocalDate.now(clock), range),
                streak = streak,
                places = when {
                    !prefs.isPro -> PlacesUi.NotPro
                    !prefs.placesEnabled -> PlacesUi.Disabled
                    else -> PlacesUi.Enabled(places)
                },
                medals = medals,
                tags = tags,
                year = YearHeatmap.weeks(
                    all.filter { it.type == SessionType.WORK && it.completed }
                        .groupBy { it.epochDay }
                        .mapValues { (_, day) -> day.sumOf { it.actualSeconds } / 60 },
                    LocalDate.now(clock),
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Al ver las medallas aquí ya no hace falta el aviso de la pantalla principal. */
    fun markMedalsSeen(medals: List<MedalProgress>) {
        val earned = medals.filter { it.earned }.map { it.medal }
        if (earned.isNotEmpty()) viewModelScope.launch { medalRepository.markSeen(earned) }
    }

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
