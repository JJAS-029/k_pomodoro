package com.jjas.labpomodoro.ui.league

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.league.LeagueProfile
import com.jjas.labpomodoro.data.league.LeagueRepository
import com.jjas.labpomodoro.data.league.LeagueResult
import com.jjas.labpomodoro.data.remote.AuthRepository
import com.jjas.labpomodoro.data.repository.InventoryRepository
import com.jjas.labpomodoro.domain.model.Element
import com.jjas.labpomodoro.domain.model.League
import com.jjas.labpomodoro.domain.model.LeagueMember
import com.jjas.labpomodoro.domain.model.LeagueOutcome
import com.jjas.labpomodoro.domain.model.LeagueRules
import com.jjas.labpomodoro.domain.model.PeriodicTable
import com.jjas.labpomodoro.service.LeagueSync
import com.jjas.labpomodoro.ui.components.ElementTile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import kotlin.random.Random

private sealed interface Step {
    data object Loading : Step
    data class Loaded(val profile: LeagueProfile?) : Step
}

sealed interface LeagueUi {
    data object Loading : LeagueUi
    data object SignedOut : LeagueUi
    data class NotJoined(val avatars: List<Element>) : LeagueUi
    data class Joined(
        val profile: LeagueProfile,
        /** Ya ordenado, con los bots incluidos. */
        val ranking: List<LeagueMember>,
        val myId: String,
        val daysLeft: Long,
    ) : LeagueUi
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val leagues: LeagueRepository,
    private val auth: AuthRepository,
    private val sync: LeagueSync,
    inventory: InventoryRepository,
    private val clock: Clock,
) : ViewModel() {

    private val refresh = MutableStateFlow(0)
    private val _error = MutableStateFlow<Int?>(null)
    val error: StateFlow<Int?> = _error

    private val discovered = inventory.items.map { items -> items.filter { it.firstObtainedAtMillis != null }.map { it.element } }

    val state: StateFlow<LeagueUi> = combine(auth.currentUser, refresh) { user, _ -> user }
        .flatMapLatest { user ->
            if (user == null) return@flatMapLatest flowOf(LeagueUi.SignedOut)
            flow {
                emit(Step.Loading)
                // Si empezó otra semana, primero se cierra la anterior
                runCatching { leagues.rollOverIfNeeded(sync.currentXp()) }
                emit(Step.Loaded(runCatching { leagues.profile() }.getOrNull()))
            }.flatMapLatest { step ->
                val profile = (step as? Step.Loaded)?.profile
                when {
                    step is Step.Loading -> flowOf(LeagueUi.Loading)
                    profile?.groupId == null -> discovered.map { LeagueUi.NotJoined(it.ifEmpty { listOf(PeriodicTable[1]) }) }
                    else -> leagues.observeMembers(profile.groupId).map { real ->
                        val now = clock.instant()
                        val bots = LeagueRules.bots(profile.groupId, profile.league, real.size, LeagueRules.weekProgress(now))
                        LeagueUi.Joined(
                            profile = profile,
                            ranking = LeagueRules.ranking(real + bots),
                            myId = user.uid,
                            daysLeft = Duration.between(now, LeagueRules.weekEnd(now)).toDays(),
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LeagueUi.Loading)

    fun signIn(activityContext: Context) {
        viewModelScope.launch { runCatching { auth.signInWithGoogle(activityContext) } }
    }

    fun join(nickname: String, avatar: Int) {
        viewModelScope.launch {
            _error.value = null
            runCatching { leagues.join(nickname, avatar, sync.currentXp()) }
                .onSuccess { refresh.value++ }
                .onFailure { _error.value = R.string.set_league_join_failed }
        }
    }

    fun leave() {
        viewModelScope.launch {
            runCatching { leagues.leave() }
            refresh.value++
        }
    }

    fun resultSeen() {
        viewModelScope.launch {
            runCatching { leagues.markResultSeen() }
            refresh.value++
        }
    }
}

private const val BOT_PREFIX = "Asistente "

private val Promote = Color(0xFF2E7D32)
private val Demote = Color(0xFFC62828)

/** Pestaña Liga de Logros. */
@Composable
fun LeaguePanel(modifier: Modifier = Modifier, viewModel: LeagueViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (val s = state) {
            LeagueUi.Loading -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            LeagueUi.SignedOut -> SignedOut(onSignIn = viewModel::signIn)
            is LeagueUi.NotJoined -> JoinForm(s.avatars, onJoin = viewModel::join)
            is LeagueUi.Joined -> {
                Joined(s, onLeave = viewModel::leave)
                s.profile.lastResult?.takeIf { !it.seen }?.let { ResultDialog(it, onDismiss = viewModel::resultSeen) }
            }
        }
        error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Rules() {
    Text(
        stringResource(R.string.set_league_rules),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SignedOut(onSignIn: (Context) -> Unit) {
    val context = LocalContext.current
    Rules()
    Text(
        stringResource(R.string.set_league_sign_in_desc),
        style = MaterialTheme.typography.bodyMedium,
    )
    FilledTonalButton(onClick = { onSignIn(context) }) { Text(stringResource(R.string.set_sign_in_google)) }
}

@Composable
private fun JoinForm(avatars: List<Element>, onJoin: (String, Int) -> Unit) {
    var nickname by rememberSaveable { mutableStateOf("Koala ${Random.nextInt(100, 1000)}") }
    var avatar by rememberSaveable { mutableIntStateOf(avatars.first().atomicNumber) }
    Rules()
    Text(stringResource(R.string.set_league_nickname), style = MaterialTheme.typography.titleSmall)
    OutlinedTextField(
        value = nickname,
        onValueChange = { nickname = it.take(LeagueRepository.MAX_NICKNAME) },
        singleLine = true,
        supportingText = { Text(stringResource(R.string.set_league_nickname_hint)) },
        modifier = Modifier.fillMaxWidth(),
    )
    Text(stringResource(R.string.set_league_avatar), style = MaterialTheme.typography.titleSmall)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(avatars, key = { it.atomicNumber }) { element ->
            ElementTile(
                element,
                discovered = true,
                size = 44.dp,
                highlighted = element.atomicNumber == avatar,
                modifier = Modifier.clickable { avatar = element.atomicNumber },
            )
        }
    }
    Button(onClick = { onJoin(nickname, avatar) }, enabled = nickname.isNotBlank()) { Text(stringResource(R.string.set_league_join)) }
}

@Composable
private fun Joined(state: LeagueUi.Joined, onLeave: () -> Unit) {
    val league = state.profile.league
    val color = Color(league.color)
    val size = state.ranking.size
    val promote = if (league.next != null) LeagueRules.promoteCount(size) else 0
    val demote = if (league.previous != null) LeagueRules.demoteCount(size) else 0
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.85f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center,
            ) { Text(league.symbol, style = MaterialTheme.typography.titleLarge, color = Color(0xFF111111), fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.set_league_name, stringResource(league.labelRes)), style = MaterialTheme.typography.titleLarge)
                Text(
                    if (state.daysLeft == 0L) {
                        stringResource(R.string.set_league_ends_today)
                    } else {
                        val days = state.daysLeft.toInt()
                        pluralStringResource(R.plurals.set_league_ends_in, days, days)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
    val promoteText = league.next?.takeIf { promote > 0 }?.let {
        pluralStringResource(R.plurals.set_league_promote, promote, promote, stringResource(it.labelRes))
    }
    val demoteText = league.previous?.takeIf { demote > 0 }?.let {
        pluralStringResource(R.plurals.set_league_demote, demote, demote, stringResource(it.labelRes))
    }
    Text(
        listOfNotNull(promoteText, demoteText).joinToString(" "),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        state.ranking.forEachIndexed { i, member ->
            val rank = i + 1
            val zone = when {
                rank <= promote -> Promote
                rank > size - demote -> Demote
                else -> null
            }
            MemberRow(rank, member, isMe = member.id == state.myId, zone = zone)
        }
    }
    TextButton(onClick = onLeave) { Text(stringResource(R.string.set_league_leave)) }
}

@Composable
private fun MemberRow(rank: Int, member: LeagueMember, isMe: Boolean, zone: Color?) {
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .background(zone?.copy(alpha = 0.16f) ?: Color.Transparent, shape)
            .then(if (isMe) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, shape) else Modifier)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("$rank", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(28.dp), color = zone ?: MaterialTheme.colorScheme.onSurface)
        ElementTile(PeriodicTable[member.avatar.coerceIn(1, PeriodicTable.SIZE)], discovered = true, size = 30.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            // Los bots se llaman "Asistente X" en el modelo; aquí se traduce el prefijo
            val name = if (member.isBot && member.nickname.startsWith(BOT_PREFIX)) {
                stringResource(R.string.set_league_bot_name, member.nickname.removePrefix(BOT_PREFIX))
            } else {
                member.nickname
            }
            Text(
                if (isMe) stringResource(R.string.set_league_me, name) else name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isMe) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
            )
            if (member.isBot) {
                Text(stringResource(R.string.set_league_bot), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("${member.xp} min", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ResultDialog(result: LeagueResult, onDismiss: () -> Unit) {
    val (titleRes, textRes) = when (result.outcome) {
        LeagueOutcome.PROMOTED -> R.string.set_league_promoted_title to R.string.set_league_promoted_text
        LeagueOutcome.DEMOTED -> R.string.set_league_demoted_title to R.string.set_league_demoted_text
        LeagueOutcome.STAYED -> R.string.set_league_stayed_title to R.string.set_league_stayed_text
    }
    val title = stringResource(titleRes, stringResource(result.to.labelRes))
    val text = stringResource(textRes, result.rank, result.size)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { Button(onClick = onDismiss) { Text(stringResource(R.string.set_league_lets_go)) } },
        title = { Text(title) },
        text = { Text(text) },
    )
}
