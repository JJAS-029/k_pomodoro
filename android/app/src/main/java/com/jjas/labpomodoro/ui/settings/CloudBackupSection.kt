package com.jjas.labpomodoro.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jjas.labpomodoro.R
import com.jjas.labpomodoro.data.backup.BackupInfo
import com.jjas.labpomodoro.data.backup.BackupRepository
import com.jjas.labpomodoro.data.backup.BackupStatus
import com.jjas.labpomodoro.data.remote.AuthRepository
import com.jjas.labpomodoro.ui.main.formattedDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CloudBackupViewModel @Inject constructor(
    private val backup: BackupRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    val signedIn: StateFlow<Boolean?> = auth.currentUser.map { it != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val info: StateFlow<BackupInfo?> = backup.info
    val status: StateFlow<BackupStatus> = backup.status

    init {
        viewModelScope.launch {
            backup.clearStatus()
            backup.refreshInfo()
        }
    }

    fun signIn(activityContext: Context) {
        viewModelScope.launch {
            runCatching { auth.signInWithGoogle(activityContext) }
                .onSuccess { if (backup.refreshInfo() == null) backup.backup() }
        }
    }

    fun backupNow() {
        viewModelScope.launch { backup.backup() }
    }

    fun restore() {
        viewModelScope.launch { backup.restore() }
    }
}

/** Respaldo del progreso en la nube: gratis, con la cuenta de Google. */
@Composable
fun CloudBackupSection(viewModel: CloudBackupViewModel = hiltViewModel()) {
    val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
    val info by viewModel.info.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmRestore by rememberSaveable { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (signedIn) {
            null -> Unit
            false -> {
                Text(
                    stringResource(R.string.set_backup_signed_out_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(onClick = { viewModel.signIn(context) }) { Text(stringResource(R.string.set_sign_in_google)) }
            }
            true -> {
                Text(
                    text = info?.let {
                        stringResource(
                            R.string.set_backup_last,
                            it.formattedDate(),
                            pluralStringResource(R.plurals.set_backup_elements, it.elements, it.elements),
                            pluralStringResource(R.plurals.set_backup_sessions, it.sessions, it.sessions),
                        )
                    } ?: stringResource(R.string.set_backup_none),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    stringResource(R.string.set_backup_auto),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val working = status is BackupStatus.Working
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = viewModel::backupNow, enabled = !working) { Text(stringResource(R.string.set_backup_now)) }
                    if (info != null) {
                        TextButton(onClick = { confirmRestore = true }, enabled = !working) { Text(stringResource(R.string.set_restore)) }
                    }
                    if (working) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
                when (val s = status) {
                    is BackupStatus.Done -> Text(stringResource(s.messageRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    is BackupStatus.Failed -> Text(stringResource(s.messageRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    else -> Unit
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            confirmButton = {
                Button(onClick = {
                    confirmRestore = false
                    viewModel.restore()
                }) { Text(stringResource(R.string.set_restore)) }
            },
            dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text(stringResource(R.string.set_cancel)) } },
            title = { Text(stringResource(R.string.set_restore_confirm_title)) },
            text = { Text(stringResource(R.string.set_restore_confirm_text)) },
        )
    }
}
