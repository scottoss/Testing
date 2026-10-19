package chat.stoat.screens.chat.dialogs

import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import chat.stoat.R
import chat.stoat.api.StoatAPI
import chat.stoat.api.routes.channel.createInvite
import chat.stoat.core.model.data.STOAT_INVITES
import chat.stoat.internals.Platform
import chat.stoat.ui.theme.FragmentMono
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val inviteChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')

private fun placeholderInviteCode(): String {
    return (1..8)
        .map { inviteChars.random() }
        .joinToString("")
}

data class InviteCodeChar(
    val char: Char,
    val isActual: Boolean
)

operator fun InviteCodeChar.compareTo(other: InviteCodeChar): Int {
    return char.compareTo(other.char)
}

@Composable
fun InviteDialog(channelId: String, onDismissRequest: () -> Unit) {
    val channel = StoatAPI.channelCache[channelId]

    if (channel == null) {
        onDismissRequest()
        return
    }

    var isActual by remember { mutableStateOf(false) }
    var inviteCode by remember { mutableStateOf(placeholderInviteCode()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var retryTrigger by remember { mutableIntStateOf(0) }

    val invitePrefixOpacity by animateFloatAsState(
        targetValue = if (isActual && errorMessage == null) 1f else 0f,
        label = "Invite prefix opacity"
    )

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var swapInviteJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(channelId, retryTrigger) {
        isActual = false
        errorMessage = null
        try {
            val invite = createInvite(channelId)
            swapInviteJob?.cancel()
            val code = invite.effectiveCode.ifBlank { invite.id }
            if (code.isNotBlank() && code != "error") {
                inviteCode = code
                isActual = true
            } else {
                throw Exception("Could not generate invite code.")
            }
        } catch (error: Throwable) {
            if (error is CancellationException) throw error
            Log.e("InviteDialog", "Error generating invite", error)
            swapInviteJob?.cancel()
            isActual = true
            errorMessage = error.message ?: "Failed to create invite"
            inviteCode = "error"
        }
    }

    fun swapInviteCode() {
        if (isActual) return
        inviteCode = placeholderInviteCode()
    }

    // Every second, swap the invite code to a new one. When we have a real invite, stop swapping.
    LaunchedEffect(inviteCode, isActual) {
        if (isActual) {
            swapInviteJob?.cancel()
            return@LaunchedEffect
        }
        if (swapInviteJob != null) return@LaunchedEffect
        swapInviteJob = scope.launch {
            while (!isActual) {
                swapInviteCode()
                delay(500)
            }
        }
    }

    BoxWithConstraints {
        Column(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
                .width(maxWidth * 0.85f)
                .heightIn(max = maxHeight * 0.85f)
        ) {
            Text(
                stringResource(
                    R.string.invite_dialog_header,
                    channel.name ?: stringResource(R.string.unknown)
                ),
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(Modifier.height(16.dp))

            val inviteHostPrefix = remember {
                val uri = Uri.parse(STOAT_INVITES)
                val host = uri.host ?: "csinvite.net"
                val path = uri.path?.trimEnd('/').orEmpty()
                if (path.isNotEmpty()) "$host$path/" else "$host/"
            }

            Text(
                inviteHostPrefix,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(invitePrefixOpacity)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                inviteCode
                    .map { InviteCodeChar(it, isActual) }
                    .forEach {
                        AnimatedContent(
                            targetState = it,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInVertically { -it } togetherWith slideOutVertically { it }
                                } else {
                                    slideInVertically { it } togetherWith slideOutVertically { -it }
                                }
                            },
                            label = "Invite code char"
                        ) { state ->
                            Text(
                                state.char.toString(),
                                style = MaterialTheme.typography.displayLarge.copy(
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                fontFamily = FragmentMono,
                                color = if (errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .alpha(if (state.isActual) 1f else 0.4f)
                            )
                        }
                    }
            }

            if (errorMessage != null) {
                Text(
                    text = errorMessage ?: "Error creating invite",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                stringResource(R.string.invite_dialog_description),
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = {
                    onDismissRequest()
                }) {
                    Text(stringResource(R.string.invite_dialog_close))
                }
                Spacer(Modifier.width(8.dp))
                if (errorMessage != null) {
                    Button(onClick = {
                        retryTrigger++
                    }) {
                        Text("Retry")
                    }
                } else {
                    Button(
                        onClick = {
                            val fullUrl = "${STOAT_INVITES.trimEnd('/')}/$inviteCode"
                            clipboardManager.setText(AnnotatedString(fullUrl))

                            if (Platform.needsShowClipboardNotification()) {
                                Toast.makeText(
                                    context,
                                    resources.getString(R.string.copied),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = isActual && inviteCode != "error"
                    ) {
                        Text(stringResource(R.string.invite_dialog_copy))
                    }
                }
            }
        }
    }
}
