package com.jjas.labpomodoro.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.data.repository.SessionRepository
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.DailyGoal
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Meta diaria: [goal] 0 = sin meta. [celebratedToday] evita repetir el aviso el mismo día. */
data class GoalUi(
    val goal: Int = 0,
    val today: Int = 0,
    val streak: Int = 0,
    val celebratedToday: Boolean = true,
    /** Meta semanal y pomodoros de esta semana (desde el lunes). */
    val weekGoal: Int = 0,
    val week: Int = 0,
) {
    val reached: Boolean get() = goal > 0 && today >= goal
    val fraction: Float get() = if (goal <= 0) 0f else (today.toFloat() / goal).coerceIn(0f, 1f)
    val weekFraction: Float get() = if (weekGoal <= 0) 0f else (week.toFloat() / weekGoal).coerceIn(0f, 1f)
    val hasAny: Boolean get() = goal > 0 || weekGoal > 0
}

@HiltViewModel
class GoalViewModel @Inject constructor(
    sessions: SessionRepository,
    private val settings: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    val state: StateFlow<GoalUi> = combine(sessions.allSessions, settings.settings) { all, prefs ->
        val counts = all.filter { it.type == SessionType.WORK && it.completed }.groupingBy { it.epochDay }.eachCount()
        val today = LocalDate.now(clock).toEpochDay()
        GoalUi(
            goal = prefs.dailyGoal,
            today = counts[today] ?: 0,
            streak = DailyGoal.streak(counts, today, prefs.dailyGoal),
            celebratedToday = prefs.goalCelebratedDay == today,
            weekGoal = prefs.weeklyGoal,
            week = (0..LocalDate.ofEpochDay(today).dayOfWeek.ordinal).sumOf { counts[today - it] ?: 0 },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GoalUi())

    fun setGoal(goal: Int) {
        val value = goal.coerceIn(DailyGoal.GOAL_RANGE)
        viewModelScope.launch {
            // Si al ajustarla ya está cumplida, no se celebra: el aviso es para cuando se alcanza
            if (value > 0 && state.value.today >= value) settings.setGoalCelebratedDay(LocalDate.now(clock).toEpochDay())
            settings.setDailyGoal(value)
        }
    }

    fun setWeeklyGoal(goal: Int) {
        viewModelScope.launch { settings.setWeeklyGoal(goal.coerceIn(DailyGoal.WEEKLY_RANGE)) }
    }

    fun markCelebrated() {
        viewModelScope.launch { settings.setGoalCelebratedDay(LocalDate.now(clock).toEpochDay()) }
    }
}
