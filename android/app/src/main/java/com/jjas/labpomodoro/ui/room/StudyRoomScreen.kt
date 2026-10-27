package com.jjas.labpomodoro.ui.room

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.room.MemberStatus
import com.jjas.labpomodoro.data.room.RoomMember
import com.jjas.labpomodoro.data.room.RoomState
import com.jjas.labpomodoro.data.room.StudyRoomRepository
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.domain.model.SessionConfig
import com.jjas.labpomodoro.domain.model.SessionType
import com.jjas.labpomodoro.domain.usecase.SessionPlanGenerator
import com.jjas.labpomodoro.ui.components.ElementTile
import com.jjas.labpomodoro.ui.main.MainViewModel
import com.jjas.labpomodoro.ui.promo.shareText

/** Estudiar con amigos: crear una sala (Pro), entrar con un código y ver a los demás. */
@Composable
fun StudyRoomScreen(
    onBack: () -> Unit,
    onOpenPro: () -> Unit,
    viewModel: StudyRoomViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val nickname by viewModel.nickname.collectAsStateWithLifecycle()
    val account by mainViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.room_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onBack) { Text(stringResource(R.string.room_done)) }
            }
            val room = state.room
            when {
                room != null -> InRoom(room, state, onStart = viewModel::startTogether, onLeave = viewModel::leave, onBack = onBack)
                !account.isSignedIn -> SignedOut(busy = account.isBusy, onSignIn = { mainViewModel.signIn(context) })
                else -> Lobby(
                    state = state,
                    nickname = nickname,
                    busy = busy,
                    onNickname = viewModel::setNickname,
                    onCreate = { if (state.isPro) viewModel.create(state.config) else onOpenPro() },
                    onJoin = viewModel::join,
                )
            }
        }
    }

    val shownMessage = message ?: if (state.closed) R.string.room_closed_by_host else null
    shownMessage?.let { res ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            text = { Text(stringResource(res)) },
            confirmButton = { TextButton(onClick = viewModel::clearMessage) { Text(stringResource(R.string.room_ok)) } },
        )
    }
}

@Composable
private fun SignedOut(busy: Boolean, onSignIn: () -> Unit) {
    Text(stringResource(R.string.room_intro), style = MaterialTheme.typography.bodyLarge)
    Text(stringResource(R.string.room_sign_in_needed), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = onSignIn, enabled = !busy) { Text(stringResource(R.string.room_sign_in)) }
}

@Composable
private fun Lobby(
    state: StudyRoomUi,
    nickname: String,
    busy: Boolean,
    onNickname: (String) -> Unit,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Text(stringResource(R.string.room_intro), style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = nickname,
        onValueChange = onNickname,
        label = { Text(stringResource(R.string.room_nickname)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Card {
        Text(stringResource(R.string.room_create_title), style = MaterialTheme.typography.titleMedium)
        Text(planSummary(state.config), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.room_create_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onCreate, enabled = !busy) {
            Text(stringResource(if (state.isPro) R.string.room_create else R.string.room_create_pro))
        }
    }
    Card {
        Text(stringResource(R.string.room_join_title), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = code,
            onValueChange = { value -> code = value.uppercase().filter { it in StudyRoomRepository.CODE_ALPHABET }.take(StudyRoomRepository.CODE_LENGTH) },
            label = { Text(stringResource(R.string.room_code)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.timerActive) {
            Text(stringResource(R.string.room_replaces_plan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Button(onClick = { onJoin(code) }, enabled = !busy && code.length == StudyRoomRepository.CODE_LENGTH) {
            Text(stringResource(R.string.room_join))
        }
    }
}

@Composable
private fun InRoom(room: RoomState, state: StudyRoomUi, onStart: () -> Unit, onLeave: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var confirmLeave by rememberSaveable { mutableStateOf(false) }
    Card {
        Text(stringResource(R.string.room_code_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                room.room.code,
                style = MaterialTheme.typography.displaySmall.copy(fontFamily = FontFamily.Monospace, letterSpacing = 6.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            val shareText = stringResource(R.string.room_share_text, room.room.code)
            IconButton(onClick = { context.shareText(shareText) }) {
                Icon(painterResource(R.drawable.ic_share), contentDescription = stringResource(R.string.room_share))
            }
        }
        Text(planSummary(room.room.config), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    when {
        !room.room.started && room.isHost -> {
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.room_start_together)) }
            if (state.timerActive) {
                Text(stringResource(R.string.room_replaces_plan), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        !room.room.started -> Text(stringResource(R.string.room_waiting_host, room.room.hostName), style = MaterialTheme.typography.bodyLarge)
        else -> {
            if (room.room.paused) Text(stringResource(R.string.room_paused_by_host), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.room_go_to_timer)) }
        }
    }
    Text(
        pluralStringResource(R.plurals.room_members, room.members.size, room.members.size, StudyRoomRepository.MAX_MEMBERS),
        style = MaterialTheme.typography.titleMedium,
    )
    room.members.forEach { member -> MemberRow(member, isMe = member.uid == room.myUid, isHost = member.uid == room.room.hostUid) }
    OutlinedButton(onClick = { confirmLeave = true }, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(if (room.isHost) R.string.room_close else R.string.room_leave))
    }
    if (confirmLeave) {
        AlertDialog(
            onDismissRequest = { confirmLeave = false },
            text = { Text(stringResource(if (room.isHost) R.string.room_close_confirm else R.string.room_leave_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLeave = false
                    onLeave()
                }) { Text(stringResource(if (room.isHost) R.string.room_close else R.string.room_leave)) }
            },
            dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text(stringResource(R.string.room_cancel)) } },
        )
    }
}

@Composable
private fun MemberRow(member: RoomMember, isMe: Boolean, isHost: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        ElementTile(PeriodicTable[member.avatar.coerceIn(1, PeriodicTable.SIZE)], discovered = true, size = 40.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                if (isMe) stringResource(R.string.room_member_me, member.nickname) else member.nickname,
                style = MaterialTheme.typography.titleSmall,
            )
            if (isHost) Text(stringResource(R.string.room_host), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val (label, color) = statusLabel(member.status)
        Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.18f)) {
            Text(stringResource(label), color = color, style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
        }
    }
}

private fun statusLabel(status: MemberStatus): Pair<Int, Color> = when (status) {
    MemberStatus.WAITING -> R.string.room_status_waiting to Color(0xFF9AA3AE)
    MemberStatus.FOCUS -> R.string.room_status_focus to Color(0xFF39FF14)
    MemberStatus.BREAK -> R.string.room_status_break to Color(0xFFFFC107)
    MemberStatus.PAUSED -> R.string.room_status_paused to Color(0xFF7FC8FF)
    MemberStatus.AWAY -> R.string.room_status_away to Color(0xFFFF5A6E)
    MemberStatus.DONE -> R.string.room_status_done to Color(0xFFC77DFF)
    MemberStatus.OFFLINE -> R.string.room_status_offline to Color(0xFF6E7681)
}

@Composable
private fun planSummary(config: SessionConfig): String {
    val plan = SessionPlanGenerator.generate(config)
    val pomodoros = plan.count { it.type == SessionType.WORK }
    val minutes = plan.filter { it.type == SessionType.WORK }.sumOf { it.durationSeconds } / 60
    return pluralStringResource(R.plurals.room_plan_summary, pomodoros, pomodoros, minutes / 60, minutes % 60)
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}
