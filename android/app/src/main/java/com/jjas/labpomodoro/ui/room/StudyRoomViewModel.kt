package com.jjas.labpomodoro.ui.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.repository.SettingsRepository
import com.jjas.labpomodoro.data.room.JoinResult
import com.jjas.labpomodoro.data.room.RoomState
import com.jjas.labpomodoro.data.room.StudyRoomRepository
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.timer.TimerEngine
import com.jjas.labpomodoro.timer.TimerState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StudyRoomUi(
    val room: RoomState? = null,
    val isPro: Boolean = false,
    /** Plan de siempre: es el que lleva una sala nueva. */
    val config: SessionConfig = SessionConfig(),
    /** Ya hay un plan propio corriendo (al empezar la sala, se reemplaza). */
    val timerActive: Boolean = false,
    /** El anfitrión cerró la sala en la que estabas. */
    val closed: Boolean = false,
)

@HiltViewModel
class StudyRoomViewModel @Inject constructor(
    private val rooms: StudyRoomRepository,
    settings: SettingsRepository,
    engine: TimerEngine,
) : ViewModel() {

    val state: StateFlow<StudyRoomUi> = combine(rooms.state, settings.settings, engine.state, rooms.closed) { room, prefs, timer, closed ->
        StudyRoomUi(room, prefs.isPro, prefs.session, timer is TimerState.Active, closed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StudyRoomUi())

    /** Ocupado (creando o entrando) y el último aviso para mostrar. */
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message

    private val _nickname = MutableStateFlow("")
    val nickname: StateFlow<String> = _nickname

    init {
        viewModelScope.launch { _nickname.value = rooms.defaultNickname() }
    }

    fun setNickname(value: String) {
        _nickname.value = value.take(StudyRoomRepository.MAX_NICKNAME)
    }

    fun create(config: SessionConfig) = run {
        if (rooms.create(config, _nickname.value) == null) _message.value = R.string.room_error_connection
    }

    fun join(code: String) = run {
        _message.value = when (rooms.join(code, _nickname.value)) {
            JoinResult.OK -> null
            JoinResult.SIGNED_OUT -> R.string.room_sign_in_needed
            JoinResult.NOT_FOUND -> R.string.room_error_not_found
            JoinResult.FULL -> R.string.room_error_full
            JoinResult.ERROR -> R.string.room_error_connection
        }
    }

    fun startTogether() {
        viewModelScope.launch { rooms.startTogether() }
    }

    fun leave() {
        // El plan sigue corriendo ya por su cuenta; para detenerlo está el botón de detener
        viewModelScope.launch { rooms.leave(stopTimer = false) }
    }

    fun clearMessage() {
        _message.value = null
        rooms.clearClosed()
    }

    private fun run(block: suspend () -> Unit) {
        if (_busy.value) return
        _busy.update { true }
        viewModelScope.launch {
            try {
                block()
            } finally {
                _busy.value = false
            }
        }
    }
}
